package ru.klimovich.order_service.client;

import java.util.Collection;
import java.util.List;

/**
 * Данные товаров из catalog-service (контракт design.md D3).
 */
public interface CatalogClient {

    /**
     * Доступные товары из переданных id. Отсутствующие и недоступные id в результат не входят.
     *
     * @throws ru.klimovich.order_service.exception.CatalogServiceException каталог недоступен или ответил ошибкой
     */
    List<CatalogProduct> getProductsByIds(Collection<String> productIds, String accessToken);
}
