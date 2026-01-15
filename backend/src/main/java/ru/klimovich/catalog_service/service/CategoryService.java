package ru.klimovich.catalog_service.service;

import ru.klimovich.catalog_service.DTO.CategoryDTO;

import java.util.List;

public interface CategoryService {

    CategoryDTO createCategory(CategoryDTO categoryDTO);

    List<CategoryDTO> getAllCategories();

    CategoryDTO getCategoryById(String id);

    CategoryDTO updateCategory(String id, CategoryDTO categoryDetails);

    void deleteCategoryById(String id);
}
