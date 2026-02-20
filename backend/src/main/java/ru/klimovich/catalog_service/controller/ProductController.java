package ru.klimovich.catalog_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
@Tag(name = "Product Controller", description = "API для упреавления продуктами")
public class ProductController {

    private final ProductServiceImpl productService;

    @Operation(
            summary = "Создание нового продукта",
            description = "Создает новый продукт с предоставленными данными"
    )
    @ApiResponse(responseCode = "201", description = "Продукт успешно создан")

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Response creatProduct(@Valid @ModelAttribute ProductRequest productRequest) {
        productService.createProduct(productRequest);
        return new Response(
                MessageKeys.PRODUCT_CREATED_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }

    @Operation(
            summary = "Получение продукта по ID",
            description = "Возвращает продукт по его уникальному идентификатору"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Продукт найден"),
            @ApiResponse(responseCode = "404", description = "Продукт не найден")
    })
    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable String id) {
        return productService.getProductById(id);
    }

    @Operation(
            summary = "Получение всех продуктов",
            description = "Возвращает постранично список товаров"
    )
    @ApiResponse(responseCode = "200", description = "Страница списка всех категорий успешно вохрващена")
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(@PageableDefault(size = 5, page = 0) Pageable pageable,
                                                                @RequestParam(required = false) String categoryId) {
        if (categoryId != null) {
            return ResponseEntity.ok(
                    productService.getProductsByCategory(pageable, categoryId));
        }
        return ResponseEntity
                .ok(productService.getAllProducts(pageable));
    }

    @Operation(
            summary = "Получение продукта по имени",
            description = "Возвращает список продуктов, соответствующих заданному названию"
    )
    @ApiResponse(responseCode = "200", description = "Список всех продуктов по соотвествующему названию успешно возвращен")
    @GetMapping("/productByName/{productName}")
    public ResponseEntity<List<ProductResponse>> getProductByName(@PathVariable String productName) {
        return ResponseEntity.ok(productService.getProductByName(productName));
    }

    @Operation(
            summary = "Обновление продукта",
            description = "Обновляет категорию с уникальным идентификатором по предоставленным данным"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Продукт успешно изменен"),
            @ApiResponse(responseCode = "404", description = "Продукт не найден")
    })
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    public Response updateProduct(@PathVariable String id, @Valid @RequestBody ProductRequest productDetails) {
        productService.updateProductById(id, productDetails);
        return new Response(
                MessageKeys.PRODUCT_UPDATE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }

    @Operation(
            summary = "Удаление продукта",
            description = "Удаление продукта по уникальному идентификатору"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Продукт успешно удален"),
            @ApiResponse(responseCode = "404", description = "Продукт не найден")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Response deleteProduct(@PathVariable String id) {
        productService.deleteProductById(id);
        return new Response(
                MessageKeys.PRODUCT_DELETE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }
}