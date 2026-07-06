package ru.klimovich.catalog_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import ru.klimovich.catalog_service.dto.request.CharacteristicRequest;
import ru.klimovich.catalog_service.dto.response.CategoryShortResponse;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Schema(description = "Формирование ответа для *Продукт*")
public abstract class TemplateProduct {

    @NotBlank(message = "Product name must not be empty.")
    @Schema(description = "Имя продукта", example = "Фотокамера")
    private String name;

    @Size(max = 500, message = "Description can't exceed 500 characters.")
    @Schema(description = "Описание продукта",
            example = "Цифровая камера")
    private String description;

    @Size(max = 50, message = "Brand can't exceed 50 characters.")
    @Schema(description = "Бренд продукта",
            example = "Canon")
    private String brand;

    @NotNull(message = "Price must be provided.")
    @Schema(description = "Цена продукта",
            example = "2000")
    private BigDecimal price;

    @Schema(description = "Артикул продукта",
            example = "ART-daac8cdb-6a57-4a12-86c7-4239a0aecc52")
    private String articleNumber;

    @NotEmpty(message = "At least one image URL must be provided.")
    @Schema(description = "Картинка продукта",
            example = "http://minio:9000/products/6cfa2b98-5854-49b9-a0bb-fdb0ad6e4cbc_img_38f6a8ad-2f63-4182-a1f9-9cf287995978.jpg")
    private List<String> imagesUrl;

    @NotEmpty(message = "Categories list cannot be empty.")
    @Schema(description = "Категория продукта",
            example = "id: 698c0886569bbd7a36602fcd, " +
                    "name: Фотокамеры")
    private List<CategoryShortResponse> categories;

    @NotNull(message = "Characteristic details must be provided.")
    @Schema(description = "Характеристики продукта",
            example = "Цвет: черный, " +
                    "Вес: 600г")
    private CharacteristicRequest characteristic;

    @NotEmpty(message = "At least one tag must be provided.")
    @Schema(description = "Тэг продукта",
            example = "Техника, фотокамера")
    private List<String> tag;

    @Pattern(regexp = "OUT_OF_STOCK|IN_STOCK|PRE_ORDER|DRAFT", message = "Status must be one of: ACTIVE, INACTIVE, ARCHIVED, DRAFT")
    @Schema(description = "Статус продукта", example = "ACTIVE")
    private String status;
}