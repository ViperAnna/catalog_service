package ru.klimovich.catalog_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import ru.klimovich.catalog_service.entity.emum.StatusItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "items")
public class Item {

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
    private Long articleNumber;

    @Field(value = "picture")
    private List<String> picture;

    @Field(value = "specification")
    private Specification specification;

    @Field(value = "tags")
    private List<String> tag;

    @Field(value = "status")
    private StatusItem status;

    @Field(value = "date_create")
    private LocalDateTime dateCreate;

    @Field(value = "date_update")
    private LocalDateTime dateUpdate;

    public Item(String name, String description, String brand, BigDecimal price, Long articleNumber, StatusItem status) {
        this();
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.price = price;
        this.articleNumber = articleNumber;
        this.status = status != null ? status : StatusItem.DRAFT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Objects.equals(getId(), item.getId()) && Objects.equals(getName(), item.getName()) && Objects.equals(getDescription(), item.getDescription()) && Objects.equals(getBrand(), item.getBrand()) && Objects.equals(getPrice(), item.getPrice()) && Objects.equals(getArticleNumber(), item.getArticleNumber()) && Objects.equals(getPicture(), item.getPicture()) && Objects.equals(getSpecification(), item.getSpecification()) && Objects.equals(getTag(), item.getTag()) && getStatus() == item.getStatus() && Objects.equals(getDateCreate(), item.getDateCreate()) && Objects.equals(getDateUpdate(), item.getDateUpdate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getDescription(), getBrand(), getPrice(), getArticleNumber(), getPicture(), getSpecification(), getTag(), getStatus(), getDateCreate(), getDateUpdate());
    }
}
