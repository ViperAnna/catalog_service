package ru.klimovich.catalog_service.service;


import ru.klimovich.catalog_service.dto.request.CategoryRequest;
import ru.klimovich.catalog_service.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    void createCategory(CategoryRequest categoryDTO);

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(String id);

    void updateCategoryById(String id, CategoryRequest categoryDetails);

    void deleteCategoryById(String id);
}
