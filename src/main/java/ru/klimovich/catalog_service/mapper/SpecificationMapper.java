package ru.klimovich.catalog_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import ru.klimovich.catalog_service.DTO.SpecificationDTO;
import ru.klimovich.catalog_service.entity.Specification;

@Mapper(componentModel = "spring")
public interface SpecificationMapper {

    SpecificationMapper INSTANCE = Mappers.getMapper(SpecificationMapper.class);

    Specification toEntity(SpecificationDTO specificationDTO);

    SpecificationDTO toDTO(Specification specification);
}
