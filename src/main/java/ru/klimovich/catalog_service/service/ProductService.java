package ru.klimovich.catalog_service.service;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.klimovich.catalog_service.dto.request.ProductRequest;
import ru.klimovich.catalog_service.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest productDetails);

    Page<ProductResponse> getAllProducts(Pageable pageable);

    ProductResponse getProductById(String id);

    List<ProductResponse> getProductByName(String productName);

    ProductResponse updateProductById(String id, ProductRequest productDetails);

    void deleteProductById(String id);
}
