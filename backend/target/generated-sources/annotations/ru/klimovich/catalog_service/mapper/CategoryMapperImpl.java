package ru.klimovich.catalog_service.mapper;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import ru.klimovich.catalog_service.DTO.CategoryDTO;
import ru.klimovich.catalog_service.entity.Category;
import ru.klimovich.catalog_service.entity.emum.StatusCategory;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-04T16:53:39+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 18.0.2 (Amazon.com Inc.)"
)
@Component
public class CategoryMapperImpl implements CategoryMapper {

    @Override
    public Category toEntity(CategoryDTO categoryDTO) {
        if ( categoryDTO == null ) {
            return null;
        }

        Category category = new Category();

        category.setStatus( stringToStatus( categoryDTO.getStatus() ) );
        category.setId( categoryDTO.getId() );
        category.setName( categoryDTO.getName() );
        category.setDescription( categoryDTO.getDescription() );
        category.setPicture( categoryDTO.getPicture() );

        return category;
    }

    @Override
    public CategoryDTO toDTO(Category category) {
        if ( category == null ) {
            return null;
        }

        CategoryDTO categoryDTO = new CategoryDTO();

        categoryDTO.setStatus( statusToString( category.getStatus() ) );
        categoryDTO.setId( category.getId() );
        categoryDTO.setName( category.getName() );
        categoryDTO.setDescription( category.getDescription() );
        categoryDTO.setPicture( category.getPicture() );

        return categoryDTO;
    }

    @Override
    public void updateCategoryFromDTO(CategoryDTO categoryDetails, Category category) {
        if ( categoryDetails == null ) {
            return;
        }

        if ( categoryDetails.getId() != null ) {
            category.setId( categoryDetails.getId() );
        }
        if ( categoryDetails.getName() != null ) {
            category.setName( categoryDetails.getName() );
        }
        if ( categoryDetails.getDescription() != null ) {
            category.setDescription( categoryDetails.getDescription() );
        }
        if ( categoryDetails.getPicture() != null ) {
            category.setPicture( categoryDetails.getPicture() );
        }
        if ( categoryDetails.getStatus() != null ) {
            category.setStatus( Enum.valueOf( StatusCategory.class, categoryDetails.getStatus() ) );
        }
    }
}
