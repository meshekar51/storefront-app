package com.storefront.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    /** paymentToken: "tok_visa" (approve) or "tok_decline" (decline) in this demo. */
    public record CheckoutRequest(@NotNull UUID cartId, @NotBlank String paymentToken) {
    }

    private final CheckoutService checkout;

    public OrderController(CheckoutService checkout) {
        this.checkout = checkout;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CheckoutService.OrderView create(@Valid @RequestBody CheckoutRequest request) {
        return checkout.checkout(request.cartId(), request.paymentToken());
    }

    @GetMapping("/{orderRef}")
    public CheckoutService.OrderView get(@PathVariable String orderRef) {
        return checkout.find(orderRef);
    }
}
