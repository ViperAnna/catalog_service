package ru.klimovich.catalog_service.tools.category;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import ru.klimovich.catalog_service.dto.response.CategoryResponse;
import ru.klimovich.catalog_service.model.Category;
import ru.klimovich.catalog_service.model.Status;
import ru.klimovich.catalog_service.repository.CategoryRepository;
import ru.klimovich.catalog_service.service.FileStorageService;

import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Paths;;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor

@Slf4j
public class CategoryInitializer {
    private final ObjectMapper objectMapper;
    private final CategoryRepository categoryRepo;
    private final FileStorageService fileStorageService;

    @Value("${app.init.categories:false}")
    private boolean initCategories;

    public List<String> initCategories() throws Exception {
        List<String> categoryIds = new ArrayList<>();


        if (!initCategories) {
            log.info("Category initialization disabled app.init.categories=false");
            return categoryRepo.findAll()
                    .stream()
                    .map(Category::getId)
                    .toList();
        }

        if (categoryRepo.count() > 0) {
            log.info("Category initialization:delete all existing category images...");
            fileStorageService.deleteAllCategoryImage();
            log.info("Category initialization:delete all existing categories...");
            categoryRepo.deleteAll();
        }

        log.info("Initializing categories from JSON");
        ClassPathResource jsonResource = new ClassPathResource("data/categories.json");
        List<CategoryResponse> categories = objectMapper.readValue(
                jsonResource.getInputStream(),
                new TypeReference<>() {
                }
        );

        for (CategoryResponse dto : categories) {

            ClassPathResource imageResource = new ClassPathResource(dto.getImageUrl());
            if (!imageResource.exists()) {
                log.info("File not found: " + imageResource.getFilename());
                continue;
            }

            try (InputStream is = imageResource.getInputStream()) {
                String fileName = Paths.get(dto.getImageUrl()).getFileName().toString();
                log.info("MongoInit: fileName: " + fileName);
                String contentType = URLConnection.guessContentTypeFromStream(is);
                if (contentType == null) contentType = "image/jpeg";

                MultipartFile multipartFile = new MockMultipartFile(
                        "file",
                        fileName,
                        contentType,
                        is
                );

                String imageUrl = fileStorageService.uploadCategoryImage(multipartFile);
                log.info("Mongoinit:  imageUrl: {}", imageUrl);
                Category category = new Category();
                category.setName(dto.getName());
                category.setDescription(dto.getDescription());
                category.setImage(imageUrl);
                category.setStatus(Status.valueOf(dto.getStatus()));
                category.setCreatedAt(dto.getCreatedAt());
                category.setUpdatedAt(dto.getUpdatedAt());
                categoryRepo.save(category);
                categoryIds.add(category.getId());
                log.info("Created category: {} ", category.getName());
            }
        }
        log.info("Categories initialization competed.");
        return categoryIds;
    }
}

