package ru.klimovich.catalog_service.service.impl;

import io.minio.*;
import io.minio.messages.Item;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.klimovich.catalog_service.config.ImageBucket;
import ru.klimovich.catalog_service.service.FileStorageService;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {
    private final MinioClient minioClient;
    private final String minioEndpoint;

    public FileStorageServiceImpl(MinioClient minioClient,
                                  @Value("${minio.endpoint}") String minioEndpoint) {
        this.minioClient = minioClient;
        this.minioEndpoint = minioEndpoint;
    }

    @Override
    public String uploadCategoryImage(MultipartFile file) {
        return uploadSingleFile(file, ImageBucket.CATEGORIES);
    }

    @Override
    public List<String> uploadProductImage(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        return files.stream()
                .map(file -> uploadSingleFile(file, ImageBucket.PRODUCTS))
                .toList();
    }

    @Override
    public String uploadProductImage(MultipartFile file) {
        return uploadSingleFile(file, ImageBucket.PRODUCTS);

    }

    @Override
    public void deleteAllProductImages() {
        try {
            Iterable<Result<Item>> object = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(ImageBucket.PRODUCTS.toString())
                            .recursive(true)
                            .build()
            );
            for (Result<Item> result : object) {
                Item item = result.get();
                String objectName = item.objectName();
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(ImageBucket.PRODUCTS.toString())
                                .object(objectName)
                                .build()
                );
            }

        }
        catch (Exception e){
            throw  new RuntimeException("Failed to delete all product images from MinIO", e);
        }
    }

    @SneakyThrows
    private String uploadSingleFile(MultipartFile file, ImageBucket bucket) {
        log.info("Staring upload method.");
        String originalFileName = Objects.requireNonNullElse(file.getOriginalFilename(), "unknown.jpg");
        originalFileName = Paths.get(originalFileName).getFileName().toString();
        String objectKey = UUID.randomUUID().toString() + "_" + originalFileName;
        ensureBucketExists(bucket.getName());

        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket.getName())
                .object(objectKey)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
        log.info("FileStorage: minioEndpoint: {}, backetName: {}", minioEndpoint, bucket.getName());
        return minioEndpoint + "/" + bucket.getName() + "/" + objectKey;
    }

    @SneakyThrows
    public void ensureBucketExists(String bucketName) {

        boolean found = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build());
        if (!found) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build());
        }
    }
}
