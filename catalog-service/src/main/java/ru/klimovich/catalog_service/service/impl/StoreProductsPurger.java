package ru.klimovich.catalog_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import ru.klimovich.catalog_service.model.Image;
import ru.klimovich.catalog_service.model.Product;
import ru.klimovich.catalog_service.service.FileStorageService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Физическое удаление товаров окончательно удаленного магазина (событие STORE_PURGED).
 * Сначала удаляются изображения, на которые не ссылаются товары других магазинов (объекты
 * дедуплицируются по содержимому), затем документы: при сбое повторная обработка находит те же
 * товары и доводит удаление до конца. Отсутствующий объект или документ — не ошибка.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoreProductsPurger {

    private final MongoTemplate mongoTemplate;
    private final FileStorageService fileStorageService;

    public void purgeProductsOfStore(Long storeId) {
        List<Product> products = mongoTemplate.find(
                new Query(Criteria.where("storeId").is(storeId)), Product.class);

        Map<String, Image> imagesByHash = new LinkedHashMap<>();
        for (Product product : products) {
            if (product.getImages() != null) {
                product.getImages().forEach(image -> imagesByHash.putIfAbsent(image.getHash(), image));
            }
        }

        int deletedImages = 0;
        for (Image image : imagesByHash.values()) {
            if (!usedByOtherStore(image.getHash(), storeId)) {
                fileStorageService.deleteProductImage(image);
                deletedImages++;
            }
        }

        mongoTemplate.remove(new Query(Criteria.where("storeId").is(storeId)), Product.class);
        log.info("Purged {} products and {} images of store {}", products.size(), deletedImages, storeId);
    }

    private boolean usedByOtherStore(String hash, Long storeId) {
        Query query = new Query(Criteria.where("images.hash").is(hash).and("storeId").ne(storeId));
        return mongoTemplate.exists(query, Product.class);
    }
}
