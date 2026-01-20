package ru.klimovich.catalog_service.service.impl;


import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;
import ru.klimovich.catalog_service.dto.request.CategoryRequest;
import ru.klimovich.catalog_service.dto.response.CategoryResponse;
import ru.klimovich.catalog_service.exception.ResourceConflictException;
import ru.klimovich.catalog_service.service.CategoryService;
import ru.klimovich.catalog_service.util.MessageKeys;
import ru.klimovich.catalog_service.model.Category;
import ru.klimovich.catalog_service.exception.ResourceNotFoundException;
import ru.klimovich.catalog_service.mapper.CategoryMapper;
import ru.klimovich.catalog_service.repository.CategoryRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepo;
    private final CategoryMapper categoryMapper;
    private final FileStorageService fileStorageService;

    @Override
    public CategoryResponse createCategory(@NonNull CategoryRequest categoryDetails) {

        if (categoryRepo.findByName(categoryDetails.getName()).isPresent()) {
            throw new ResourceConflictException(String
                    .format(MessageKeys.CATEGORY_ALREADY_EXIST, categoryDetails.getName()));
        }
        if (categoryDetails.getImage().isEmpty()) {
            throw new ResourceNotFoundException(String
                    .format(MessageKeys.CATEGORY_IMAGE_NOT_FOUND));
        }
        String fileName = fileStorageService.uploadFile(categoryDetails.getImage());
        Category category = categoryMapper.toEntity(categoryDetails);
        category.setPicture(fileName);
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
                        new ResourceNotFoundException(String
                                .format(MessageKeys.CATEGORY_NOT_FOUND_ID_KEY, id)));
        return categoryMapper.toDTO(category);
    }


    @Override
    public CategoryResponse updateCategoryById(String id, CategoryRequest categoryDetails) {
        Category category = categoryRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(String
                                .format(MessageKeys.CATEGORY_NOT_FOUND_ID_KEY, id)));
        MultipartFile newPicture = categoryDetails.getImage();
        if (newPicture != null && !newPicture.isEmpty()) {
            String newPictureUrl = fileStorageService.uploadFile(newPicture);
            category.setPicture(newPictureUrl);
        }
        categoryMapper.updateCategoryFromDTO(categoryDetails, category);
        return categoryMapper.toDTO(categoryRepo.save(category));
    }

    @Override
    public void deleteCategoryById(String id) {
        Category category = categoryRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(String
                                .format(MessageKeys.CATEGORY_NOT_FOUND_ID_KEY, id)));
        categoryRepo.delete(category);
    }
}
