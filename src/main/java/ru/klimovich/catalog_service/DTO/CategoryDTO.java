package ru.klimovich.catalog_service.DTO;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDTO {
    private String id;
    private String name;
    private String description;
    private String picture;
    private String status;
}
