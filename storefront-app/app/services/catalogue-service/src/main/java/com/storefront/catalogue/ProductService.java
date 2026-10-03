package com.storefront.catalogue;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Cache-aside: Spring checks Redis first ("Check cached product data");
 * only on a miss does the method body run and read PostgreSQL
 * ("On cache miss: read products"). The result is written back to Redis
 * with the TTL from spring.cache.redis.time-to-live.
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Cacheable("products")
    public List<ProductDto> listProducts() {
        log.info("Cache miss: loading product list from PostgreSQL");
        return repository.findByActiveTrueOrderByIdAsc().stream().map(ProductDto::from).toList();
    }

    @Cacheable(value = "product", key = "#id")
    public ProductDto getProduct(Long id) {
        log.info("Cache miss: loading product {} from PostgreSQL", id);
        return repository.findById(id)
                .filter(Product::isActive)
                .map(ProductDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + id + " not found"));
    }
}
