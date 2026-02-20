package ru.klimovich.catalog_service.util.imageUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

public class ListImageValidator implements ConstraintValidator<ValidImage, List<MultipartFile>> {
    private Set<String> allowedTypes;

    @Override
    public void initialize(ValidImage constraintAnnotation) {
        this.allowedTypes = Set.of(constraintAnnotation.allowedTypes());

    }

    @Override
    public boolean isValid(List<MultipartFile> files, ConstraintValidatorContext context) {
        if (files == null) {
            return true;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                return false;
            }
            if (!ImageValidationUtils.hasValidMimeType(file, allowedTypes)) {
                return false;
            }
        }
        return true;
    }
}
