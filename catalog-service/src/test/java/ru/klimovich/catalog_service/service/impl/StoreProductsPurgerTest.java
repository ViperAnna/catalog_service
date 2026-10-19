package ru.klimovich.catalog_service.service.impl;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import ru.klimovich.catalog_service.model.Image;
import ru.klimovich.catalog_service.model.Product;
import ru.klimovich.catalog_service.outbox.ProductDeletionEventWriter;
import ru.klimovich.catalog_service.service.FileStorageService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Requirements "Purging products of deleted store" (catalog-products) и
 * "No deletion of stored images by API operations" (catalog-media), change seller-store-lifecycle.
 */
@ExtendWith(MockitoExtension.class)
class StoreProductsPurgerTest {

    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private ProductDeletionEventWriter deletionEventWriter;

    @InjectMocks
    private StoreProductsPurger purger;

    private static Image image(String hash) {
        return new Image(hash, "http://localhost:9000/products/" + hash + "_a.jpg", "a.jpg", 1L);
    }

    private static Product product(Image... images) {
        Product p = new Product();
        p.setStoreId(3L);
        p.setImages(List.of(images));
        return p;
    }

    private static boolean isSharedCheckFor(Query q, String hash) {
        if (q == null) {
            return false;
        }
        Document o = q.getQueryObject();
        return hash.equals(o.get("images.hash"))
                && new Document("$ne", 3L).equals(o.get("storeId"));
    }

    @Test
    @DisplayName("Удаляются только изображения без ссылок из других магазинов, затем документы товаров")
    void purge_deletesUnsharedImagesThenDocuments() {
        Image own = image("h1");
        Image shared = image("h2");
        when(mongoTemplate.find(any(Query.class), eq(Product.class)))
                .thenReturn(List.of(product(own, shared), product(own)));
        when(mongoTemplate.exists(argThat((Query q) -> isSharedCheckFor(q, "h1")), eq(Product.class))).thenReturn(false);
        when(mongoTemplate.exists(argThat((Query q) -> isSharedCheckFor(q, "h2")), eq(Product.class))).thenReturn(true);

        purger.purgeProductsOfStore(3L);

        verify(fileStorageService, times(1)).deleteProductImage(own);
        verify(fileStorageService, never()).deleteProductImage(shared);
        InOrder order = inOrder(fileStorageService, mongoTemplate);
        order.verify(fileStorageService).deleteProductImage(own);
        ArgumentCaptor<Query> removeQuery = ArgumentCaptor.forClass(Query.class);
        order.verify(mongoTemplate).remove(removeQuery.capture(), eq(Product.class));
        assertThat(removeQuery.getValue().getQueryObject()).isEqualTo(new Document("storeId", 3L));
    }

    @Test
    @DisplayName("Повтор после удаления: товаров нет, изображения не трогаются, ошибки нет")
    void purge_repeated_noProducts() {
        when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of());

        purger.purgeProductsOfStore(3L);

        verify(fileStorageService, never()).deleteProductImage(any());
        verify(mongoTemplate).remove(any(Query.class), eq(Product.class));
    }

    @Test
    @DisplayName("Сбой удаления изображения: документы не удаляются, исключение уходит в повторы и DLT")
    void purge_imageFailure_keepsDocuments() {
        Image own = image("h1");
        when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(product(own)));
        when(mongoTemplate.exists(any(Query.class), eq(Product.class))).thenReturn(false);
        doThrow(new RuntimeException("minio down")).when(fileStorageService).deleteProductImage(own);

        assertThatThrownBy(() -> purger.purgeProductsOfStore(3L)).hasMessage("minio down");
        verify(mongoTemplate, never()).remove(any(Query.class), eq(Product.class));
        verifyNoInteractions(deletionEventWriter);
    }

    // ---------- change product-deletion-sync ----------

    @Test
    @DisplayName("STORE_PURGED: PRODUCT_DELETED для каждого товара после изображений и до удаления документов")
    void purge_writesDeletionEventsBeforeRemove() {
        Image own = image("h1");
        Product first = product(own);
        first.setId("p-1");
        Product second = product();
        second.setId("p-2");
        when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(first, second));
        when(mongoTemplate.exists(any(Query.class), eq(Product.class))).thenReturn(false);

        purger.purgeProductsOfStore(3L);

        InOrder order = inOrder(fileStorageService, deletionEventWriter, mongoTemplate);
        order.verify(fileStorageService).deleteProductImage(own);
        order.verify(deletionEventWriter).record("p-1", 3L);
        order.verify(deletionEventWriter).record("p-2", 3L);
        order.verify(mongoTemplate).remove(any(Query.class), eq(Product.class));
    }
}
