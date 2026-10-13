package ru.klimovich.catalog_service.service.impl;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.web.multipart.MultipartFile;
import ru.klimovich.catalog_service.client.SellerClient;
import ru.klimovich.catalog_service.dto.request.ProductRequest;
import ru.klimovich.catalog_service.dto.request.ProductUpdateRequest;
import ru.klimovich.catalog_service.dto.response.ProductResponse;
import ru.klimovich.catalog_service.dto.response.StoreResponse;
import ru.klimovich.catalog_service.exception.ResourceConflictException;
import ru.klimovich.catalog_service.exception.ResourceNotFoundException;
import ru.klimovich.catalog_service.exception.SellerServiceUnavailableException;
import ru.klimovich.catalog_service.mapper.ProductMapper;
import ru.klimovich.catalog_service.model.Product;
import ru.klimovich.catalog_service.model.Status;
import ru.klimovich.catalog_service.repository.CategoryRepository;
import ru.klimovich.catalog_service.repository.ProductRepository;
import ru.klimovich.catalog_service.service.CategoryService;
import ru.klimovich.catalog_service.service.FileStorageService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Requirements "Product availability", "Product retrieval by id", "Product changes require active
 * store and seller", "Product creation preconditions", "Product update" (change seller-store-lifecycle).
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    private static final String TOKEN = "jwt";

    @Mock private ProductRepository productRepo;
    @Mock private CategoryService categoryService;
    @Mock private CategoryRepository categoryRepo;
    @Mock private ProductMapper productMapper;
    @Mock private FileStorageService fileStorageService;
    @Mock private SellerClient sellerClient;
    @Mock private CurrentUserService currentUserService;
    @Mock private MongoTemplate mongoTemplate;

    private ProductServiceImpl service;
    private final ProductResponse response = new ProductResponse();

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(productRepo, categoryService, categoryRepo, productMapper,
                fileStorageService, sellerClient, currentUserService, mongoTemplate);
        lenient().when(productMapper.toDTO(any(), any())).thenReturn(response);
        lenient().when(currentUserService.getAccessToken()).thenReturn(TOKEN);
    }

    private static Product product(Status status, Boolean storeBlocked, Boolean sellerBlocked) {
        Product p = new Product();
        p.setId("p1");
        p.setStoreId(3L);
        p.setStatus(status);
        p.setCategories(List.of());
        p.setStoreBlocked(storeBlocked);
        p.setSellerBlocked(sellerBlocked);
        return p;
    }

    private static StoreResponse store(String status, String sellerStatus) {
        StoreResponse s = new StoreResponse();
        s.setId(3L);
        s.setStatus(status);
        s.setSellerStatus(sellerStatus);
        return s;
    }

    // --- Публичная выдача ---

    @Test
    @DisplayName("GET /products: только ACTIVE и не заблокированные")
    void listing_filtered() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Product> page = new PageImpl<>(List.of(product(Status.ACTIVE, null, null)));
        when(productRepo.findByStatusAndStoreBlockedNotAndSellerBlockedNot(Status.ACTIVE, true, true, pageable))
                .thenReturn(page);

        assertThat(service.getAllProducts(pageable).getContent()).hasSize(1);
        verify(productRepo, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Выдача по категории, поиск и by-ids используют фильтр доступности")
    void categorySearchByIds_filtered() {
        Pageable pageable = PageRequest.of(0, 5);
        when(categoryRepo.findById("c1")).thenReturn(Optional.of(new ru.klimovich.catalog_service.model.Category()));
        when(productRepo.findByCategoriesContainingAndStatusAndStoreBlockedNotAndSellerBlockedNot(
                "c1", Status.ACTIVE, true, true, pageable)).thenReturn(Page.empty());
        when(productRepo.findByNameIgnoreCaseAndStatusAndStoreBlockedNotAndSellerBlockedNot(
                "camera", Status.ACTIVE, true, true)).thenReturn(List.of(product(Status.ACTIVE, null, null)));
        when(productRepo.findAllByIdInAndStatusAndStoreBlockedNotAndSellerBlockedNot(
                List.of("p1", "p2"), Status.ACTIVE, true, true)).thenReturn(List.of());

        service.getProductsByCategory(pageable, "c1");
        assertThat(service.getProductByName("camera")).hasSize(1);
        assertThat(service.getProductsByIds(List.of("p1", "p2"))).isEmpty();
    }

    @Test
    @DisplayName("Поиск: только недоступные совпадения -> 404")
    void search_onlyUnavailable_notFound() {
        when(productRepo.findByNameIgnoreCaseAndStatusAndStoreBlockedNotAndSellerBlockedNot(
                "camera", Status.ACTIVE, true, true)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getProductByName("camera")).isInstanceOf(ResourceNotFoundException.class);
    }

    // --- GET /products/{id} ---

    @Test
    @DisplayName("Доступный товар возвращается без обращения к seller-service")
    void byId_available() {
        when(productRepo.findById("p1")).thenReturn(Optional.of(product(Status.ACTIVE, false, null)));

        assertThat(service.getProductById("p1")).isSameAs(response);
        verifyNoInteractions(sellerClient);
    }

    @Test
    @DisplayName("Недоступный товар анонимному пользователю: 404, seller-service не вызывается")
    void byId_unavailable_anonymous() {
        when(productRepo.findById("p1")).thenReturn(Optional.of(product(Status.DRAFT, null, null)));
        when(currentUserService.isSeller()).thenReturn(false);

        assertThatThrownBy(() -> service.getProductById("p1")).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(sellerClient);
    }

    @Test
    @DisplayName("Недоступный товар владельцу: проверка владения, 200")
    void byId_unavailable_owner() {
        when(productRepo.findById("p1")).thenReturn(Optional.of(product(Status.ACTIVE, true, null)));
        when(currentUserService.isSeller()).thenReturn(true);
        when(sellerClient.getMyStoreById(3L, TOKEN)).thenReturn(store("BLOCKED", "ACTIVE"));

        assertThat(service.getProductById("p1")).isSameAs(response);
    }

    @Test
    @DisplayName("Недоступный товар чужому продавцу: 404")
    void byId_unavailable_foreignSeller() {
        when(productRepo.findById("p1")).thenReturn(Optional.of(product(Status.DRAFT, null, null)));
        when(currentUserService.isSeller()).thenReturn(true);
        when(sellerClient.getMyStoreById(3L, TOKEN)).thenThrow(new ResourceNotFoundException("Store [3] not found"));

        assertThatThrownBy(() -> service.getProductById("p1")).isInstanceOf(ResourceNotFoundException.class);
    }

    // --- Создание ---

    private ProductRequest createRequest() {
        ProductRequest r = new ProductRequest();
        r.setStoreId(3L);
        r.setCategories(List.of());
        r.setImages(List.of(mock(MultipartFile.class)));
        return r;
    }

    @Test
    @DisplayName("Создание в заблокированном магазине: 409 до загрузки изображений")
    void create_blockedBeforeUpload() {
        when(sellerClient.getMyStoreById(3L, TOKEN)).thenReturn(store("BLOCKED", "ACTIVE"));

        assertThatThrownBy(() -> service.createProduct(createRequest())).isInstanceOf(ResourceConflictException.class);
        verify(fileStorageService, never()).uploadProductImage(anyList());
        verify(productRepo, never()).save(any());
    }

    @Test
    @DisplayName("Создание заблокированным продавцом: 409")
    void create_blockedSeller() {
        when(sellerClient.getMyStoreById(3L, TOKEN)).thenReturn(store("ACTIVE", "BLOCKED"));

        assertThatThrownBy(() -> service.createProduct(createRequest())).isInstanceOf(ResourceConflictException.class);
        verify(productRepo, never()).save(any());
    }

    private Product stubSave() {
        Product saved = product(Status.ACTIVE, null, null);
        when(productMapper.toEntity(any(ProductRequest.class))).thenReturn(saved);
        when(fileStorageService.uploadProductImage(anyList())).thenReturn(List.of());
        return saved;
    }

    @Test
    @DisplayName("Блокировка между проверками: созданный документ удален, 409")
    void create_blockedBetweenChecks() {
        Product saved = stubSave();
        when(sellerClient.getMyStoreById(3L, TOKEN))
                .thenReturn(store("ACTIVE", "ACTIVE"), store("BLOCKED", "ACTIVE"));

        assertThatThrownBy(() -> service.createProduct(createRequest())).isInstanceOf(ResourceConflictException.class);
        verify(productRepo).save(saved);
        verify(productRepo).delete(saved);
    }

    @Test
    @DisplayName("Магазин удален между проверками: документ удален, 404")
    void create_storeDeletedBetweenChecks() {
        Product saved = stubSave();
        when(sellerClient.getMyStoreById(3L, TOKEN))
                .thenReturn(store("ACTIVE", "ACTIVE"))
                .thenThrow(new ResourceNotFoundException("Store [3] not found"));

        assertThatThrownBy(() -> service.createProduct(createRequest())).isInstanceOf(ResourceNotFoundException.class);
        verify(productRepo).delete(saved);
    }

    @Test
    @DisplayName("seller-service недоступен на повторной проверке: документ удален, 503")
    void create_sellerUnavailableOnRecheck() {
        Product saved = stubSave();
        when(sellerClient.getMyStoreById(3L, TOKEN))
                .thenReturn(store("ACTIVE", "ACTIVE"))
                .thenThrow(new SellerServiceUnavailableException());

        assertThatThrownBy(() -> service.createProduct(createRequest()))
                .isInstanceOf(SellerServiceUnavailableException.class);
        verify(productRepo).delete(saved);
    }

    @Test
    @DisplayName("Обе проверки ACTIVE: товар сохранен без признаков блокировки")
    void create_success() {
        Product saved = stubSave();
        when(sellerClient.getMyStoreById(3L, TOKEN)).thenReturn(store("ACTIVE", "ACTIVE"));

        service.createProduct(createRequest());

        verify(productRepo).save(saved);
        verify(productRepo, never()).delete(any());
        assertThat(saved.getStoreBlocked()).isNull();
        assertThat(saved.getSellerBlocked()).isNull();
    }

    // --- Изменение и удаление ---

    @Test
    @DisplayName("Изменение товара заблокированного магазина: 409, ничего не пишется")
    void update_blockedStore() {
        when(productRepo.findById("p1")).thenReturn(Optional.of(product(Status.ACTIVE, null, null)));
        when(sellerClient.getMyStoreById(3L, TOKEN)).thenReturn(store("BLOCKED", "ACTIVE"));

        assertThatThrownBy(() -> service.updateProductById("p1", new ProductUpdateRequest()))
                .isInstanceOf(ResourceConflictException.class);
        verifyNoInteractions(mongoTemplate);
        verify(productRepo, never()).save(any());
    }

    @Test
    @DisplayName("Изменение: частичное обновление без признаков блокировки и storeId")
    void update_partial() {
        when(productRepo.findById("p1")).thenReturn(Optional.of(product(Status.ACTIVE, null, null)));
        when(sellerClient.getMyStoreById(3L, TOKEN)).thenReturn(store("ACTIVE", "ACTIVE"));
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setCategories(List.of());
        request.setImages(List.of());

        service.updateProductById("p1", request);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<UpdateDefinition> update = ArgumentCaptor.forClass(UpdateDefinition.class);
        verify(mongoTemplate).updateFirst(query.capture(), update.capture(), eq(Product.class));
        assertThat(query.getValue().getQueryObject()).isEqualTo(new Document("_id", "p1"));
        Document set = (Document) update.getValue().getUpdateObject().get("$set");
        assertThat(set).doesNotContainKeys("storeBlocked", "sellerBlocked", "storeId", "articleNumber", "_id");
        assertThat(set).containsKeys("name", "images", "categories", "status");
        verify(productRepo, never()).save(any());
    }

    @Test
    @DisplayName("Удаление товара заблокированным продавцом: 409, документ не удален")
    void delete_blockedSeller() {
        when(productRepo.findById("p1")).thenReturn(Optional.of(product(Status.ACTIVE, null, null)));
        when(sellerClient.getMyStoreById(3L, TOKEN)).thenReturn(store("ACTIVE", "BLOCKED"));

        assertThatThrownBy(() -> service.deleteProductById("p1")).isInstanceOf(ResourceConflictException.class);
        verify(productRepo, never()).delete(any());
    }

    @Test
    @DisplayName("Владельческий список: без фильтра, просмотр разрешен при блокировке")
    void ownerList_unfiltered() {
        when(sellerClient.getMyStoreById(3L, TOKEN)).thenReturn(store("BLOCKED", "BLOCKED"));
        when(productRepo.findAllByStoreId(3L)).thenReturn(List.of(product(Status.DRAFT, true, true)));

        assertThat(service.getProductsByStore(3L)).hasSize(1);
    }
}
