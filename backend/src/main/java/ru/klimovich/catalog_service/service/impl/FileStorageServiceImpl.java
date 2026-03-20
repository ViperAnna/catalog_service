package ru.klimovich.catalog_service.service.impl;

import io.minio.*;
import io.minio.messages.Item;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.klimovich.catalog_service.config.ImageBucket;
import ru.klimovich.catalog_service.model.Image;
import ru.klimovich.catalog_service.service.FileStorageService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

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
    public Image uploadCategoryImage(MultipartFile file) {
        return uploadImage(file, ImageBucket.CATEGORIES);
    }

    @Override
    public Image uploadProductImage(MultipartFile file) {
        return uploadImage(file, ImageBucket.PRODUCTS);
    }

    @Override
    public List<Image> uploadProductImage(List<MultipartFile> files) {

        return files.stream()
                .map(file -> uploadImage(file, ImageBucket.PRODUCTS))
                .toList();
    }

    private Image uploadImage(MultipartFile file, ImageBucket bucket) {
        String hash = calculateHash(file);
        String originalFileName = Optional.ofNullable(file.getOriginalFilename())
                .map(Paths::get)
                .map(Path::getFileName)
                .map(Path::toString)
                .orElse("unknown.jpg");
        String objectKey = hash + "_" + originalFileName;

        String url = minioEndpoint + "/" + bucket.getName() + "/" + objectKey;

        if (!objectExists(bucket.getName(), objectKey)) {
            uploadSingleFile(file, bucket, objectKey);
            log.info("Uploaded new image {}", objectKey);
        } else {
            log.info("Image already exists, skipping upload {}", objectKey);
        }

        Image image = new Image();
        image.setHash(hash);
        image.setUrl(url);
        image.setFileName(file.getOriginalFilename());
        image.setSize(file.getSize());

        return image;
    }

    private boolean objectExists(String bucket, String objectKey) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String calculateHash(MultipartFile file) {

        try (DigestInputStream dis = new DigestInputStream(file.getInputStream(), MessageDigest.getInstance("SHA-256"))) {
            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) {
            }
            byte[] hashBytes = dis.getMessageDigest().digest();
            return Hex.encodeHexString(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate file hash", e);
        }
    }

    @SneakyThrows
    private void uploadSingleFile(MultipartFile file, ImageBucket bucket, String objectKey) {
        log.info("Starting upload method.");
        ensureBucketExists(bucket.getName());
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket.getName())
                .object(objectKey)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
        log.info("FileStorage: minioEndpoint: {}, bucketName: {}", minioEndpoint, bucket.getName());
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

    @Override
    public void deleteAllProductImages() {
        try {
            log.info("Deleting all category image from MinIO bucket [{}]", ImageBucket.PRODUCTS);
            Iterable<Result<Item>> object = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(ImageBucket.PRODUCTS.getName())
                            .recursive(true)
                            .build()
            );
            for (Result<Item> result : object) {
                Item item = result.get();
                String objectName = item.objectName();
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(ImageBucket.PRODUCTS.getName())
                                .object(objectName)
                                .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete all product images from MinIO bucket: " + ImageBucket.PRODUCTS, e);
        }
    }

    @Override
    public void deleteAllCategoryImage() {
        try {
            log.info("Deleting all category image from MinIO bucket [{}]", ImageBucket.CATEGORIES);
            Iterable<Result<Item>> object = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(ImageBucket.CATEGORIES.getName())
                            .recursive(true)
                            .build()
            );
            for (Result<Item> result : object) {
                Item item = result.get();
                String objectName = item.objectName();
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(ImageBucket.CATEGORIES.getName())
                                .object(objectName)
                                .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete all category images from MinIO from bucket: " + ImageBucket.CATEGORIES, e);
        }
    }
}
