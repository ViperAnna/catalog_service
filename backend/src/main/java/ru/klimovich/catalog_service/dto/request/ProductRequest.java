package ru.klimovich.catalog_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name must not be empty.")
    private String name;

    @NotBlank
    @Size(max = 500, message = "Description can't exceed 500 characters.")
    private String description;

    @NotBlank
    @Size(max = 50, message = "Brand can't exceed 50 characters.")
    private String brand;

    @NotNull(message = "Price must be provided.")
    private BigDecimal price;

    @NotEmpty
    private List<MultipartFile> images;

    private String articleNumber;

    @NotEmpty(message = "Categories list cannot be empty.")
    private List<String> categories;

    @NotNull(message = "Characteristic details must be provided.")
    private CharacteristicRequest characteristic;

    @NotEmpty(message = "At least one tag must be provided.")
    private List<String> tag;

    @Pattern(regexp = "ACTIVE|INACTIVE|ARCHIVED|DRAFT", message = "Status must be one of: ACTIVE, INACTIVE, ARCHIVED, DRAFT")
    private String status;
}