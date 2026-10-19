package ru.klimovich.user_service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import ru.klimovich.user_service.dto.responce.ProductResponse;
import ru.klimovich.user_service.exception.CatalogServiceException;
import ru.klimovich.user_service.exception.ProductNotFoundException;
import ru.klimovich.user_service.util.MessageKeys;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CatalogClient {

    private final WebClient.Builder webClient;

    @Value("${catalog-service.url}")
    private String catalogUrl;


    public ProductResponse getProductById(String productId, String accessToken) {
        return webClient
                .build()
                .get()
                .uri(catalogUrl + "/products/{id}", productId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> Mono.error(
                                new ProductNotFoundException(
                                        MessageKeys.PRODUCT_NOT_FOUND, productId
                                )
                        )
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> Mono.error(
                                new CatalogServiceException(
                                        MessageKeys.CATALOG_SERVICE_UNAVAILABLE
                                )
                        )
                )
                .bodyToMono(ProductResponse.class)
                .onErrorMap(WebClientRequestException.class, CatalogClient::catalogUnavailable)
                .block()
                ;
    }


    public List<ProductResponse> getProductsByIds(Set<String> productIds, String accessToken) {
        return webClient
                .build()
                .post()
                .uri(catalogUrl + "/products/by-ids")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .bodyValue(productIds)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> Mono.error(
                                        new ProductNotFoundException(
                                                MessageKeys.PRODUCTS_NOT_FOUND)
                        )
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> Mono.error(
                                new CatalogServiceException(
                                        MessageKeys.CATALOG_SERVICE_UNAVAILABLE
                                )
                        )
                )
                .bodyToFlux(ProductResponse.class)
                .collectList()
                .onErrorMap(WebClientRequestException.class, CatalogClient::catalogUnavailable)
                .block();
    }

    /**
     * Отказ соединения, таймаут подключения или ответа и другие сетевые ошибки — та же недоступность
     * каталога, что и ответ 5xx.
     */
    private static CatalogServiceException catalogUnavailable(WebClientRequestException e) {
        return new CatalogServiceException(MessageKeys.CATALOG_SERVICE_UNAVAILABLE);
    }
}