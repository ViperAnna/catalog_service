package ru.klimovich.catalog_service.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Document(collection = "categories")
public class Category {
    @Id
    private String id;

    @Field(value = "name")
    private String name;

    @Field(value = "description")
    private String description;

    @Field(value = "picture")
    private String picture;

    @Field(value = "status")
    private CategoryStatus status;

    @CreatedDate
    @Field(value = "date_create")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field(value = "date_update")
    private LocalDateTime updatedAt;
}
