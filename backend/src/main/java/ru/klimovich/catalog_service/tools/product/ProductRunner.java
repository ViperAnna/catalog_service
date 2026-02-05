package ru.klimovich.catalog_service.tools.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    private static final int THREADS = 8;

    public void generateProducts(int totalProducts, List<String> images) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        AtomicInteger counter = new AtomicInteger();

        for (int i = 1; i <= totalProducts; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    productGenerator.createProduct(index, images);

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
        executor.awaitTermination(2, TimeUnit.HOURS);
        log.info("All {} products created", totalProducts);
    }
}


