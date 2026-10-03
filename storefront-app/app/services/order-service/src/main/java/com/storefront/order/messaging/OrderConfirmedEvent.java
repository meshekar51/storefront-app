package com.storefront.order.messaging;

import java.math.BigDecimal;
import java.time.Instant;

/** Payload published to Amazon MQ after a confirmed order. */
public record OrderConfirmedEvent(
        String orderRef,
        BigDecimal total,
        String currency,
        int itemCount,
        Instant confirmedAt) {
}
