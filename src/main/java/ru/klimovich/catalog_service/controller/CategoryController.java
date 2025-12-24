package ru.klimovich.catalog_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.klimovich.catalog_service.dto.request.CategoryRequestDTO;
import ru.klimovich.catalog_service.dto.request.CreateGroup;
import ru.klimovich.catalog_service.dto.response.CategoryResponseDTO;
import ru.klimovich.catalog_service.model.Response;
import ru.klimovich.catalog_service.service.impl.CategoryServiceImpl;
import ru.klimovich.catalog_service.util.ErrorKeys;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryServiceImpl categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response createCategory(@Valid @RequestBody CategoryRequestDTO categoryDTO) {
        categoryService.createCategory(categoryDTO);
        return new Response(
                ErrorKeys.CATEGORY_CREATED_SUCCESSFULLY,
                HttpStatus.CREATED.value(),
                LocalDateTime.now()
        );
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> getCategoryById(@PathVariable String id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Response> updateCategory(@PathVariable String id, @RequestBody CategoryRequestDTO categoryDetails) {
        categoryService.updateCategoryById(id, categoryDetails);
        Response body = new Response(
                ErrorKeys.CATEGORY_UPDATE_SUCCESSFULLY,
                HttpStatus.CREATED.value(),
                LocalDateTime.now()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(body);

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response> deleteCategory(@PathVariable String id) {
        categoryService.deleteCategoryById(id);
        Response body = new Response(
                ErrorKeys.CATEGORY_DELETE_SUCCESSFULLY,
                HttpStatus.CREATED.value(),
                LocalDateTime.now()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(body);
    }
}
