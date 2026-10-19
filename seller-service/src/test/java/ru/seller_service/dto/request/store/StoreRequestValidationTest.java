package ru.seller_service.dto.request.store;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Requirements "Store field validation", "Store creation" и
 * "Store phone normalization" (change fix-store-create-validation).
 */
class StoreRequestValidationTest {

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

    private static StoreRequest create(String description, String email, String phone) {
        return new StoreRequest("Shop", description, email, phone);
    }

    private static Set<String> invalidFields(Object request) {
        Set<ConstraintViolation<Object>> violations = validator.validate(request);
        return violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    @DisplayName("Store creation: без email -> нарушение")
    void create_withoutEmail_invalid() {
        assertThat(invalidFields(create(null, null, "+79991234567"))).contains("email");
    }

    @Test
    @DisplayName("Store creation: без телефона -> нарушение")
    void create_withoutPhone_invalid() {
        assertThat(invalidFields(create(null, "shop@example.com", null))).contains("phoneNumber");
    }

    @ParameterizedTest
    @ValueSource(strings = {"79991234567", "+7 999 123-45-67", "abc", "+12025550123"})
    @DisplayName("Store phone normalization: недопустимые формы телефона -> нарушение")
    void create_invalidPhone_invalid(String phone) {
        assertThat(invalidFields(create(null, "shop@example.com", phone))).contains("phoneNumber");
    }

    @ParameterizedTest
    @ValueSource(strings = {"+79991234567", "89991234567"})
    @DisplayName("Store phone normalization: +7XXXXXXXXXX и 8XXXXXXXXXX допустимы")
    void create_validPhone_valid(String phone) {
        assertThat(invalidFields(create(null, "shop@example.com", phone))).isEmpty();
    }

    @Test
    @DisplayName("Store field validation: description 256 символов -> нарушение")
    void create_description256_invalid() {
        assertThat(invalidFields(create("a".repeat(256), "shop@example.com", "+79991234567")))
                .contains("description");
    }

    @Test
    @DisplayName("Store field validation: description ровно 255 символов допустим")
    void create_description255_valid() {
        assertThat(invalidFields(create("a".repeat(255), "shop@example.com", "+79991234567"))).isEmpty();
    }

    // --- StoreUpdateRequest (PUT /stores/{id}) ---

    @Test
    @DisplayName("Store update: без email и телефона нарушений нет")
    void update_withoutEmailAndPhone_valid() {
        assertThat(invalidFields(new StoreUpdateRequest("Shop", null, null, null))).isEmpty();
    }

    @Test
    @DisplayName("Store update: storeName по-прежнему обязателен")
    void update_withoutStoreName_invalid() {
        assertThat(invalidFields(new StoreUpdateRequest(null, null, null, null))).contains("storeName");
    }

    @ParameterizedTest
    @ValueSource(strings = {"79991234567", "+7 999 123-45-67"})
    @DisplayName("Store phone normalization: при изменении недопустимый телефон -> нарушение")
    void update_invalidPhone_invalid(String phone) {
        assertThat(invalidFields(new StoreUpdateRequest("Shop", null, null, phone))).contains("phoneNumber");
    }

    @Test
    @DisplayName("Store field validation: при изменении description 256 символов -> нарушение")
    void update_description256_invalid() {
        assertThat(invalidFields(new StoreUpdateRequest("Shop", "a".repeat(256), null, null))).contains("description");
    }
}
