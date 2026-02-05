package ru.klimovich.catalog_service.tools.product;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.klimovich.catalog_service.model.Characteristic;
import ru.klimovich.catalog_service.model.Product;
import ru.klimovich.catalog_service.model.Status;
import ru.klimovich.catalog_service.repository.ProductRepository;
import ru.klimovich.catalog_service.service.ProductService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Service
@Setter
@Slf4j
public class ProductGenerator {

    private final ProductRepository productRepo;

    public void createProduct(int index, List<String> imageFiles, List<String> categoryIds) {

        Product product = new Product();
        product.setName("Product " + index);
        product.setDescription("Generated product " + index);
        product.setBrand("Brand " + (index % 5));
        product.setPrice(BigDecimal.valueOf(100 + index));
        product.setArticleNumber("ART-" + UUID.randomUUID());
        product.setImages(pickRandomImages(imageFiles, 3));
        product.setCategories(pickRandomCategories(categoryIds, 3));
        product.setCharacteristic(defaultCharacteristic());
        product.setTag(List.of("product", product.getName()));
        product.setStatus(Status.valueOf(index % 2 == 0 ? "ACTIVE" : "INACTIVE"));
        product.setDateCreate(LocalDateTime.now());
        product.setDateUpdate(LocalDateTime.now());
        productRepo.save(product);

        log.info("Created product: {}", product.getName());
    }

    private List<String> pickRandomImages(List<String> urls, int count) {
        if (urls == null || urls.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> copy = new ArrayList<>(urls);
        Collections.shuffle(copy);
        return copy.subList(0, Math.min(count, copy.size()));
    }

    private List<String> pickRandomCategories(List<String> allCategoryIds, int count) {
        if (allCategoryIds == null || allCategoryIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> copy = new ArrayList<>(allCategoryIds);
        Collections.shuffle(copy);
        return new ArrayList<>(copy.subList(0, Math.min(count, copy.size())));
    }

    private Characteristic defaultCharacteristic() {
        Characteristic request = new Characteristic();
        Map<String, String> attributes = new HashMap<>();
        attributes.put("color", "black");
        attributes.put("weight", "1kg");
        request.setAttributes(attributes);
        return request;
    }
}


