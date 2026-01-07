package ru.klimovich.catalog_service.DTO;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ItemDTO {
    private String id;
    private String name;
    private String description;
    private String brand;
    private BigDecimal price;
    private Long articleNumber;
    private List<String> picture;
    private SpecificationDTO specification;
    private List<String> tag;
    private String status;
}
