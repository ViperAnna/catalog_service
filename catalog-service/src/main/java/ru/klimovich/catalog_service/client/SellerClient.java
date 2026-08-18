package ru.klimovich.catalog_service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.klimovich.catalog_service.dto.response.StoreResponse;

@Component
@RequiredArgsConstructor
public class SellerClient {
    private final WebClient.Builder webClient;
    @Value("${seller-service.url}")
    private String sellerUrl;

    public StoreResponse getMySoreById(Long storeId, String accessToken){
        return webClient.build()
                .get()
                .uri(sellerUrl + "/stores/{id}", storeId)
                .headers(headers ->
                        headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(StoreResponse.class)
                .block();
    }
}
