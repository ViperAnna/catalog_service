package ru.klimovich.catalog_service.initializer.product;

import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.klimovich.catalog_service.model.Image;
import ru.klimovich.catalog_service.service.FileStorageService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageUploadService {

    private final FileStorageService fileStorageService;
    private static final Path TEMP_DIR = Paths.get("/temp/images/products");
    private static final int THREADS = 8;

    private final MinioClient minioClient;
    private static final String BUCKET_NAME = "products";
    @Value("${minio.endpoint}")
    private String minioEndpoint;


    public List<Image> listExistingImages() throws Exception {
        List<Image> images = new ArrayList<>();

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(BUCKET_NAME)
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();
            Image image = new Image();
            image.setUrl(minioEndpoint +"/" + BUCKET_NAME + "/" + item.objectName());
            image.setFileName(item.objectName());
            image.setSize(item.size());

            images.add(image);
        }

        return images;
    }

    public List<Image> getOrUploadImages(int totalImages, int width, int height) throws Exception {
        List<Image> existing = listExistingImages();
        int missing = totalImages - existing.size();
        if (missing <= 0) {
            log.info("There are enough images in MinIO. Using the existing {} images.", existing.size());
            return existing;
        }

        log.info("{} images missing, uploading {} new images...", missing, missing);
        List<Image> newImages = uploadRandomImages(missing, width, height);
        existing.addAll(newImages);
        return existing;
    }

    public List<Image> uploadRandomImages(int totalImages, int width, int height) throws IOException, InterruptedException {
        if (!Files.exists(TEMP_DIR)) {
            Files.createDirectories(TEMP_DIR);
            log.info("Created temp directory: [{}]", TEMP_DIR.toAbsolutePath());
        }

        List<Image> uploadedImages = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        for (int i = 1; i <= totalImages; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(100);
                    String imageUrl = String.format("https://picsum.photos/%d/%d?random=%d", width, height, index);
                    Path localFile = TEMP_DIR.resolve("img_" + UUID.randomUUID() + ".jpg");

                    try (InputStream is = new URL(imageUrl).openStream()) {
                        Files.copy(is, localFile, StandardCopyOption.REPLACE_EXISTING);
                    }

                    try (InputStream fileInput = Files.newInputStream(localFile)) {
                        MultipartFile multipartFile = new MockMultipartFile(
                                "file",
                                localFile.getFileName().toString(),
                                "image/jpeg",
                                fileInput
                        );

                        Image uploadedImage = fileStorageService.uploadProductImage(multipartFile);
                        uploadedImages.add(uploadedImage);
                        log.info("[{}] Uploaded to MinIO: {}", index, uploadedImage);
                    }

                    Files.deleteIfExists(localFile);

                } catch (Exception e) {
                    log.error("Failed to upload image {}", index, e);
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.MINUTES);

        return uploadedImages;
    }
}