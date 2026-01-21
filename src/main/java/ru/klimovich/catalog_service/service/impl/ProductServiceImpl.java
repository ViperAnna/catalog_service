package ru.klimovich.catalog_service.service.impl;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import ru.klimovich.catalog_service.dto.request.ProductRequest;
import ru.klimovich.catalog_service.dto.response.ProductResponse;
import ru.klimovich.catalog_service.model.Category;
import ru.klimovich.catalog_service.repository.CategoryRepository;
import ru.klimovich.catalog_service.service.ProductService;
import ru.klimovich.catalog_service.util.MessageKeys;
import ru.klimovich.catalog_service.exception.ResourceNotFoundException;
import ru.klimovich.catalog_service.model.Product;
import ru.klimovich.catalog_service.mapper.ProductMapper;
import ru.klimovich.catalog_service.repository.ProductRepository;
import ru.klimovich.catalog_service.service.CategoryService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepo;
    private final CategoryService categoryService;
    private final CategoryRepository categoryRepo;
    private final ProductMapper productMapper;

    @Override
    public ProductResponse createProduct(@NotNull ProductRequest productDetails) {
        List<String> categoriesIdList = productDetails.getCategories();
        for (String categoryId : categoriesIdList) {
            categoryService.getCategoryById(categoryId);
        }
        Product product = productMapper.toEntity(productDetails);
        productRepo.save(product);
        Map<String, String> categoryNames = categoryRepo.findAllById(product.getCategories())
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        return productMapper.toDTO(product, categoryNames);
    }

    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> productPage = productRepo.findAll(pageable);

        Map<String,String> categoryInfo = categoryRepo.findAllById(
                productPage.getContent()
                        .stream()
                        .flatMap(p -> p.getCategories().stream())
                        .distinct()
                        .toList()
        ).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));


        return productPage
                .map(p->productMapper.toDTO(p,categoryInfo));
    }

    @Override
    public ProductResponse getProductById(String id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String
                        .format(MessageKeys.PRODUCT_NOT_FOUND_ID_KEY, id)));
        Map<String, String> categoriesNames = categoryRepo.findAllById(product.getCategories())
                .stream()
                .collect(Collectors.toMap(Category::getId,Category::getName));
        return productMapper.toDTO(product, categoriesNames);
    }

    @Override
    public List<ProductResponse> getProductByName(String productName) {
        List<Product> products = productRepo.findByNameIgnoreCase(productName);
        if (products.isEmpty()) {
            throw new ResourceNotFoundException(String
                    .format(MessageKeys.PRODUCT_NOT_FOUND_NAME_KEY, productName));
        }
        List<String> categoryId = products.stream()
                .flatMap(p-> p.getCategories().stream())
                .distinct()
                .toList();
        Map<String, String> categoriesNames = categoryRepo.findAllById(categoryId)
                .stream()
                .collect(Collectors.toMap(Category::getId,Category::getName));

        return products.stream()
                .map(p -> productMapper.toDTO(p, categoriesNames))
                .toList();
    }
    @Override
    public List<ProductResponse> getProductsByCategory(String categoryName){
        Category category = categoryRepo.findByName(categoryName)
                .orElseThrow(() ->
                        new ResourceNotFoundException(String
                                .format(MessageKeys.CATEGORY_NOT_FOUND_NAME_KEY, categoryName)));
        List<Product> categoryList = productRepo.findByCategoriesContaining(category.getId());
        List<String> categoryIds = categoryList.stream()
                .flatMap(p -> p.getCategories().stream())
                .distinct()
                .toList();

        Map<String, String> categoryNames = categoryRepo.findAllById(categoryIds)
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        return categoryList.stream()
                .map(p -> productMapper.toDTO(p, categoryNames))
                .toList();
    }

    @Override
    public ProductResponse updateProductById(String id, ProductRequest productDetails) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String
                        .format(MessageKeys.PRODUCT_NOT_FOUND_ID_KEY, id)));
        productMapper.updateProductFromDTO(productDetails, product);
        productRepo.save(product);
        List<String> categoryIds = product.getCategories();
        Map<String, String> categoryNames = categoryRepo.findAllById(categoryIds)
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        return productMapper.toDTO(product, categoryNames);
    }

    @Override
    public void deleteProductById(String id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String
                        .format(MessageKeys.PRODUCT_NOT_FOUND_ID_KEY, id)));
        productRepo.delete(product);
    }
}
