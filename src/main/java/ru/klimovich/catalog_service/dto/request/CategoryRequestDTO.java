package ru.klimovich.catalog_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRequestDTO {

    @NotBlank(message = "Category name must not be empty.", groups = CreateGroup.class)
    private String name;
    @Size(max = 500, message = "Description can't exceed 500 characters.", groups = CreateGroup.class)
    private String description;
    @NotBlank(groups = CreateGroup.class)
    private String picture;
    @Pattern(regexp = "ACTIVE|INACTIVE|ARCHIVED|DRAFT", message = "Status must be one of: ACTIVE, INACTIVE, ARCHIVED, DRAFT", groups = CreateGroup.class)
    private String status;

}
