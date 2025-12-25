package ru.klimovich.catalog_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import ru.klimovich.catalog_service.dto.TemplateCategory;

@Getter
@Setter
@ToString
//@AllArgsConstructor
@NoArgsConstructor
public class CategoryRequest extends TemplateCategory {

//    @NotBlank(message = "Category name must not be empty.")
//    private String name;
//    @Size(max = 500, message = "Description can't exceed 500 characters.")
//    private String description;
//    @NotBlank
//    private String picture;
//    @Pattern(regexp = "ACTIVE|INACTIVE|ARCHIVED|DRAFT", message = "Status must be one of: ACTIVE, INACTIVE, ARCHIVED, DRAFT")
//    private String status;

}
