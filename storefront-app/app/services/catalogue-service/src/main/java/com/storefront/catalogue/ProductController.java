package com.storefront.catalogue;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    /** Browse products (public, via the gateway). */
    @GetMapping
    public List<ProductDto> list() {
        return service.listProducts();
    }

    /** Product details — also used by cart and order to validate prices. */
    @GetMapping("/{id}")
    public ProductDto get(@PathVariable Long id) {
        return service.getProduct(id);
    }
}
