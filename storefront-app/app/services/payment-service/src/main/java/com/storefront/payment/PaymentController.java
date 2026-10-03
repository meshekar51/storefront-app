package com.storefront.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal only — no HTTPRoute exposes this; only order-service may call it (NetworkPolicy). */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    public record ChargeRequest(
            @NotBlank String orderRef,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String currency,
            @NotBlank String paymentToken) {
    }

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    public PaymentService.PaymentResult charge(@Valid @RequestBody ChargeRequest request) {
        return service.charge(request.orderRef(), request.amount(), request.currency(), request.paymentToken());
    }
}
