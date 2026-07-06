package ru.klimovich.catalog_service.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "Формирование ответа для *Катеогрия*")
public class CategoryResponse {
    @Schema(description = "ID категории", example = "698c29d7c3ec452364c56756")
    private String id;

    @NotBlank(message = "Category name must not be empty.")
    @Schema(description = "Имя категории", example = "Цифровая техника")
    private String name;

    @Size(max = 500, message = "Description can't exceed 500 characters.")
    @Schema(description = "Описание категории", example = "Фотокамеры, дроны, видеокамеры")
    private String description;

    @NotBlank
    @Schema(description = "Картинка категории", example = "http://minio:9000/categories/ad4e043e-63d3-4de4-8ed9-2e1c700334ac_sport.jpg")
    private String imageUrl;

    @Pattern(regexp = "ACTIVE|INACTIVE|ARCHIVED|DRAFT", message = "Status must be one of: ACTIVE, INACTIVE, ARCHIVED, DRAFT")
    @Schema(description = "Category status", example = "ACTIVE")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Дата создания категории", example = "2026-02-11 00:00:00")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Дата последнего изменения категории", example = "2026-02-11 00:00:00")
    private LocalDateTime updatedAt;
}