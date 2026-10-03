package com.storefront.order;

import com.storefront.order.clients.CartClient;
import com.storefront.order.clients.CatalogueClient;
import com.storefront.order.clients.InventoryClient;
import com.storefront.order.clients.PaymentClient;
import com.storefront.order.messaging.OrderConfirmedEvent;
import com.storefront.order.messaging.OrderEventPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * The checkout saga, step by step (same order as the architecture diagram):
 *
 *   1. read selected items          → cart-service
 *   2. validate current prices      → catalogue-service
 *   3. save order (PENDING)         → orders table
 *   4. check and reserve stock      → inventory-service
 *   5. request payment              → payment-service
 *        declined → release stock, order REJECTED
 *   6. order CONFIRMED, clear cart
 *   7. publish "order.confirmed"    → Amazon MQ → notification-service
 *
 * Each step that changes another service has an undo (release stock), so a
 * failure part-way never leaves stock locked.
 */
@Service
public class CheckoutService {

    public record LineView(Long productId, String productName, BigDecimal unitPrice, int quantity) {
    }

    public record OrderView(String orderRef, String status, String statusReason, BigDecimal total,
                            String currency, String paymentRef, Instant createdAt, List<LineView> items) {
        static OrderView from(CustomerOrder o) {
            return new OrderView(o.getOrderRef(), o.getStatus().name(), o.getStatusReason(), o.getTotal(),
                    o.getCurrency(), o.getPaymentRef(), o.getCreatedAt(),
                    o.getItems().stream()
                            .map(i -> new LineView(i.getProductId(), i.getProductName(), i.getUnitPrice(), i.getQuantity()))
                            .toList());
        }
    }

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    private static final String CURRENCY = "EUR";

    private final OrderRepository orders;
    private final CartClient cart;
    private final CatalogueClient catalogue;
    private final InventoryClient inventory;
    private final PaymentClient payment;
    private final OrderEventPublisher events;

    public CheckoutService(OrderRepository orders, CartClient cart, CatalogueClient catalogue,
                           InventoryClient inventory, PaymentClient payment, OrderEventPublisher events) {
        this.orders = orders;
        this.cart = cart;
        this.catalogue = catalogue;
        this.inventory = inventory;
        this.payment = payment;
        this.events = events;
    }

    public OrderView checkout(UUID cartId, String paymentToken) {
        // 1. read selected items
        CartClient.Cart current = cart.get(cartId);
        if (current.items() == null || current.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        // 2 + 3. validate prices against the catalogue and save the order as PENDING
        CustomerOrder order = new CustomerOrder(newOrderRef(), cartId, CURRENCY);
        for (CartClient.Line line : current.items()) {
            CatalogueClient.Product product;
            try {
                product = catalogue.get(line.productId());
            } catch (CatalogueClient.ProductUnavailableException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
            order.addItem(product.id(), product.name(), product.price(), line.quantity());
        }
        order = orders.save(order);
        log.info("Order {} created for cart {}, total {} {}", order.getOrderRef(), cartId, order.getTotal(), CURRENCY);

        // 4. check and reserve stock
        List<InventoryClient.Item> items = order.getItems().stream()
                .map(i -> new InventoryClient.Item(i.getProductId(), i.getQuantity()))
                .toList();
        try {
            inventory.reserve(order.getOrderRef(), items);
        } catch (InventoryClient.OutOfStockException e) {
            order.reject("Out of stock");
            return OrderView.from(orders.save(order));
        } catch (RuntimeException e) {
            order.reject("Inventory service unavailable");
            orders.save(order);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not reserve stock, try again", e);
        }

        // 5. request payment
        PaymentClient.Result result;
        try {
            result = payment.charge(order.getOrderRef(), order.getTotal(), CURRENCY, paymentToken);
        } catch (RuntimeException e) {
            inventory.release(order.getOrderRef());
            order.reject("Payment service unavailable");
            orders.save(order);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payment failed, nothing was charged", e);
        }
        if (!result.approved()) {
            inventory.release(order.getOrderRef());
            order.reject("Payment declined");
            return OrderView.from(orders.save(order));
        }

        // 6. confirm and empty the cart
        order.confirm(result.providerRef());
        order = orders.save(order);
        try {
            cart.clear(cartId);
        } catch (RuntimeException e) {
            log.warn("Order {} confirmed but cart {} not cleared", order.getOrderRef(), cartId, e);
        }

        // 7. "After confirmed payment and stock reservation" → Amazon MQ
        events.orderConfirmed(new OrderConfirmedEvent(order.getOrderRef(), order.getTotal(), CURRENCY,
                order.getItems().size(), Instant.now()));
        return OrderView.from(order);
    }

    @Transactional(readOnly = true)
    public OrderView find(String orderRef) {
        return orders.findByOrderRef(orderRef)
                .map(OrderView::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order " + orderRef + " not found"));
    }

    private static String newOrderRef() {
        return "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
    }
}
