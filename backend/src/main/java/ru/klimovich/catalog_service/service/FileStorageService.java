package ru.klimovich.catalog_service.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {
    void ensureBucketExists(String bucketName);
    String uploadCategoryImage(MultipartFile file);
    List<String> uploadProductImage(List<MultipartFile> fileList);
    String uploadProductImage(MultipartFile files);
    void deleteAllProductImages();
    void deleteAllCategoryImage();
}
