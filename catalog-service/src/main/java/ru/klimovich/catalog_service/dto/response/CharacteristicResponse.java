package ru.klimovich.catalog_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ на создание/обновление характеристик продукта")
public class CharacteristicResponse {

    @Schema(description = "Атрибуты характеристики", example = "Цвет: черный, Вес: 1кг")
    private Map<String, String> attributes;
}