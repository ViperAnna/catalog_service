package ru.klimovich.catalog_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.klimovich.catalog_service.dto.TemplateProduct;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Schema(description = "Формирование ответа для *Продукт*")
public class ProductResponse extends TemplateProduct {

    @Schema(description = "ID продукта", example = "698c08f7569bbd7a36602fe5")
    private String id;
}
