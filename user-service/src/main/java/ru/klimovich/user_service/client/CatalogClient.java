package ru.klimovich.user_service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.klimovich.user_service.dto.responce.ItemResponse;
import ru.klimovich.user_service.exception.CatalogServiceException;
import ru.klimovich.user_service.exception.ProductNotFoundException;
import ru.klimovich.user_service.util.MessageKeys;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogClient {

    private final WebClient webClient;

    @Value("${catalog-service.url}")
    private String catalogUrl;

    public ItemResponse getProductById(String productId, String token) {
        return webClient
                .get()
                .uri(catalogUrl + "/products/{id}", productId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> Mono.error(
                                new ProductNotFoundException(
                                        MessageKeys.PRODUCT_NOT_FOUND,
                                        productId
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
                .bodyToMono(ItemResponse.class)
                .block()
                ;
    }


    public List<ItemResponse> getProductsByIds(List<String> productIds, String token) {
        return webClient
                .post()
                .uri(catalogUrl + "/products/by-ids")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(productIds)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> Mono.error(
                                new RuntimeException("Product not found")
                        )
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> Mono.error(
                                new RuntimeException("Catalog service unavailable")
                        )
                )
                .bodyToFlux(ItemResponse.class)
                .collectList()
                .block();
    }
}