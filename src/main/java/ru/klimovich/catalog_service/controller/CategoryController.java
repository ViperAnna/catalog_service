package ru.klimovich.catalog_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.klimovich.catalog_service.dto.request.CategoryRequest;
import ru.klimovich.catalog_service.dto.response.CategoryResponse;
import ru.klimovich.catalog_service.dto.Response;
import ru.klimovich.catalog_service.service.impl.CategoryServiceImpl;

import java.time.LocalDateTime;
import java.util.List;

import static ru.klimovich.catalog_service.util.MessageKeys.*;

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
                CATEGORY_CREATED_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }


    @GetMapping
    public List<CategoryResponse> getAllCategories() {
        return categoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategoryById(@PathVariable String id) {
        return categoryService.getCategoryById(id);
    }

    @PutMapping("/{id}")
    public Response updateCategory(@PathVariable String id, @Valid @RequestBody CategoryRequest categoryDetails) {
        categoryService.updateCategoryById(id, categoryDetails);
        return new Response(
                CATEGORY_UPDATE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Response deleteCategory(@PathVariable String id) {
        categoryService.deleteCategoryById(id);
        return new Response(
                CATEGORY_DELETE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }
}
