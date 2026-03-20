package ru.klimovich.catalog_service.initializer.product;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.klimovich.catalog_service.model.Characteristic;
import ru.klimovich.catalog_service.model.Image;
import ru.klimovich.catalog_service.model.Product;
import ru.klimovich.catalog_service.model.Status;
import ru.klimovich.catalog_service.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Service
@Setter
@Slf4j
public class ProductGenerator {

    private final ProductRepository productRepo;

    public void createProduct(int index, List<Image> imageFiles, List<String> categoryIds) {

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

    private List<Image> pickRandomImages(List<Image> images, int count) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }
        List<Image> copy = new ArrayList<>(images);
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


