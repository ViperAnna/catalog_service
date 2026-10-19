package ru.seller_service.util;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class UniqueViolationsTest {

    private static DataIntegrityViolationException violation(String sqlState, String constraint) {
        return new DataIntegrityViolationException("could not execute statement",
                new RuntimeException("wrapper",
                        new SQLException("duplicate key value violates unique constraint \"" + constraint + "\"", sqlState)));
    }

    @Test
    void matchesNamedConstraintInCauseChain() {
        assertThat(UniqueViolations.isUniqueViolation(violation("23505", "uq_a"), "uq_b", "uq_a")).isTrue();
    }

    @Test
    void otherConstraintName_notMatched() {
        assertThat(UniqueViolations.isUniqueViolation(violation("23505", "uq_other"), "uq_a")).isFalse();
    }

    @Test
    void otherSqlState_notMatched() {
        assertThat(UniqueViolations.isUniqueViolation(violation("23502", "uq_a"), "uq_a")).isFalse();
    }

    @Test
    void noSqlException_notMatched() {
        assertThat(UniqueViolations.isUniqueViolation(new DataIntegrityViolationException("x"), "uq_a")).isFalse();
    }
}
