package com.storefront.order.clients;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** "Validate current product details" — the order uses today's price, not the cart snapshot. */
@Component
public class CatalogueClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Product(Long id, String name, BigDecimal price, String currency) {
    }

    public static class ProductUnavailableException extends RuntimeException {
        public ProductUnavailableException(Long productId) {
            super("Product " + productId + " is no longer available");
        }
    }

    private final RestClient http;

    public CatalogueClient(RestClient.Builder builder, @Value("${clients.catalogue.url}") String baseUrl) {
        this.http = builder.baseUrl(baseUrl).build();
    }

    public Product get(Long productId) {
        return http.get()
                .uri("/api/products/{id}", productId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ProductUnavailableException(productId);
                })
                .body(Product.class);
    }
}
