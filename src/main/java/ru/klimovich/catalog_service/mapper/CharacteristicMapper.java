package ru.klimovich.catalog_service.mapper;

import org.mapstruct.Mapper;

import ru.klimovich.catalog_service.dto.request.CharacteristicRequestDTO;
import ru.klimovich.catalog_service.dto.response.CharacteristicResponseDTO;
import ru.klimovich.catalog_service.model.Characteristic;

@Mapper(componentModel = "spring")

public interface CharacteristicMapper {
    Characteristic toEntity(CharacteristicRequestDTO characteristicDTO);

    CharacteristicResponseDTO toDTO(Characteristic characteristic);
}
