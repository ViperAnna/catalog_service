package ru.klimovich.catalog_service.service;


import ru.klimovich.catalog_service.dto.request.CategoryRequestDTO;
import ru.klimovich.catalog_service.dto.response.CategoryResponseDTO;

import java.util.List;

public interface CategoryService {

    CategoryResponseDTO createCategory(CategoryRequestDTO categoryDTO);

    List<CategoryResponseDTO> getAllCategories();

    CategoryResponseDTO getCategoryById(String id);

    CategoryResponseDTO updateCategoryById(String id, CategoryRequestDTO categoryDetails);

    void deleteCategoryById(String id);
}
