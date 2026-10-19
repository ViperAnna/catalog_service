package ru.seller_service.util;

import java.sql.SQLException;

/**
 * Распознает нарушение уникального ограничения PostgreSQL (SQLState 23505) по имени ограничения или индекса.
 */
public final class UniqueViolations {

    private static final String UNIQUE_VIOLATION = "23505";

    private UniqueViolations() {
    }

    public static boolean isUniqueViolation(Throwable ex, String... constraintNames) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (current instanceof SQLException sql && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
                String message = String.valueOf(sql.getMessage());
                for (String constraintName : constraintNames) {
                    if (message.contains(constraintName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
