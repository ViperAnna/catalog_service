package ru.klimovich.catalog_service.service.impl;


import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.klimovich.catalog_service.dto.request.CategoryRequest;
import ru.klimovich.catalog_service.dto.response.CategoryResponse;
import ru.klimovich.catalog_service.exception.ResourceConflictException;
import ru.klimovich.catalog_service.exception.ResourceNotFoundException;
import ru.klimovich.catalog_service.mapper.CategoryMapper;
import ru.klimovich.catalog_service.model.Category;
import ru.klimovich.catalog_service.repository.CategoryRepository;
import ru.klimovich.catalog_service.repository.ProductRepository;
import ru.klimovich.catalog_service.service.CategoryService;
import ru.klimovich.catalog_service.util.MessageKeys;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepo;
    private final CategoryMapper categoryMapper;
    private final ProductRepository productRepo;
    private final FileStorageServiceImpl fileStorageService;

    @Override
    public CategoryResponse createCategory(CategoryRequest categoryDetails) {

        if (categoryRepo.findByName(categoryDetails.getName()).isPresent()) {
            throw new ResourceConflictException(String
                    .format(MessageKeys.CATEGORY_ALREADY_EXIST, categoryDetails.getName()));
        }

        String fileName = fileStorageService.uploadCategoryImage(categoryDetails.getImage());
        Category category = categoryMapper.toEntity(categoryDetails);
        category.setImage(fileName);
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

    public Map<String, String> getNamesById(List<String> id) {
        if (id == null || id.isEmpty()) {
            return Map.of();
        }
        return categoryRepo.findAllById(id).stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        Category::getName
                ));
    }

    @Override
    public CategoryResponse updateCategoryById(String id, CategoryRequest categoryDetails) {
        Category category = categoryRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(String
                                .format(MessageKeys.CATEGORY_NOT_FOUND_ID_KEY, id)));

        MultipartFile newImage = categoryDetails.getImage();
        String newImageUrl = fileStorageService.uploadCategoryImage(newImage);
        category.setImage(newImageUrl);

        categoryMapper.updateCategoryFromDTO(categoryDetails, category);
        return categoryMapper.toDTO(categoryRepo.save(category));
    }

    @Override
    public void deleteCategoryById(String id) {
        if (!productRepo.findByCategoriesContaining(id).isEmpty()) {
            throw new ResourceConflictException(String
                    .format(MessageKeys.CATEGORY_HAS_PRODUCTS_EXCEPTION));
        }

        Category category = categoryRepo.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(String
                                .format(MessageKeys.CATEGORY_NOT_FOUND_ID_KEY, id)));
        categoryRepo.delete(category);

    }
}
