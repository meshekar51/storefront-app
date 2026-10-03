package com.storefront.cart;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartIdOrderByIdAsc(UUID cartId);

    Optional<CartItem> findByCartIdAndProductId(UUID cartId, Long productId);

    long deleteByCartIdAndProductId(UUID cartId, Long productId);

    long deleteByCartId(UUID cartId);
}
