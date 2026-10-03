package com.storefront.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class CustomerOrder {

    public enum Status { PENDING, CONFIRMED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_ref", nullable = false, unique = true)
    private String orderRef;

    @Column(name = "cart_id", nullable = false)
    private UUID cartId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "status_reason")
    private String statusReason;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(nullable = false)
    private String currency;

    @Column(name = "payment_ref")
    private String paymentRef;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected CustomerOrder() {
    }

    public CustomerOrder(String orderRef, UUID cartId, String currency) {
        this.orderRef = orderRef;
        this.cartId = cartId;
        this.currency = currency;
        this.status = Status.PENDING;
        this.total = BigDecimal.ZERO;
        this.createdAt = Instant.now();
    }

    public void addItem(Long productId, String productName, BigDecimal unitPrice, int quantity) {
        items.add(new OrderItem(this, productId, productName, unitPrice, quantity));
        total = total.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    public void confirm(String paymentRef) {
        this.status = Status.CONFIRMED;
        this.paymentRef = paymentRef;
        this.statusReason = null;
    }

    public void reject(String reason) {
        this.status = Status.REJECTED;
        this.statusReason = reason;
    }

    public Long getId() { return id; }
    public String getOrderRef() { return orderRef; }
    public UUID getCartId() { return cartId; }
    public Status getStatus() { return status; }
    public String getStatusReason() { return statusReason; }
    public BigDecimal getTotal() { return total; }
    public String getCurrency() { return currency; }
    public String getPaymentRef() { return paymentRef; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return items; }
}
