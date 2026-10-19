package ru.klimovich.order_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import ru.klimovich.order_service.exception.CatalogServiceException;
import ru.klimovich.order_service.util.MessageKeys;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebClientCatalogClient implements CatalogClient {

    private final WebClient.Builder webClient;

    @Value("${catalog-service.url}")
    private String catalogUrl;

    /**
     * POST /products/by-ids с JWT пользователя. Любой неуспешный ответ (4xx на корректный запрос,
     * 5xx), таймаут и сетевая ошибка означают недоступность каталога — 503.
     */
    @Override
    public List<CatalogProduct> getProductsByIds(Collection<String> productIds, String accessToken) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        try {
            List<CatalogProduct> products = webClient.build()
                    .post()
                    .uri(catalogUrl + "/products/by-ids")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .bodyValue(List.copyOf(productIds))
                    .retrieve()
                    .bodyToFlux(CatalogProduct.class)
                    .collectList()
                    .block();
            return products != null ? products : List.of();
        } catch (WebClientResponseException e) {
            log.warn("Catalog service responded with status {}", e.getStatusCode().value());
            throw new CatalogServiceException(MessageKeys.CATALOG_SERVICE_UNAVAILABLE);
        } catch (RuntimeException e) {
            log.warn("Catalog service request failed: {}", e.getClass().getSimpleName());
            throw new CatalogServiceException(MessageKeys.CATALOG_SERVICE_UNAVAILABLE);
        }
    }
}
