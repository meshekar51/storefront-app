package com.storefront.order.clients;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** "Read selected items" from cart-service, and empty the cart after a confirmed order. */
@Component
public class CartClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Line(Long productId, String productName, int quantity) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cart(UUID cartId, List<Line> items, BigDecimal total) {
    }

    private final RestClient http;

    public CartClient(RestClient.Builder builder, @Value("${clients.cart.url}") String baseUrl) {
        this.http = builder.baseUrl(baseUrl).build();
    }

    public Cart get(UUID cartId) {
        return http.get().uri("/api/carts/{id}", cartId).retrieve().body(Cart.class);
    }

    public void clear(UUID cartId) {
        http.delete().uri("/api/carts/{id}", cartId).retrieve().toBodilessEntity();
    }
}
