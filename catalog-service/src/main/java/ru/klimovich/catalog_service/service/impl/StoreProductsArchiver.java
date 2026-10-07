package ru.klimovich.catalog_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import ru.klimovich.catalog_service.model.Product;
import ru.klimovich.catalog_service.model.Status;

/**
 * Переводит в архив товары удаленного магазина (событие STORE_DELETED из
 * store-events-topic). Повторный вызов ничего не меняет: уже архивные товары
 * не попадают под условие.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoreProductsArchiver {

    private final MongoTemplate mongoTemplate;

    public long archiveProductsOfStore(Long storeId) {
        Query query = new Query(Criteria.where("storeId").is(storeId)
                .and("status").ne(Status.ARCHIVED));
        Update update = new Update().set("status", Status.ARCHIVED);

        long archived = mongoTemplate.updateMulti(query, update, Product.class).getModifiedCount();
        log.info("Archived {} products of deleted store {}", archived, storeId);
        return archived;
    }
}
