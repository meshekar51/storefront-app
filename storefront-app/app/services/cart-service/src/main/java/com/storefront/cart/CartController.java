package com.storefront.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The browser creates a random cart id (UUID) and keeps it in localStorage.
 * No login in this demo — the id itself is the cart's key.
 */
@RestController
@RequestMapping("/api/carts/{cartId}")
public class CartController {

    public record AddItemRequest(@NotNull Long productId, @Min(1) @Max(20) int quantity) {
    }

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @GetMapping
    public CartService.Cart get(@PathVariable UUID cartId) {
        return service.get(cartId);
    }

    @PostMapping("/items")
    public CartService.Cart add(@PathVariable UUID cartId, @Valid @RequestBody AddItemRequest request) {
        return service.addItem(cartId, request.productId(), request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartService.Cart remove(@PathVariable UUID cartId, @PathVariable Long productId) {
        return service.removeItem(cartId, productId);
    }

    /** Called by order-service after a confirmed order. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@PathVariable UUID cartId) {
        service.clear(cartId);
    }
}
