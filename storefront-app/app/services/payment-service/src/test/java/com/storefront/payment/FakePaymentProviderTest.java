package com.storefront.payment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FakePaymentProviderTest {

    private final FakePaymentProvider provider = new FakePaymentProvider();

    @Test
    void approvesTheVisaTestToken() {
        var result = provider.charge("ORD-1", new BigDecimal("10.00"), "EUR", "tok_visa");
        assertTrue(result.approved());
        assertTrue(result.providerRef().startsWith("pay_"));
    }

    @Test
    void declinesTheDeclineTestToken() {
        assertFalse(provider.charge("ORD-2", BigDecimal.ONE, "EUR", "tok_decline").approved());
    }
}
