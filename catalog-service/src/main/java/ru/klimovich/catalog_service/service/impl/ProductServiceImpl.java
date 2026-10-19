package ru.klimovich.catalog_service.service.impl;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
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
import ru.klimovich.catalog_service.model.Category;
import ru.klimovich.catalog_service.model.Image;
import ru.klimovich.catalog_service.model.Product;
import ru.klimovich.catalog_service.model.Status;
import ru.klimovich.catalog_service.outbox.ProductDeletionEventWriter;
import ru.klimovich.catalog_service.repository.CategoryRepository;
import ru.klimovich.catalog_service.repository.ProductRepository;
import ru.klimovich.catalog_service.service.CategoryService;
import ru.klimovich.catalog_service.service.FileStorageService;
import ru.klimovich.catalog_service.service.ProductService;
import ru.klimovich.catalog_service.util.MessageKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private static final String ACTIVE = "ACTIVE";

    private final ProductRepository productRepo;
    private final CategoryService categoryService;
    private final CategoryRepository categoryRepo;
    private final ProductMapper productMapper;
    private final FileStorageService fileStorageService;
    private final SellerClient sellerClient;
    private final CurrentUserService currentUserService;
    private final MongoTemplate mongoTemplate;
    private final ProductDeletionEventWriter deletionEventWriter;

    private ProductResponse buildProductResponse(Product product) {
        Map<String, String> categoryNames = categoryRepo.findAllById(product.getCategories())
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        return productMapper.toDTO(product, categoryNames);
    }

    /**
     * Проверка статуса до загрузки изображений и повторная после сохранения: если магазин
     * заблокирован или удален в этом окне, событие могло пройти мимо еще не сохраненного товара,
     * поэтому документ удаляется и товар не становится доступным.
     */
    @Override
    public void createProduct(@NotNull ProductRequest productDetails) {
        productDetails.getCategories().forEach(categoryService::getCategoryById);
        StoreResponse store = getModifiableStore(productDetails.getStoreId());

        List<Image> fileNameList = fileStorageService.uploadProductImage(productDetails.getImages());

        Product product = productMapper.toEntity(productDetails);
        product.setStoreId(store.getId());
        product.setImages(fileNameList);

        productRepo.save(product);
        try {
            getModifiableStore(store.getId());
        } catch (ResourceConflictException | ResourceNotFoundException | SellerServiceUnavailableException e) {
            productRepo.delete(product);
            throw e;
        }
        buildProductResponse(product);
    }

    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> productPage = productRepo
                .findByStatusAndStoreBlockedNotAndSellerBlockedNot(Status.ACTIVE, true, true, pageable);

        Map<String, String> categoryInfo = categoryRepo.findAllById(
                        productPage.getContent()
                                .stream()
                                .flatMap(p -> p.getCategories().stream())
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return productPage
                .map(p -> productMapper.toDTO(p, categoryInfo));
    }

    /** Владельческий список: все товары магазина, в том числе недоступные покупателям. */
    @Override
    public List<ProductResponse> getProductsByStore(Long storeId) {

        validateStoreOwnership(storeId);

        return productRepo.findAllByStoreId(storeId)
                .stream()
                .map(this::buildProductResponse)
                .toList();
    }

    /**
     * Недоступный товар раскрывается только владельцу (JWT с ролью SELLER и проверка владения),
     * остальным — 404, как для несуществующего.
     */
    @Override
    public ProductResponse getProductById(String id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> productNotFound(id));
        if (!product.isAvailable()) {
            if (!currentUserService.isSeller() || product.getStoreId() == null) {
                throw productNotFound(id);
            }
            try {
                validateStoreOwnership(product.getStoreId());
            } catch (ResourceNotFoundException e) {
                throw productNotFound(id);
            }
        }
        return buildProductResponse(product);
    }

    @Override
    public List<ProductResponse> getProductByName(String productName) {
        List<Product> products = productRepo
                .findByNameIgnoreCaseAndStatusAndStoreBlockedNotAndSellerBlockedNot(productName, Status.ACTIVE, true, true);
        if (products.isEmpty()) {
            throw new ResourceNotFoundException(String
                    .format(MessageKeys.PRODUCT_NOT_FOUND_NAME_KEY, productName));
        }
        return products.stream()
                .map(this::buildProductResponse)
                .toList();
    }

    @Override
    public List<ProductResponse> getProductsByIds(List<String> productsId) {
        return productRepo
                .findAllByIdInAndStatusAndStoreBlockedNotAndSellerBlockedNot(productsId, Status.ACTIVE, true, true)
                .stream()
                .map(this::buildProductResponse)
                .toList();
    }

    @Override
    public Page<ProductResponse> getProductsByCategory(Pageable pageable, String categoryId) {
        categoryRepo.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(String
                                .format(MessageKeys.CATEGORY_NOT_FOUND_ID_KEY, categoryId)));
        Page<Product> categoryList = productRepo.findByCategoriesContainingAndStatusAndStoreBlockedNotAndSellerBlockedNot(
                categoryId, Status.ACTIVE, true, true, pageable);

        return categoryList
                .map(this::buildProductResponse);
    }

    /**
     * Частичное обновление ($set редактируемых полей): документ, прочитанный до обработки
     * события блокировки, не должен перезаписать признаки storeBlocked и sellerBlocked.
     */
    @Override
    public void updateProductById(String id, ProductUpdateRequest productDetails) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> productNotFound(id));

        getModifiableStore(product.getStoreId());

        productDetails.getCategories().forEach(categoryService::getCategoryById);

        List<MultipartFile> newImages = productDetails.getImages();
        List<Image> existingImages = product.getImages() == null ? List.of() : product.getImages();
        List<Image> updatedImages = new ArrayList<>();

        for (MultipartFile file : newImages) {
            String newHash = fileStorageService.calculateHash(file);
            Optional<Image> existingImage = existingImages.stream()
                    .filter(img -> img.getHash().equals(newHash))
                    .findFirst();
            if (existingImage.isPresent()) {
                log.info("Image not changed (hash matches)");
                updatedImages.add(existingImage.get());
            } else {
                Image newImage = fileStorageService.uploadProductImage(file);
                updatedImages.add(newImage);
            }
        }

        product.setImages(updatedImages);

        productMapper.updateProductFromDTO(productDetails, product);

        Update update = new Update()
                .set("name", product.getName())
                .set("description", product.getDescription())
                .set("brand", product.getBrand())
                .set("price", product.getPrice())
                .set("images", product.getImages())
                .set("categories", product.getCategories())
                .set("characteristic", product.getCharacteristic())
                .set("tag", product.getTag())
                .set("status", product.getStatus());
        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(id)), update, Product.class);

        buildProductResponse(product);
    }

    private StoreResponse validateStoreOwnership(Long id) {
        String accessToken = currentUserService.getAccessToken();
        return sellerClient.getMyStoreById(id, accessToken);
    }

    /** Владение и текущие статусы магазина и продавца: изменения разрешены только при ACTIVE. */
    private StoreResponse getModifiableStore(Long storeId) {
        StoreResponse store = validateStoreOwnership(storeId);
        if (!ACTIVE.equals(store.getStatus()) || !ACTIVE.equals(store.getSellerStatus())) {
            throw new ResourceConflictException(String.format(MessageKeys.STORE_NOT_ACTIVE, storeId));
        }
        return store;
    }

    private static ResourceNotFoundException productNotFound(String id) {
        return new ResourceNotFoundException(String.format(MessageKeys.PRODUCT_NOT_FOUND_ID_KEY, id));
    }

    @Override
    public void deleteProductById(String id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> productNotFound(id));
        getModifiableStore(product.getStoreId());
        // До удаления документа: публикатор отправит событие, только когда товара не станет (product-deletion-sync).
        deletionEventWriter.record(product.getId(), product.getStoreId());
        productRepo.delete(product);
    }
}
