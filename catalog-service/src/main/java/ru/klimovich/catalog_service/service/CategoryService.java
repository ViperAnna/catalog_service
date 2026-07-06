package ru.klimovich.catalog_service.service;

import ru.klimovich.catalog_service.dto.request.CategoryRequest;
import ru.klimovich.catalog_service.dto.request.CategoryUpdateRequest;
import ru.klimovich.catalog_service.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    void createCategory(CategoryRequest categoryDTO);

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(String id);

    CategoryResponse updateCategoryById(String id, CategoryUpdateRequest categoryDetails);

    void deleteCategoryById(String id);
}
