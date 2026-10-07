package ru.seller_service.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Requirement "Store phone normalization" (change fix-store-create-validation).
 */
class PhoneNormalizerTest {

    @Test
    @DisplayName("8XXXXXXXXXX -> +7XXXXXXXXXX")
    void eightPrefix_isNormalized() {
        assertThat(PhoneNormalizer.normalize("89991234567")).isEqualTo("+79991234567");
    }

    @Test
    @DisplayName("+7XXXXXXXXXX остается без изменений")
    void canonical_isUnchanged() {
        assertThat(PhoneNormalizer.normalize("+79991234567")).isEqualTo("+79991234567");
    }

    @Test
    @DisplayName("null остается null")
    void null_isNull() {
        assertThat(PhoneNormalizer.normalize(null)).isNull();
    }
}
