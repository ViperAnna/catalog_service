package ru.klimovich.catalog_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.klimovich.catalog_service.aop.Loggable;
import ru.klimovich.catalog_service.dto.Response;
import ru.klimovich.catalog_service.dto.request.ProductRequest;
import ru.klimovich.catalog_service.dto.response.ProductResponse;
import ru.klimovich.catalog_service.service.impl.ProductServiceImpl;
import ru.klimovich.catalog_service.util.MessageKeys;

import java.time.LocalDateTime;
import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductServiceImpl productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Loggable
    public Response creatProduct(@Valid @RequestBody ProductRequest productRequest) {
        productService.createProduct(productRequest);
        return new Response(
                MessageKeys.PRODUCT_CREATED_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }

    @GetMapping("/{id}")
    @Loggable
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping
    @Loggable
    public ResponseEntity<Page<ProductResponse>> getAllProducts(@PageableDefault(size = 5, page = 0) Pageable pageable) {
        return ResponseEntity
                .ok(productService.getAllProducts(pageable));
    }

    @GetMapping("/productByName/{productName}")
    @Loggable
    public ResponseEntity<List<ProductResponse>> getProductByName(@PathVariable String productName) {
        return ResponseEntity.ok(productService.getProductByName(productName));
    }

    @GetMapping("/productsByCategory/{categoryName}")
    @Loggable
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable String categoryName) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryName));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    @Loggable
    public Response updateProduct(@PathVariable String id, @Valid @RequestBody ProductRequest productDetails) {
        productService.updateProductById(id, productDetails);
        return new Response(
                MessageKeys.PRODUCT_UPDATE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Loggable
    public Response deleteProduct(@PathVariable String id) {
        productService.deleteProductById(id);
        return new Response(
                MessageKeys.PRODUCT_DELETE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }
}