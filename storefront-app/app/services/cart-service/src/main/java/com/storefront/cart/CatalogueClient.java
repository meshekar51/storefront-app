package com.storefront.cart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/** Reads product name + current price from catalogue-service. */
@Component
public class CatalogueClient {

    /** Only the fields cart needs; anything else in the JSON is ignored. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Product(Long id, String name, BigDecimal price, String currency) {
    }

    private final RestClient http;

    public CatalogueClient(RestClient.Builder builder, @Value("${clients.catalogue.url}") String baseUrl) {
        this.http = builder.baseUrl(baseUrl).build();
    }

    public Product getProduct(Long productId) {
        return http.get()
                .uri("/api/products/{id}", productId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown product " + productId);
                })
                .body(Product.class);
    }
}
