package ru.klimovich.catalog_service.util.imageUtils;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {SingleImageValidator.class,
        ListImageValidator.class})
@Documented
public @interface ValidImage {
    String message() default "Invalid image file. Only allowed MIME types are accepted.";

    String[] allowedTypes() default {"image/jpeg"};

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
