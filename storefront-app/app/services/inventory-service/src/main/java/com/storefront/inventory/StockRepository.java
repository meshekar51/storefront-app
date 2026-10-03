package com.storefront.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, Long> {

    /**
     * One atomic SQL UPDATE: decrements only if enough stock is left.
     * Returns 0 when stock is insufficient — no race between "check" and "reserve".
     */
    @Modifying
    @Query("update Stock s set s.available = s.available - :qty where s.productId = :productId and s.available >= :qty")
    int tryReserve(@Param("productId") Long productId, @Param("qty") int qty);

    @Modifying
    @Query("update Stock s set s.available = s.available + :qty where s.productId = :productId")
    int giveBack(@Param("productId") Long productId, @Param("qty") int qty);
}
