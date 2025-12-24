package ru.klimovich.catalog_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequestDTO {

    @NotBlank(message = "Product name must not be empty.", groups = CreateGroup.class)
    private String name;
    @Size(max = 500, message = "Description can't exceed 500 characters.", groups = CreateGroup.class)
    private String description;
    @Size(max = 50, message = "Brand can't exceed 50 characters.", groups = CreateGroup.class)
    private String brand;
    @NotNull(message = "Price must be provided.", groups = CreateGroup.class)
    private BigDecimal price;
    private String articleNumber;
    @NotEmpty(message = "At least one picture URL must be provided.", groups = CreateGroup.class)
    private List<String> picture;
    @NotEmpty(message = "Categories list cannot be empty.", groups = CreateGroup.class)
    private List<String> categories;
    @NotNull(message = "Characteristic details must be provided.", groups = CreateGroup.class)
    private CharacteristicRequestDTO characteristic;
    @NotEmpty(message = "At least one tag must be provided.", groups = CreateGroup.class)
    private List<String> tag;
    @Pattern(regexp = "OUT_OF_STOCK|IN_STOCK|PRE_ORDER|DRAFT", message = "Status must be one of: ACTIVE, INACTIVE, ARCHIVED, DRAFT", groups = CreateGroup.class)
    private String status;
}

