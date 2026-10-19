package ru.seller_service.dto.request.seller;

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
 * Requirements "Registration application field validation", "Seller update application submission"
 * и "Seller phone normalization" (change seller-application-contact-data).
 */
class SellerRequestValidationTest {

    private static final String ACCOUNT = "12345678901234567890";

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

    private static SellerRegistrationRequest registration(String phone, String email) {
        return new SellerRegistrationRequest("Shop", "1234567890", phone, email, ACCOUNT);
    }

    private static SellerUpdateRequest update(String phone, String email) {
        return new SellerUpdateRequest(phone, email, ACCOUNT);
    }

    private static Set<String> invalidFields(Object request) {
        return validator.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static Set<String> messages(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @ParameterizedTest
    @ValueSource(strings = {"+79991234567", "89991234567"})
    @DisplayName("Регистрация: формы +7 и 8 проходят")
    void registration_validPhone(String phone) {
        assertThat(invalidFields(registration(phone, "shop@example.com"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"79991234567", "+7 999 123-45-67", "1234567", "+19991234567"})
    @DisplayName("Регистрация: прочие формы телефона отклоняются")
    void registration_invalidPhone(String phone) {
        assertThat(invalidFields(registration(phone, "shop@example.com"))).containsExactly("phoneNumber");
        assertThat(messages(registration(phone, "shop@example.com")))
                .contains("Phone must be in format +7XXXXXXXXXX or 8XXXXXXXXXX.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Регистрация: email обязателен")
    void registration_withoutEmail_invalid(String email) {
        assertThat(invalidFields(registration("+79991234567", email))).containsExactly("email");
        assertThat(messages(registration("+79991234567", email))).contains("Email is required.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"+79991234567", "89991234567"})
    @DisplayName("UPDATE: формы +7 и 8 проходят в поле phoneNumber")
    void update_validPhone(String phone) {
        assertThat(invalidFields(update(phone, "shop@example.com"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"79991234567", "+7 999 123-45-67", "1234567"})
    @DisplayName("UPDATE: прочие формы телефона отклоняются")
    void update_invalidPhone(String phone) {
        assertThat(invalidFields(update(phone, null))).containsExactly("phoneNumber");
    }

    @Test
    @DisplayName("UPDATE: без телефона -> Phone is required.")
    void update_withoutPhone_invalid() {
        assertThat(invalidFields(update(null, null))).containsExactly("phoneNumber");
        assertThat(messages(update(null, null))).contains("Phone is required.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("UPDATE: email необязателен")
    void update_withoutEmail_valid(String email) {
        assertThat(invalidFields(update("+79991234567", email))).isEmpty();
    }
}
