package ru.klimovich.order_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Поля товара из ответа catalog-service, нужные заказу. Остальные поля игнорируются.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogProduct {

    private String id;

    private Long storeId;

    private String name;

    private BigDecimal price;

    private String articleNumber;
}
