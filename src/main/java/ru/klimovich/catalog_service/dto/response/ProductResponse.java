package ru.klimovich.catalog_service.dto.response;

import lombok.*;
import ru.klimovich.catalog_service.dto.TemplateProduct;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ProductResponse extends TemplateProduct {

    private String id;
}
