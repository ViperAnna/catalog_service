package ru.klimovich.catalog_service.util.imageUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public class SingleImageValidator implements ConstraintValidator<ValidImage, MultipartFile> {
    private Set<String> allowedTypes;

    @Override
    public void initialize(ValidImage constraintAnnotation) {
        this.allowedTypes = Set.of(constraintAnnotation.allowedTypes());
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null) {
            return true;
        }
        if (file.isEmpty()) {
            return false;
        }
        return ImageValidationUtils.hasValidMimeType(file, allowedTypes);
    }
}
