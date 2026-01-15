package ru.klimovich.catalog_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import ru.klimovich.catalog_service.entity.emum.StatusCategory;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "categories")
public class Category {
    @Id
    private String id;

    @Field(value = "name")
    private String name;

//    private List<Item> listOfItems;

    @Field(value = "description")
    private String description;

    @Field(value = "picture")
    private String picture;

    @Field(value = "status")
    private StatusCategory status;

    @Field(value = "date_create")
    private LocalDateTime dateCreate;

    @Field(value = "date_update")
    private LocalDateTime dateUpdate;

    public Category(String name, String description, String picture, StatusCategory status) {
        this.name = name;
        this.description = description;
        this.picture = picture;
        this.status = status != null ? status : StatusCategory.DRAFT;
        this.dateCreate = LocalDateTime.now();
        this.dateUpdate = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(getId(), category.getId()) && Objects.equals(getName(), category.getName()) && Objects.equals(getDescription(), category.getDescription()) && Objects.equals(getPicture(), category.getPicture()) && getStatus() == category.getStatus() && Objects.equals(getDateCreate(), category.getDateCreate()) && Objects.equals(getDateUpdate(), category.getDateUpdate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getDescription(), getPicture(), getStatus(), getDateCreate(), getDateUpdate());
    }
}
