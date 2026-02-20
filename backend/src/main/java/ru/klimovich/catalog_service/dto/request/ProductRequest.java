package ru.klimovich.catalog_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import ru.klimovich.catalog_service.util.imageUtils.ValidImage;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на создание/обновление продукта")
public class ProductRequest {

    @NotBlank(message = "Product name is required.")
    @Schema(description = "Имя продукта", example = "Фотокамера")
    private String name;

    @NotBlank
    @Size(max = 500, message = "Description can't exceed 500 characters.")
    @Schema(description = "Описание продукта",
            example = "Цифровая камера")
    private String description;

    @NotBlank
    @Size(max = 50, message = "Brand can't exceed 50 characters.")
    @Schema(description = "Бренд продукта",
            example = "Canon")
    private String brand;

    @NotNull(message = "Price is required.")
    @Schema(description = "Цена продукта",
            example = "2000")
    private BigDecimal price;

    @NotEmpty(message = "At least one image is required.")
    @ValidImage(allowedTypes = {"image/jpeg", "image/jpg"})
    @Schema(description = "Картинка продукта",
            example = "http://minio:9000/products/6cfa2b98-5854-49b9-a0bb-fdb0ad6e4cbc_img_38f6a8ad-2f63-4182-a1f9-9cf287995978.jpg")
    private List<MultipartFile> images;

    @Schema(description = "Артикул продукта",
            example = "ART-daac8cdb-6a57-4a12-86c7-4239a0aecc52")
    private String articleNumber;

    @NotEmpty(message = "Categories list is required.")
    @Schema(description = "Категория продукта",
            example = "id: 698c0886569bbd7a36602fcd, " +
                    "name: Фотокамеры")
    private List<String> categories;

    @NotNull(message = "Characteristic details is required.")
    @Schema(description = "Характеристики продукта",
            example = "Цвет: черный, " +
                    "Вес: 600г")
    private CharacteristicRequest characteristic;

    @NotEmpty(message = "At least one tag is required.")
    @Schema(description = "Тэг продукта",
            example = "Техника, фотокамера")
    private List<String> tag;

    @Pattern(regexp = "ACTIVE|INACTIVE|ARCHIVED|DRAFT", message = "Status must be one of: ACTIVE, INACTIVE, ARCHIVED, DRAFT")
    @Schema(description = "Статус продукта", example = "ACTIVE")
    private String status;
}