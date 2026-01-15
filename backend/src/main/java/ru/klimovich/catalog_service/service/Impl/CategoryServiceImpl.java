package ru.klimovich.catalog_service.service.Impl;


import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.klimovich.catalog_service.DTO.CategoryDTO;
import ru.klimovich.catalog_service.entity.Category;
import ru.klimovich.catalog_service.exception.CategoryNotFoundException;
import ru.klimovich.catalog_service.mapper.CategoryMapper;
import ru.klimovich.catalog_service.repository.CategoryRepository;
import ru.klimovich.catalog_service.service.CategoryService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepo;

    @Override
    public CategoryDTO createCategory(@NonNull CategoryDTO categoryDetails) {
        if (!StringUtils.hasText(categoryDetails.getName())) {
            throw new IllegalArgumentException("Category name can't be empty.");
        }
        if (categoryRepo.findByName(categoryDetails.getName()).isPresent()) {
            throw new IllegalArgumentException("Category with name [" + categoryDetails.getName() + "] already exists.");
        }

        Category category = new Category();
        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());
        category.setPicture(categoryDetails.getPicture());
        category.setStatus(CategoryMapper.INSTANCE.stringToStatus(categoryDetails.getStatus()));
        category.setDateCreate(LocalDateTime.now());
        category.setDateUpdate(LocalDateTime.now());

        return CategoryMapper.INSTANCE.toDTO(categoryRepo.save(category));
    }

    @Override
    public List<CategoryDTO> getAllCategories() {
        List<Category> categoryList = categoryRepo.findAll();
        return categoryList.stream()
                .map(CategoryMapper.INSTANCE::toDTO)
                .toList();
    }

    @Override
    public CategoryDTO getCategoryById(String id) {
        Category category = categoryRepo.findById(id)
                .orElseThrow(() ->
                        new CategoryNotFoundException("Category not found with id: " + id));
        return CategoryMapper.INSTANCE.toDTO(category);
    }

    @Override
    public CategoryDTO updateCategory(String id, CategoryDTO categoryDetails) {
        Category category = categoryRepo.findById(id)
                .orElseThrow(() ->
                        new CategoryNotFoundException("Category not found with id: " + id));
        CategoryMapper.INSTANCE.updateCategoryFromDTO(categoryDetails, category);
        return CategoryMapper.INSTANCE.toDTO(categoryRepo.save(category));
    }

    @Override
    public void deleteCategoryById(String id) {
        Category category = categoryRepo.findById(id)
                .orElseThrow(() ->
                        new CategoryNotFoundException("Category not found with id: " + id));
        /*** нужно ли добавить в категорию список товаров, а при удалении
         категории удалять все товары? ***/
        categoryRepo.delete(category);
    }
}
