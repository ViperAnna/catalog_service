package ru.klimovich.catalog_service.mapper;

import org.mapstruct.*;

import ru.klimovich.catalog_service.dto.request.CategoryRequestDTO;
import ru.klimovich.catalog_service.dto.response.CategoryResponseDTO;
import ru.klimovich.catalog_service.model.Category;
import ru.klimovich.catalog_service.model.CategoryStatus;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)

public interface CategoryMapper {

    @Mapping(source = "status", target = "status", qualifiedByName = "mapStatusDTO")
    Category toEntity(CategoryRequestDTO categoryDTO);

    @Mapping(source = "status", target = "status", qualifiedByName = "mapStatus")
    CategoryResponseDTO toDTO(Category category);

    @Named("mapStatusDTO")
    default CategoryStatus stringToStatus(String status) {
        return status != null ? CategoryStatus.valueOf(status.toUpperCase()) : CategoryStatus.DRAFT;
    }

    @Named("mapStatus")
    default String statusToString(CategoryStatus status) {
        return status.name().toLowerCase();
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCategoryFromDTO(CategoryRequestDTO categoryDetails, @MappingTarget Category category);
}
