package ru.klimovich.catalog_service.service.impl;


import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import ru.klimovich.catalog_service.dto.request.CategoryRequest;
import ru.klimovich.catalog_service.dto.response.CategoryResponse;
import ru.klimovich.catalog_service.exception.ResourceConflictException;
import ru.klimovich.catalog_service.util.ErrorKeys;
import ru.klimovich.catalog_service.model.Category;
import ru.klimovich.catalog_service.exception.ResourceNotFoundException;
import ru.klimovich.catalog_service.mapper.CategoryMapper;
import ru.klimovich.catalog_service.repository.CategoryRepository;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements ru.klimovich.catalog_service.service.CategoryService {
    private final CategoryRepository categoryRepo;
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryResponse createCategory(@NonNull CategoryRequest categoryDetails) {
        if (categoryRepo.findByName(categoryDetails.getName()).isPresent()) {
            throw new ResourceConflictException(MessageFormat
                    .format(ResourceBundle.getBundle("application")
                            .getString(ErrorKeys.CATEGORY_ALREADY_EXIST), categoryDetails.getName()));
        }
        Category category = categoryMapper.toEntity(categoryDetails);
        return categoryMapper.toDTO(categoryRepo.save(category));
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        List<Category> categoryList = categoryRepo.findAll();
        return categoryList.stream()
                .map(categoryMapper::toDTO)
                .toList();
    }

    @Override
    public CategoryResponse getCategoryById(String id) {
        Category category = categoryRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(MessageFormat
                                .format(ResourceBundle.getBundle("application")
                                        .getString(ErrorKeys.CATEGORY_NOT_FOUND_NAME_KEY), id)));
        return categoryMapper.toDTO(category);
    }

    @Override
    public CategoryResponse updateCategoryById(String id, CategoryRequest categoryDetails) {
        Category category = categoryRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(MessageFormat
                                .format(ResourceBundle.getBundle("application")
                                        .getString(ErrorKeys.CATEGORY_NOT_FOUND_NAME_KEY), id)));
        categoryMapper.updateCategoryFromDTO(categoryDetails, category);
        return categoryMapper.toDTO(categoryRepo.save(category));
    }

    @Override
    public void deleteCategoryById(String id) {
        Category category = categoryRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(MessageFormat
                                .format(ResourceBundle.getBundle("application")
                                        .getString(ErrorKeys.CATEGORY_NOT_FOUND_NAME_KEY), id)));
        categoryRepo.delete(category);
    }
}
