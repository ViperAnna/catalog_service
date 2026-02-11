package ru.klimovich.catalog_service.initializer.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.klimovich.catalog_service.repository.ProductRepository;
import ru.klimovich.catalog_service.service.FileStorageService;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductInitializer {

    private final ImageUploadService imageUploadService;
    private final ProductRunner productRunner;
    private final ProductRepository productRepo;
    private final FileStorageService fileStorageService;

    private static final int TOTAL_IMAGES = 1000;
    private static final int TOTAL_PRODUCTS = 10_000;
    private static final int WIDTH = 300;
    private static final int HEIGHT = 300;

    public void initProducts() throws Exception {
        if (productRepo.count() > 0) {
            log.info("Product initialization:delete all existing product images...");
            fileStorageService.deleteAllProductImages();
            log.info("Product initialization:delete all existing products...");
            productRepo.deleteAll();
        }

        log.info("Uploading images to MinIO...");
        List<String> uploadedImages = imageUploadService.uploadRandomImages(TOTAL_IMAGES, WIDTH, HEIGHT);
        log.info(" Uploaded {} images", uploadedImages.size());

        log.info("Generating products...");
        productRunner.generateProducts(TOTAL_PRODUCTS, uploadedImages);

        log.info("All products generated successfully");
    }
}


