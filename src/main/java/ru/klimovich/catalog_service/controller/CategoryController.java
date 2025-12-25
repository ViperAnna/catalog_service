package ru.klimovich.catalog_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.klimovich.catalog_service.dto.request.CategoryRequest;
import ru.klimovich.catalog_service.dto.response.CategoryResponse;
import ru.klimovich.catalog_service.dto.Response;
import ru.klimovich.catalog_service.service.impl.CategoryServiceImpl;
import ru.klimovich.catalog_service.util.ErrorKeys;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryServiceImpl categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response createCategory(@Valid @RequestBody CategoryRequest categoryRequest) {
        categoryService.createCategory(categoryRequest);
        return new Response(
                ErrorKeys.CATEGORY_CREATED_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    public Response updateCategory(@PathVariable String id, @RequestBody CategoryRequest categoryDetails) {
        categoryService.updateCategoryById(id, categoryDetails);
        return new Response(
                ErrorKeys.CATEGORY_UPDATE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Response deleteCategory(@PathVariable String id) {
        categoryService.deleteCategoryById(id);
        return new Response(
                ErrorKeys.CATEGORY_DELETE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }
}
