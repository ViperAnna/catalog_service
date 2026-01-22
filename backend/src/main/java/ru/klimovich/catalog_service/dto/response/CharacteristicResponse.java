package ru.klimovich.catalog_service.dto.response;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CharacteristicResponse {

    private Map<String, String> attributes;
}