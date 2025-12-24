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

    @Field(value = "picture")
    private List<String> picture;

    @Field(value = "specification")
    private Characteristic characteristic;

    @Field(value = "tags")
    private List<String> tag;

    @Field(value = "categories")
    private List<String> categories;

    @Field(value = "status")
    private ProductStatus status;

    @Field(value = "date_create")
    private LocalDateTime dateCreate;

    @Field(value = "date_update")
    private LocalDateTime dateUpdate;
}
