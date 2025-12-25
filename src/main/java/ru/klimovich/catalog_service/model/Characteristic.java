package ru.klimovich.catalog_service.model;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Characteristic {

    private Map<String, String> attributes;
}
