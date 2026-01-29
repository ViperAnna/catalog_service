package ru.klimovich.catalog_service.service.impl;

import io.minio.*;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.klimovich.catalog_service.config.ImageBucket;
import ru.klimovich.catalog_service.service.FileStorageService;

import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
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

    @SneakyThrows
    private String uploadSingleFile(MultipartFile file, ImageBucket bucket) {

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
