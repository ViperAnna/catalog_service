package ru.klimovich.catalog_service.mapper;

import org.mapstruct.*;
import ru.klimovich.catalog_service.dto.request.CategoryRequest;
import ru.klimovich.catalog_service.dto.response.CategoryResponse;
import ru.klimovich.catalog_service.model.Category;
import ru.klimovich.catalog_service.model.Image;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)

public interface CategoryMapper {

    @Mapping(target = "image", ignore = true)
    @Mapping(target = "id", ignore = true)
    Category toEntity(CategoryRequest categoryDTO);

    @Mapping(source = "image", target = "imageUrl", qualifiedByName = "mapImageToDTO")
    CategoryResponse toDTO(Category category);

    @Named("mapImageToDTO")
    default String map(Image image) {
        return image != null ? image.getUrl() : null;
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "image", ignore = true)
    void updateCategoryFromDTO(CategoryRequest categoryDetails, @MappingTarget Category category);
}
