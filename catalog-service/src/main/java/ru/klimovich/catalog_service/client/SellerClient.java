package ru.klimovich.catalog_service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.klimovich.catalog_service.dto.response.StoreResponse;
import ru.klimovich.catalog_service.exception.ResourceNotFoundException;
import ru.klimovich.catalog_service.exception.SellerServiceUnavailableException;
import ru.klimovich.catalog_service.util.MessageKeys;

@Component
@RequiredArgsConstructor
public class SellerClient {
    private final WebClient.Builder webClient;
    @Value("${seller-service.url}")
    private String sellerUrl;

    public StoreResponse getMyStoreById(Long storeId, String accessToken) {
        return webClient.build()
                .get()
                .uri(sellerUrl + "/stores/{id}", storeId)
                .headers(headers ->
                        headers.setBearerAuth(accessToken))
                .retrieve()
                .onStatus(s -> s.value() == 404 || s.value() == 403,
                        r -> Mono.error(new ResourceNotFoundException(
                                String.format(MessageKeys.STORE_NOT_FOUND_OR_FORBIDDEN, storeId))))
                .onStatus(status -> status.is5xxServerError(),
                        response -> Mono.error(
                                new SellerServiceUnavailableException()))
                .bodyToMono(StoreResponse.class)
                .block();
    }
}
