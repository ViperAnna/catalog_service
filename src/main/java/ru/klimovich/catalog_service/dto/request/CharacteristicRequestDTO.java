package ru.klimovich.catalog_service.dto.request;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class CharacteristicRequestDTO {
    private Map<String, String> attributes;
}
