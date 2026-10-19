package ru.klimovich.user_service.client;

import io.netty.handler.timeout.ReadTimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import ru.klimovich.user_service.exception.CatalogServiceException;
import ru.klimovich.user_service.exception.ProductNotFoundException;

import java.net.ConnectException;
import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Requirement "Adding product to wishlist" и "Wishlist error responses" (change wishlist-catalog-timeouts):
 * сетевые ошибки и таймауты каталога дают CatalogServiceException (503), как ответ 5xx.
 */
class CatalogClientTest {

    private static final String UNAVAILABLE = "Catalog service is unavailable.";

    private static CatalogClient client(ExchangeFunction exchange) {
        CatalogClient client = new CatalogClient(WebClient.builder().exchangeFunction(exchange));
        ReflectionTestUtils.setField(client, "catalogUrl", "http://CATALOG-SERVICE");
        return client;
    }

    private static ExchangeFunction failing(Throwable cause) {
        return request -> Mono.error(new WebClientRequestException(
                cause, HttpMethod.GET, URI.create("http://CATALOG-SERVICE"), new HttpHeaders()));
    }

    private static ExchangeFunction status(HttpStatus status) {
        return request -> Mono.just(ClientResponse.create(status).build());
    }

    @Test
    @DisplayName("Отказ соединения при получении товара: 503")
    void connectionRefusedOnProduct() {
        assertThatThrownBy(() -> client(failing(new ConnectException("Connection refused")))
                .getProductById("p-1", "token"))
                .isInstanceOf(CatalogServiceException.class)
                .hasMessage(UNAVAILABLE);
    }

    @Test
    @DisplayName("Таймаут ответа при получении товара: 503")
    void readTimeoutOnProduct() {
        assertThatThrownBy(() -> client(failing(ReadTimeoutException.INSTANCE))
                .getProductById("p-1", "token"))
                .isInstanceOf(CatalogServiceException.class)
                .hasMessage(UNAVAILABLE);
    }

    @Test
    @DisplayName("Отказ соединения при получении списка товаров: 503")
    void connectionRefusedOnProducts() {
        assertThatThrownBy(() -> client(failing(new ConnectException("Connection refused")))
                .getProductsByIds(Set.of("p-1"), "token"))
                .isInstanceOf(CatalogServiceException.class)
                .hasMessage(UNAVAILABLE);
    }

    @Test
    @DisplayName("Ответ 404 по-прежнему дает ProductNotFoundException")
    void notFoundStillMapped() {
        assertThatThrownBy(() -> client(status(HttpStatus.NOT_FOUND)).getProductById("p-1", "token"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product with id [p-1] not found.");
    }

    @Test
    @DisplayName("Ответ 500 по-прежнему дает CatalogServiceException")
    void serverErrorStillMapped() {
        assertThatThrownBy(() -> client(status(HttpStatus.INTERNAL_SERVER_ERROR)).getProductsByIds(Set.of("p-1"), "token"))
                .isInstanceOf(CatalogServiceException.class)
                .hasMessage(UNAVAILABLE);
    }
}
