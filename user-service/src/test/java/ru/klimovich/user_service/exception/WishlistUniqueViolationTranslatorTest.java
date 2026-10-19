package ru.klimovich.user_service.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Change user-service-db-constraints (D4): нарушение уникальных индексов вишлиста при гонке — 409.
 */
class WishlistUniqueViolationTranslatorTest {

    static DataIntegrityViolationException violation(String constraint, String sqlState) {
        SQLException sql = new SQLException(
                "ERROR: duplicate key value violates unique constraint \"" + constraint + "\"", sqlState);
        return new DataIntegrityViolationException("could not execute statement", sql);
    }

    @Test
    @DisplayName("uq_wishlists_user_name: 409 с именем вишлиста")
    void wishlistName() {
        assertThat(WishlistUniqueViolationTranslator.translate(violation("uq_wishlists_user_name", "23505"), "Gifts", "p-1"))
                .hasValueSatisfying(e -> assertThat(e)
                        .isInstanceOf(ResourceConflictException.class)
                        .hasMessage("The wishlist with name [Gifts] already exist"));
    }

    @Test
    @DisplayName("uq_wishlist_products: 409 с id товара")
    void wishlistProduct() {
        assertThat(WishlistUniqueViolationTranslator.translate(violation("uq_wishlist_products", "23505"), "Gifts", "p-1"))
                .hasValueSatisfying(e -> assertThat(e)
                        .hasMessage("Product with id [p-1] already exists in wishlist."));
    }

    @Test
    @DisplayName("Другое ограничение или другой SQLState: не переводится")
    void otherViolations() {
        assertThat(WishlistUniqueViolationTranslator.translate(violation("uq_users_phone", "23505"), "Gifts", "p-1")).isEmpty();
        assertThat(WishlistUniqueViolationTranslator.translate(violation("uq_wishlists_user_name", "23503"), "Gifts", "p-1")).isEmpty();
        assertThat(WishlistUniqueViolationTranslator.translate(
                new DataIntegrityViolationException("no sql cause"), "Gifts", "p-1")).isEmpty();
    }
}
