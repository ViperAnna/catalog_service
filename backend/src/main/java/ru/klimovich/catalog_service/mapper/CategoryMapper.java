package ru.klimovich.catalog_service.mapper;

import org.mapstruct.*;

import ru.klimovich.catalog_service.dto.request.CategoryRequest;
import ru.klimovich.catalog_service.dto.response.CategoryResponse;
import ru.klimovich.catalog_service.model.Category;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)

public interface CategoryMapper {

    @Mapping(target = "picture", ignore = true)
    @Mapping(target = "id", ignore = true)
    Category toEntity(CategoryRequest categoryDTO);

    @Mapping(source = "picture", target = "pictureUrl")
    CategoryResponse toDTO(Category category);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCategoryFromDTO(CategoryRequest categoryDetails, @MappingTarget Category category);
}
