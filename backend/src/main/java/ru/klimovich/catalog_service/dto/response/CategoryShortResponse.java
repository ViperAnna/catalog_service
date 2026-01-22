package ru.klimovich.catalog_service.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CategoryShortResponse{
    private String id;
    @NotBlank(message = "Category name must not be empty.")
    private String name;
}
