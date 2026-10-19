package ru.klimovich.catalog_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import ru.klimovich.catalog_service.model.Product;

/**
 * Признаки блокировки магазина и продавца в товарах (события STORE_* и SELLER_* из store-events-topic).
 * Событие задает абсолютное значение одного признака: повторная доставка ничего не меняет,
 * а разблокировка одного уровня не снимает блокировку другого. Статус товара не меняется.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductBlockingFlags {

    static final String STORE_BLOCKED = "storeBlocked";
    static final String SELLER_BLOCKED = "sellerBlocked";

    private final MongoTemplate mongoTemplate;

    public void setStoreBlocked(Long storeId, boolean blocked) {
        set(storeId, STORE_BLOCKED, blocked);
    }

    public void setSellerBlocked(Long storeId, boolean blocked) {
        set(storeId, SELLER_BLOCKED, blocked);
    }

    private void set(Long storeId, String flag, boolean value) {
        Query query = new Query(Criteria.where("storeId").is(storeId));
        long updated = mongoTemplate.updateMulti(query, new Update().set(flag, value), Product.class)
                .getModifiedCount();
        log.info("Set {} = {} for {} products of store {}", flag, value, updated, storeId);
    }
}
