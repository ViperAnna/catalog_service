package ru.klimovich.catalog_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Формирование короткого ответа для *Катеогрия*")
public class CategoryShortResponse {
    @Schema(description = "ID категории", example = "698c29d7c3ec452364c56756")
    private String id;

    @NotBlank(message = "Category name must not be empty.")
    @Schema(description = "Имя категории", example = "Цифровая техника")
    private String name;
}
