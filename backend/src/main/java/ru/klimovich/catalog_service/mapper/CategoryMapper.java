package ru.klimovich.catalog_service.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import ru.klimovich.catalog_service.DTO.CategoryDTO;
import ru.klimovich.catalog_service.entity.Category;
import ru.klimovich.catalog_service.entity.emum.StatusCategory;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)

public interface CategoryMapper {
    CategoryMapper INSTANCE = Mappers.getMapper(CategoryMapper.class);

    @Mappings({
            @Mapping(source = "status", target = "status", qualifiedByName = "mapStatusDTO")
    })
    Category toEntity(ru.klimovich.catalog_service.DTO.CategoryDTO categoryDTO);

    @Mappings({
            @Mapping(source = "status", target = "status", qualifiedByName = "mapStatus")
    })
    CategoryDTO toDTO(Category category);

    @Named("mapStatusDTO")
    default StatusCategory stringToStatus(String status) {
        if (status == null) {
            return null;
        }
        return StatusCategory.valueOf(status.toUpperCase());
    }

    @Named("mapStatus")
    default String statusToString(StatusCategory status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case ACTIVE -> "active";
            case INACTIVE -> "inactive";
            case ARCHIVED -> "archived";
            case DRAFT -> "draft";
            default -> throw new IllegalArgumentException("Unknown status: " + status);
        };
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCategoryFromDTO(CategoryDTO categoryDetails, @MappingTarget Category category);
}
