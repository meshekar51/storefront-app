package com.storefront.order.clients;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** "Check and reserve stock", and release it again if payment fails. */
@Component
public class InventoryClient {

    public record Item(Long productId, int quantity) {
    }

    record ReserveRequest(String orderRef, List<Item> items) {
    }

    public static class OutOfStockException extends RuntimeException {
        public OutOfStockException() {
            super("Some items are out of stock");
        }
    }

    private final RestClient http;

    public InventoryClient(RestClient.Builder builder, @Value("${clients.inventory.url}") String baseUrl) {
        this.http = builder.baseUrl(baseUrl).build();
    }

    public void reserve(String orderRef, List<Item> items) {
        http.post()
                .uri("/api/inventory/reservations")
                .body(new ReserveRequest(orderRef, items))
                .retrieve()
                .onStatus(status -> status.value() == 409, (req, res) -> {
                    throw new OutOfStockException();
                })
                .toBodilessEntity();
    }

    public void release(String orderRef) {
        http.post().uri("/api/inventory/reservations/{ref}/release", orderRef).retrieve().toBodilessEntity();
    }
}
