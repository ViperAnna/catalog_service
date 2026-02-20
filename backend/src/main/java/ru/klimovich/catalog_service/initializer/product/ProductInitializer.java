package ru.klimovich.catalog_service.initializer.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.klimovich.catalog_service.repository.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductInitializer {

    private final ImageUploadService imageUploadService;
    private final ProductRunner productRunner;
    private final ProductRepository productRepo;
    private static final int TOTAL_IMAGES = 1000;
    private static final int TOTAL_PRODUCTS = 10_000;
    private static final int MIN_PRODUCTS_THRESHOLD = 9000;
    private static final int WIDTH = 300;
    private static final int HEIGHT = 300;

    public void initProducts() throws Exception {
        long existingCount = productRepo.count();
        log.info("Product initialization: {} existing products found", existingCount);

        if (existingCount >= MIN_PRODUCTS_THRESHOLD) {
            log.info("More than {} products exist. Using existing products, no initialization needed.", MIN_PRODUCTS_THRESHOLD);
            return;
        }

        int productsToAdd = TOTAL_PRODUCTS - (int) existingCount;
        log.info("Need to add {} products to reach {} total", productsToAdd, TOTAL_PRODUCTS);

        log.info("Getting or uploading images for products...");
        List<String> uploadedImages = imageUploadService.getOrUploadImages(TOTAL_IMAGES, WIDTH, HEIGHT);
        log.info("Total images available for products: {}", uploadedImages.size());

        log.info("Generating {} new products...", productsToAdd);
        productRunner.generateProducts(productsToAdd, uploadedImages);

        log.info("Product initialization complete. Total products now: {}", productRepo.count());
    }
}