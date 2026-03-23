package ru.klimovich.catalog_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на создание/обновление характеристик продукта")
public class CharacteristicRequest {

    @Schema(description = "Атрибуты характеристики", example = "Цвет: черный, Вес: 1кг")
    private Map<String, String> attributes;
}