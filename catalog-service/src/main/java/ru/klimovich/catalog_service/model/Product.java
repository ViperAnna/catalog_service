package ru.klimovich.catalog_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    @Indexed
    @Field(value = "store_id")
    private Long storeId;

    @Field(value = "name")
    private String name;

    @Field(value = "description")
    private String description;

    @Field(value = "brand")
    private String brand;

    @Field(value = "price")
    private BigDecimal price;

    @Indexed(unique = true)
    @Field(value = "article_number")
    private String articleNumber;

    @Field(value = "images")
    private List<Image> images;

    @Field(value = "categories")
    private List<String> categories;

    @Field(value = "characteristic")
    private Characteristic characteristic;

    @Field(value = "tags")
    private List<String> tag;

    @Field(value = "status")
    private Status status;

    /**
     * Признаки блокировки магазина и продавца. Меняются только событиями store-events-topic
     * (ProductBlockingFlags); отсутствие поля = не заблокирован. Создание и изменение товара их не пишут.
     */
    @Field(value = "store_blocked")
    private Boolean storeBlocked;

    @Field(value = "seller_blocked")
    private Boolean sellerBlocked;

    @Field(value = "date_create")
    private LocalDateTime dateCreate;

    /** Доступен покупателю: ACTIVE и магазин и продавец не заблокированы. */
    public boolean isAvailable() {
        return status == Status.ACTIVE
                && !Boolean.TRUE.equals(storeBlocked)
                && !Boolean.TRUE.equals(sellerBlocked);
    }

    @Field(value = "date_update")
    private LocalDateTime dateUpdate;
}
