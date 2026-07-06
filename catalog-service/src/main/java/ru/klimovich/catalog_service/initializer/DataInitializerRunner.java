package ru.klimovich.catalog_service.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.klimovich.catalog_service.initializer.category.CategoryInitializer;
import ru.klimovich.catalog_service.initializer.product.ProductInitializer;

@Component
@RequiredArgsConstructor
@Profile("generate-data")
@Slf4j
public class DataInitializerRunner implements CommandLineRunner {
    private final CategoryInitializer categoryInitializer;
    private final ProductInitializer productInitializer;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting category initialization...");
        categoryInitializer.initCategories();
        log.info("Categories initialized.");

        log.info("Starting product initialization...");
        productInitializer.initProducts();
        log.info("Products initialized.");

        log.info("DataInitializerRunner completed.");
        System.exit(0);
    }
}
