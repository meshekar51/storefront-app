package com.storefront.payment;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Stand-in for the "External payment provider" (Stripe, Adyen, …).
 * Test tokens, like real providers use:
 *   tok_visa    → approved
 *   tok_decline → declined
 * Replace this class with a real SDK call; nothing else changes.
 */
@Component
public class FakePaymentProvider {

    public record Result(boolean approved, String providerRef) {
    }

    public Result charge(String orderRef, BigDecimal amount, String currency, String paymentToken) {
        boolean approved = !"tok_decline".equals(paymentToken);
        return new Result(approved, "pay_" + UUID.randomUUID().toString().substring(0, 12));
    }
}
