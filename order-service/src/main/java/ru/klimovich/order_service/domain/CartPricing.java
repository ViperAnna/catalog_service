package ru.klimovich.order_service.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Серверный расчет стоимости: lineTotal = unitPrice * quantity, total = сумма lineTotal.
 * Цены приходят только из каталога (CatalogItemPolicy), значения от клиента не участвуют.
 */
public final class CartPricing {

    private static final int MONEY_SCALE = 2;

    private CartPricing() {
    }

    public static BigDecimal lineTotal(BigDecimal unitPrice, int quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal total(Collection<BigDecimal> lineTotals) {
        return lineTotals.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }
}
