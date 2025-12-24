package ru.klimovich.catalog_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.klimovich.catalog_service.dto.request.CreateGroup;
import ru.klimovich.catalog_service.dto.request.ProductRequestDTO;
import ru.klimovich.catalog_service.dto.response.ProductResponseDTO;
import ru.klimovich.catalog_service.service.impl.ProductServiceImpl;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductServiceImpl productService;

    @PostMapping
    public ResponseEntity<ProductResponseDTO> creatProduct(@Validated(CreateGroup.class) @RequestBody ProductRequestDTO productDTO) {
        ProductResponseDTO product = productService.createProduct(productDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(product);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {
        return ResponseEntity
                .ok(productService.getAllProducts());
    }

    @GetMapping("/productByName/{productName}")
    public ResponseEntity<List<ProductResponseDTO>> getProductByName(@PathVariable String productName) {
        return ResponseEntity.ok(productService.getProductByName(productName));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(@PathVariable String id, @RequestBody ProductRequestDTO productDetails) {
        return ResponseEntity.ok(productService.updateProductById(id, productDetails));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProductById(id);
        return ResponseEntity.noContent().build();
    }
}
