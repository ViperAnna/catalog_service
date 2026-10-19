package ru.klimovich.order_service.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Стабильный формат страницы вместо сериализации PageImpl.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
