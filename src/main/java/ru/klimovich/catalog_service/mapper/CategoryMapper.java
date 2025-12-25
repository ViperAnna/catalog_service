package ru.klimovich.catalog_service.mapper;

import org.mapstruct.*;

import ru.klimovich.catalog_service.dto.request.CategoryRequest;
import ru.klimovich.catalog_service.dto.response.CategoryResponse;
import ru.klimovich.catalog_service.model.Category;
import ru.klimovich.catalog_service.model.CategoryStatus;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)

public interface CategoryMapper {

    @Mapping(source = "status", target = "status", qualifiedByName = "mapStatusDTO")
    Category toEntity(CategoryRequest categoryDTO);

    @Mapping(source = "status", target = "status", qualifiedByName = "mapStatus")
    CategoryResponse toDTO(Category category);

    @Named("mapStatusDTO")
    default CategoryStatus stringToStatus(String status) {
        return status != null ? CategoryStatus.valueOf(status.toUpperCase()) : CategoryStatus.DRAFT;
    }

    @Named("mapStatus")
    default String statusToString(CategoryStatus status) {
        return status.name().toLowerCase();
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCategoryFromDTO(CategoryRequest categoryDetails, @MappingTarget Category category);
}
