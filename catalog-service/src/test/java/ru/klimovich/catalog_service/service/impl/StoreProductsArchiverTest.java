package ru.klimovich.catalog_service.service.impl;

import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import ru.klimovich.catalog_service.model.Product;
import ru.klimovich.catalog_service.model.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Requirement "Archiving products of deleted store" (catalog-products,
 * change archive-products-of-deleted-store).
 */
@ExtendWith(MockitoExtension.class)
class StoreProductsArchiverTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private StoreProductsArchiver archiver;

    @Test
    @DisplayName("Архивация: updateMulti по storeId и status != ARCHIVED, меняется только status")
    void archiveProductsOfStore_updatesOnlyNotArchivedProductsOfStore() {
        when(mongoTemplate.updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Product.class)))
                .thenReturn(UpdateResult.acknowledged(2, 2L, null));

        long archived = archiver.archiveProductsOfStore(3L);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<UpdateDefinition> update = ArgumentCaptor.forClass(UpdateDefinition.class);
        verify(mongoTemplate).updateMulti(query.capture(), update.capture(), eq(Product.class));

        Document criteria = query.getValue().getQueryObject();
        assertThat(criteria.get("storeId")).isEqualTo(3L);
        assertThat(criteria.get("status")).isEqualTo(new Document("$ne", Status.ARCHIVED));

        Document set = (Document) update.getValue().getUpdateObject().get("$set");
        assertThat(set).containsOnlyKeys("status");
        assertThat(set.get("status")).isEqualTo(Status.ARCHIVED);
        assertThat(update.getValue().getUpdateObject()).containsOnlyKeys("$set");

        assertThat(archived).isEqualTo(2L);
    }

    @Test
    @DisplayName("Архивация: повтор и магазин без товаров не дают ошибки")
    void archiveProductsOfStore_nothingToUpdate_returnsZero() {
        when(mongoTemplate.updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Product.class)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, null));

        assertThat(archiver.archiveProductsOfStore(3L)).isZero();
    }
}
