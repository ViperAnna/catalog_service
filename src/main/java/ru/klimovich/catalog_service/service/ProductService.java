package ru.klimovich.catalog_service.service;


import ru.klimovich.catalog_service.dto.request.ProductRequestDTO;
import ru.klimovich.catalog_service.dto.response.ProductResponseDTO;

import java.util.List;

public interface ProductService {

    ProductResponseDTO createProduct(ProductRequestDTO productDetails);

    List<ProductResponseDTO> getAllProducts();

    ProductResponseDTO getProductById(String id);

    List<ProductResponseDTO> getProductByName(String productName);

    ProductResponseDTO updateProductById(String id, ProductRequestDTO productDetails);

    void deleteProductById(String id);
}
