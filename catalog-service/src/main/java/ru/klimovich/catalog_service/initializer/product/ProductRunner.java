package ru.klimovich.catalog_service.initializer.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.klimovich.catalog_service.model.Category;
import ru.klimovich.catalog_service.model.Image;
import ru.klimovich.catalog_service.repository.CategoryRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductRunner {

    private final ProductGenerator productGenerator;
    private final CategoryRepository categoryRepo;
    private static final int THREADS = 8;

    public void generateProducts(int totalProducts, List<Image> images) throws InterruptedException {
        List<String> categoryIds = categoryRepo.findAll()
                .stream()
                .map(Category::getId)
                .toList();
        if (categoryIds.isEmpty()) {
            log.warn("No categories found. Product generation skipped");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        AtomicInteger counter = new AtomicInteger();

        for (int i = 1; i <= totalProducts; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    productGenerator.createProduct(index, images, categoryIds);

                    int current = counter.incrementAndGet();
                    if (current % 100 == 0) {
                        log.info("Created {} products", current);
                    }

                } catch (Exception e) {
                    log.error(" Failed to create product {}", index, e);
                }
            });
        }

        executor.shutdown();
        if (!executor.awaitTermination(2, TimeUnit.HOURS)) {
            log.error("Product generation did not finish in time.");
            executor.shutdownNow();
        }
        log.info("All {} products created", totalProducts);
    }
}


