package ru.seller_service.dto.request.application;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Requirement "Application rejection": причина отказа обязательна (change seller-moderation-rules).
 */
class ReviewSellerApplicationRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    private static Set<String> messages(String moderatorComment) {
        return validator.validate(new ReviewSellerApplicationRequest(moderatorComment)).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Пустая причина отказа отклоняется")
    void blankReason_invalid(String reason) {
        assertThat(messages(reason)).contains("Rejection reason is required.");
    }

    @Test
    @DisplayName("Причина длиной 500 символов проходит")
    void reasonAtLimit_valid() {
        assertThat(messages("a".repeat(500))).isEmpty();
    }

    @Test
    @DisplayName("Причина длиннее 500 символов отклоняется")
    void reasonOverLimit_invalid() {
        assertThat(messages("a".repeat(501))).isNotEmpty();
    }
}
