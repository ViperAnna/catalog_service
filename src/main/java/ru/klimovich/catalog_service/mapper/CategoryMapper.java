package ru.klimovich.catalog_service.mapper;

import org.mapstruct.*;

import ru.klimovich.catalog_service.dto.request.CategoryRequest;
import ru.klimovich.catalog_service.dto.response.CategoryResponse;
import ru.klimovich.catalog_service.model.Category;
import ru.klimovich.catalog_service.model.CategoryStatus;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)

public interface CategoryMapper {

    Category toEntity(CategoryRequest categoryDTO);

    CategoryResponse toDTO(Category category);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCategoryFromDTO(CategoryRequest categoryDetails, @MappingTarget Category category);
}
