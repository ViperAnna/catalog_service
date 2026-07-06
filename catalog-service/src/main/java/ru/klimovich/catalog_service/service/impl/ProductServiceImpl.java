package ru.klimovich.catalog_service.service.impl;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.klimovich.catalog_service.dto.request.ProductRequest;
import ru.klimovich.catalog_service.dto.response.ProductResponse;
import ru.klimovich.catalog_service.exception.ResourceNotFoundException;
import ru.klimovich.catalog_service.mapper.ProductMapper;
import ru.klimovich.catalog_service.model.Category;
import ru.klimovich.catalog_service.model.Image;
import ru.klimovich.catalog_service.model.Product;
import ru.klimovich.catalog_service.repository.CategoryRepository;
import ru.klimovich.catalog_service.repository.ProductRepository;
import ru.klimovich.catalog_service.service.CategoryService;
import ru.klimovich.catalog_service.service.FileStorageService;
import ru.klimovich.catalog_service.service.ProductService;
import ru.klimovich.catalog_service.util.MessageKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepo;
    private final CategoryService categoryService;
    private final CategoryRepository categoryRepo;
    private final ProductMapper productMapper;
    private final FileStorageService fileStorageService;

    private ProductResponse buildProductResponse(Product product) {
        Map<String, String> categoryNames = categoryRepo.findAllById(product.getCategories())
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        return productMapper.toDTO(product, categoryNames);
    }

    @Override
    public void createProduct(@NotNull ProductRequest productDetails) {
        productDetails.getCategories().forEach(categoryService::getCategoryById);

        List<Image> fileNameList = fileStorageService.uploadProductImage(productDetails.getImages());

        Product product = productMapper.toEntity(productDetails);
        product.setImages(fileNameList);
        productRepo.save(product);
        buildProductResponse(product);
    }

    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> productPage = productRepo.findAll(pageable);

        Map<String, String> categoryInfo = categoryRepo.findAllById(
                        productPage.getContent()
                                .stream()
                                .flatMap(p -> p.getCategories().stream())
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return productPage
                .map(p -> productMapper.toDTO(p, categoryInfo));
    }

    @Override
    public ProductResponse getProductById(String id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String
                        .format(MessageKeys.PRODUCT_NOT_FOUND_ID_KEY, id)));
        return buildProductResponse(product);
    }

    @Override
    public List<ProductResponse> getProductByName(String productName) {
        List<Product> products = productRepo.findByNameIgnoreCase(productName);
        if (products.isEmpty()) {
            throw new ResourceNotFoundException(String
                    .format(MessageKeys.PRODUCT_NOT_FOUND_NAME_KEY, productName));
        }
        return products.stream()
                .map(this::buildProductResponse)
                .toList();
    }

    @Override
    public Page<ProductResponse> getProductsByCategory(Pageable pageable, String categoryId) {
        categoryRepo.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(String
                                .format(MessageKeys.CATEGORY_NOT_FOUND_ID_KEY, categoryId)));
        Page<Product> categoryList = productRepo.findByCategoriesContaining(pageable, categoryId);

        return categoryList
                .map(this::buildProductResponse);
    }

    @Override
    public void updateProductById(String id, ProductRequest productDetails) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String
                        .format(MessageKeys.PRODUCT_NOT_FOUND_ID_KEY, id)));
        List<MultipartFile> newImages = productDetails.getImages();
        List<Image> existingImages = product.getImages();
        List<Image> updatedImages = new ArrayList<>();

        for (MultipartFile file : newImages) {
            String newHash = fileStorageService.calculateHash(file);
            Optional<Image> existingImage = existingImages.stream()
                    .filter(img -> img.getHash().equals(newHash))
                    .findFirst();
            if (existingImage.isPresent()) {
                log.info("Image not changed (hash matches)");
                updatedImages.add(existingImage.get());
            } else {
                Image newImage = fileStorageService.uploadProductImage(file);
                updatedImages.add(newImage);
            }
        }

        product.setImages(updatedImages);

        productMapper.updateProductFromDTO(productDetails, product);
        productRepo.save(product);

        buildProductResponse(product);
    }

    @Override
    public void deleteProductById(String id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String
                        .format(MessageKeys.PRODUCT_NOT_FOUND_ID_KEY, id)));
        productRepo.delete(product);
    }
}
