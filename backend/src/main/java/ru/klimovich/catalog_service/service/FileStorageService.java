package ru.klimovich.catalog_service.service;

import org.springframework.web.multipart.MultipartFile;
import ru.klimovich.catalog_service.model.Image;

import java.util.List;

public interface FileStorageService {
    void ensureBucketExists(String bucketName);

    Image uploadCategoryImage(MultipartFile file);

    List<Image> uploadProductImage(List<MultipartFile> fileList);

    Image uploadProductImage(MultipartFile files);

    String calculateHash(MultipartFile file);

    void deleteAllProductImages();

    void deleteAllCategoryImage();
}
