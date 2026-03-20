package ru.klimovich.catalog_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;
import ru.klimovich.catalog_service.util.imageUtils.ValidImage;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "Запрос на создание/обновление категории")
public class CategoryRequest {

    @NotBlank(message = "Category name is required.")
    @Schema(description = "Имя категории", example = "Цифровая техника")
    private String name;

    @Size(max = 500, message = "Description can't exceed 500 characters.")
    @Schema(description = "Описание категории", example = "Фотокамеры, дроны, видеокамеры")
    private String description;

    @NotNull(message = "Image is required.")
    @ValidImage(allowedTypes = {"image/jpeg", "image/jpg", "image/pjpeg", "application/octet-stream"})
    @Schema(description = "Картинка категории", example = "image/categories/digital_devices.jpg")
    private MultipartFile image;

    @Pattern(regexp = "ACTIVE|INACTIVE|ARCHIVED|DRAFT", message = "Status must be one of: ACTIVE, INACTIVE, ARCHIVED, DRAFT")
    @Schema(description = "Статус категории", example = "ACTIVE")
    private String status;
}
