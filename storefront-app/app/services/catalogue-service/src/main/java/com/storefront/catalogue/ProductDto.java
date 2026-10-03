package com.storefront.catalogue;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * What the API returns and what Redis stores. Serializable because the
 * Redis cache uses JDK serialization by default — no extra config needed.
 */
public record ProductDto(
        Long id,
        String sku,
        String name,
        String description,
        String category,
        BigDecimal price,
        String currency) implements Serializable {

    static ProductDto from(Product p) {
        return new ProductDto(p.getId(), p.getSku(), p.getName(), p.getDescription(),
                p.getCategory(), p.getPrice(), p.getCurrency());
    }
}
