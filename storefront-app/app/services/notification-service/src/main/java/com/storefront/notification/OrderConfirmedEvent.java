package com.storefront.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;

/** Same JSON shape order-service publishes; unknown fields are ignored so the publisher can add fields safely. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderConfirmedEvent(
        String orderRef,
        BigDecimal total,
        String currency,
        int itemCount,
        Instant confirmedAt) {
}
