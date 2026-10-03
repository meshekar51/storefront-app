package com.storefront.inventory;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryService {

    public record Item(Long productId, int quantity) {
    }

    public record StockLevel(Long productId, int available) {
    }

    private final StockRepository stock;
    private final ReservationRepository reservations;

    public InventoryService(StockRepository stock, ReservationRepository reservations) {
        this.stock = stock;
        this.reservations = reservations;
    }

    @Transactional(readOnly = true)
    public List<StockLevel> all() {
        return stock.findAll().stream().map(s -> new StockLevel(s.getProductId(), s.getAvailable())).toList();
    }

    @Transactional(readOnly = true)
    public StockLevel one(Long productId) {
        return stock.findById(productId)
                .map(s -> new StockLevel(s.getProductId(), s.getAvailable()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No stock record for " + productId));
    }

    /**
     * "Check and reserve stock". All lines succeed or none do: any shortfall
     * throws, the transaction rolls back, and earlier decrements are undone.
     * Calling twice with the same orderRef is a no-op (safe to retry).
     */
    @Transactional
    public void reserve(String orderRef, List<Item> items) {
        if (!reservations.findByOrderRef(orderRef).isEmpty()) {
            return;
        }
        for (Item item : items) {
            if (stock.tryReserve(item.productId(), item.quantity()) == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough stock for product " + item.productId());
            }
            reservations.save(new Reservation(orderRef, item.productId(), item.quantity()));
        }
    }

    /** Undo a reservation, e.g. when payment is declined. Idempotent. */
    @Transactional
    public void release(String orderRef) {
        for (Reservation r : reservations.findByOrderRef(orderRef)) {
            if (r.getStatus() == Reservation.Status.RESERVED) {
                stock.giveBack(r.getProductId(), r.getQuantity());
                r.release();
            }
        }
    }
}
