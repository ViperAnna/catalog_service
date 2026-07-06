package ru.klimovich.catalog_service.mapper;

import org.mapstruct.Mapper;
import ru.klimovich.catalog_service.dto.request.CharacteristicRequest;
import ru.klimovich.catalog_service.dto.response.CharacteristicResponse;
import ru.klimovich.catalog_service.model.Characteristic;

@Mapper(componentModel = "spring")

public interface CharacteristicMapper {
    Characteristic toEntity(CharacteristicRequest characteristicDTO);

    CharacteristicResponse toDTO(Characteristic characteristic);
}
