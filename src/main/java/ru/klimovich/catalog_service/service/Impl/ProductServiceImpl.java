package ru.klimovich.catalog_service.service.impl;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import ru.klimovich.catalog_service.dto.request.ProductRequest;
import ru.klimovich.catalog_service.dto.response.ProductResponse;
import ru.klimovich.catalog_service.util.ErrorKeys;
import ru.klimovich.catalog_service.exception.ResourceNotFoundException;
import ru.klimovich.catalog_service.model.Product;
import ru.klimovich.catalog_service.mapper.ProductMapper;
import ru.klimovich.catalog_service.repository.ProductRepository;
import ru.klimovich.catalog_service.service.CategoryService;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

@Service
@AllArgsConstructor
public class ProductServiceImpl implements ru.klimovich.catalog_service.service.ProductService {

    private final ProductRepository productRepo;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;

    @Override
    public ProductResponse createProduct(@NotNull ProductRequest productDetails) {
        List<String> categoriesIdList = productDetails.getCategories();
        for (String categoryId : categoriesIdList) {
            categoryService.getCategoryById(categoryId);
        }
        Product product = productMapper.toEntity(productDetails);
        return productMapper.toDTO(productRepo.save(product));
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        List<Product> productList = productRepo.findAll();
        return productList.stream()
                .map(productMapper::toDTO)
                .toList();
    }

    @Override
    public ProductResponse getProductById(String id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageFormat
                        .format(ResourceBundle.getBundle("application")
                                .getString(ErrorKeys.CATEGORY_NOT_FOUND_NAME_KEY), id)));
        return productMapper.toDTO(product);
    }

    @Override
    public List<ProductResponse> getProductByName(String productName) {
        List<Product> products = productRepo.findByNameIgnoreCase(productName);
        if (products.isEmpty()) {
            throw new ResourceNotFoundException(MessageFormat
                    .format(ResourceBundle.getBundle("application")
                            .getString(ErrorKeys.CATEGORY_NOT_FOUND_NAME_KEY), productName));
        }
        return products.stream()
                .map(productMapper::toDTO)
                .toList();
    }

    @Override
    public ProductResponse updateProductById(String id, ProductRequest productDetails) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageFormat
                        .format(ResourceBundle.getBundle("application")
                                .getString(ErrorKeys.CATEGORY_NOT_FOUND_NAME_KEY), id)));
        productMapper.updateProductFromDTO(productDetails, product);
        return productMapper.toDTO(productRepo.save(product));
    }

    @Override
    public void deleteProductById(String id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageFormat
                        .format(ResourceBundle.getBundle("application")
                                .getString(ErrorKeys.CATEGORY_NOT_FOUND_NAME_KEY), id)));
        productRepo.delete(product);
    }
}
