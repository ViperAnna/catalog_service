package ru.klimovich.order_service.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Заказ в списке истории — без позиций.
 */
public record OrderSummaryResponse(UUID id,
                                   String status,
                                   BigDecimal totalAmount,
                                   int itemsCount,
                                   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                   LocalDateTime createdAt,
                                   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                   LocalDateTime updatedAt,
                                   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                   LocalDateTime cancelledAt) {
}
