package ru.klimovich.catalog_service.dto.response;

import lombok.*;
import ru.klimovich.catalog_service.dto.TemplateCategory;


import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor

    public class CategoryResponse extends TemplateCategory {

    private String id;
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;
}