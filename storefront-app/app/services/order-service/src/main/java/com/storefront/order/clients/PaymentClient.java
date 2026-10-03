package com.storefront.order.clients;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** "Request payment" from payment-service (internal only). */
@Component
public class PaymentClient {

    record ChargeRequest(String orderRef, BigDecimal amount, String currency, String paymentToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(String orderRef, String status, String providerRef) {
        public boolean approved() {
            return "APPROVED".equals(status);
        }
    }

    private final RestClient http;

    public PaymentClient(RestClient.Builder builder, @Value("${clients.payment.url}") String baseUrl) {
        this.http = builder.baseUrl(baseUrl).build();
    }

    public Result charge(String orderRef, BigDecimal amount, String currency, String paymentToken) {
        return http.post()
                .uri("/api/payments")
                .body(new ChargeRequest(orderRef, amount, currency, paymentToken))
                .retrieve()
                .body(Result.class);
    }
}
