package ru.klimovich.catalog_service.dto.response;

import lombok.*;
import ru.klimovich.catalog_service.dto.TemplateCategory;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
//@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse extends TemplateCategory {

    private String id;
//    private String name;
//    private String description;
//    private String picture;
//    private String status;
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;


}
