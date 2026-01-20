package ru.klimovich.catalog_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ru.klimovich.catalog_service.dto.request.CategoryRequest;
import ru.klimovich.catalog_service.dto.response.CategoryResponse;
import ru.klimovich.catalog_service.dto.Response;
import ru.klimovich.catalog_service.service.impl.CategoryServiceImpl;

import java.time.LocalDateTime;
import java.util.List;

import static ru.klimovich.catalog_service.util.MessageKeys.*;

@Slf4j
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryServiceImpl categoryService;

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Response createCategory(@ModelAttribute CategoryRequest categoryRequest) {
        categoryService.createCategory(categoryRequest);
        log.info("Successfully created category with name: {}", categoryRequest.getName());
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
    public Response updateCategory(@PathVariable String id, @ModelAttribute CategoryRequest categoryDetails) {
        categoryService.updateCategoryById(id, categoryDetails);
        log.info("Successfully update category with id: {}", id);
        return new Response(
                CATEGORY_UPDATE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Response deleteCategory(@PathVariable String id) {
        categoryService.deleteCategoryById(id);
        log.info("Successfully delete category with id: {}", id);
        return new Response(
                CATEGORY_DELETE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }
}
