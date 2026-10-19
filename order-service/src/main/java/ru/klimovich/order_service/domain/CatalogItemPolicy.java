package ru.klimovich.order_service.domain;

import ru.klimovich.order_service.client.CatalogProduct;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Доступность позиции по данным каталога (design.md D3, spec shopping-cart "Unavailable cart items").
 * Причину недоступности (удален, заблокирован, некорректная цена) не различает.
 */
public final class CatalogItemPolicy {

    /** NUMERIC(12,2): не более 10 знаков целой части. */
    static final int MAX_INTEGER_DIGITS = 10;
    static final int PRICE_SCALE = 2;

    private CatalogItemPolicy() {
    }

    /**
     * @param product товар из ответа by-ids или null, если каталог его не вернул
     * @return доступный товар или пусто
     */
    public static Optional<AvailableProduct> evaluate(String productId, CatalogProduct product) {
        if (product == null
                || product.getStoreId() == null
                || product.getName() == null
                || product.getName().isBlank()
                || !isValidPrice(product.getPrice())) {
            return Optional.empty();
        }
        return Optional.of(new AvailableProduct(
                productId,
                product.getStoreId(),
                product.getName(),
                product.getArticleNumber(),
                product.getPrice().setScale(PRICE_SCALE, RoundingMode.UNNECESSARY)
        ));
    }

    static boolean isValidPrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            return false;
        }
        BigDecimal normalized = price.stripTrailingZeros();
        if (normalized.scale() > PRICE_SCALE) {
            return false;
        }
        int integerDigits = normalized.precision() - normalized.scale();
        return integerDigits <= MAX_INTEGER_DIGITS;
    }
}
