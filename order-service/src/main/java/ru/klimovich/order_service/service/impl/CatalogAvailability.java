package ru.klimovich.order_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.klimovich.order_service.client.CatalogClient;
import ru.klimovich.order_service.client.CatalogProduct;
import ru.klimovich.order_service.domain.AvailableProduct;
import ru.klimovich.order_service.domain.CatalogItemPolicy;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Актуальные данные товаров из каталога, отфильтрованные CatalogItemPolicy.
 * Отсутствие productId в результате означает, что позиция недоступна.
 */
@Component
@RequiredArgsConstructor
public class CatalogAvailability {

    private final CatalogClient catalogClient;

    public Map<String, AvailableProduct> availableProducts(Collection<String> productIds, String accessToken) {
        LinkedHashSet<String> ids = new LinkedHashSet<>(productIds);
        Map<String, CatalogProduct> byId = catalogClient.getProductsByIds(ids, accessToken).stream()
                .filter(p -> p.getId() != null)
                .collect(Collectors.toMap(CatalogProduct::getId, Function.identity(), (a, b) -> a));

        Map<String, AvailableProduct> result = new HashMap<>();
        for (String id : ids) {
            CatalogItemPolicy.evaluate(id, byId.get(id)).ifPresent(p -> result.put(id, p));
        }
        return result;
    }
}
