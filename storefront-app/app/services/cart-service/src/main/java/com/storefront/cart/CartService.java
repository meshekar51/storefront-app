package com.storefront.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    public record Line(Long productId, String productName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
    }

    public record Cart(UUID cartId, List<Line> items, BigDecimal total) {
    }

    private final CartItemRepository repository;
    private final CatalogueClient catalogue;

    public CartService(CartItemRepository repository, CatalogueClient catalogue) {
        this.repository = repository;
        this.catalogue = catalogue;
    }

    @Transactional(readOnly = true)
    public Cart get(UUID cartId) {
        List<Line> lines = repository.findByCartIdOrderByIdAsc(cartId).stream()
                .map(i -> new Line(i.getProductId(), i.getProductName(), i.getUnitPrice(), i.getQuantity(),
                        i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();
        BigDecimal total = lines.stream().map(Line::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Cart(cartId, lines, total);
    }

    /** "Save products and quantities" — price snapshot comes from the catalogue. */
    @Transactional
    public Cart addItem(UUID cartId, Long productId, int quantity) {
        CatalogueClient.Product product = catalogue.getProduct(productId);
        repository.findByCartIdAndProductId(cartId, productId).ifPresentOrElse(
                existing -> {
                    existing.addQuantity(quantity);
                    existing.refreshPrice(product.name(), product.price());
                },
                () -> repository.save(new CartItem(cartId, productId, product.name(), product.price(), quantity)));
        return get(cartId);
    }

    @Transactional
    public Cart removeItem(UUID cartId, Long productId) {
        repository.deleteByCartIdAndProductId(cartId, productId);
        return get(cartId);
    }

    @Transactional
    public void clear(UUID cartId) {
        repository.deleteByCartId(cartId);
    }
}
