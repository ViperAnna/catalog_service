package ru.klimovich.catalog_service.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponseDTO {

    private String id;
    private String name;
    private String description;
    private String brand;
    private BigDecimal price;
    private String articleNumber;
    private List<String> picture;
    private List<String> categories;
    private CharacteristicResponseDTO characteristic;
    private List<String> tag;
    private String status;
}
