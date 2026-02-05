package ru.klimovich.catalog_service.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.klimovich.catalog_service.tools.product.ProductInitializer;
import ru.klimovich.catalog_service.tools.category.CategoryInitializer;

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

    }
}
