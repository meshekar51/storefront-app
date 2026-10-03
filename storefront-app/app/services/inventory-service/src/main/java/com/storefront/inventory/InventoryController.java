package com.storefront.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    public record ReserveItem(@NotNull Long productId, @Min(1) int quantity) {
    }

    public record ReserveRequest(@NotBlank String orderRef, @NotEmpty List<@Valid ReserveItem> items) {
    }

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    /** Stock for every product — the shop page shows "N left". */
    @GetMapping
    public List<InventoryService.StockLevel> all() {
        return service.all();
    }

    @GetMapping("/{productId}")
    public InventoryService.StockLevel one(@PathVariable Long productId) {
        return service.one(productId);
    }

    /** Internal: called by order-service. 409 = not enough stock. */
    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public void reserve(@Valid @RequestBody ReserveRequest request) {
        service.reserve(request.orderRef(),
                request.items().stream().map(i -> new InventoryService.Item(i.productId(), i.quantity())).toList());
    }

    /** Internal: called by order-service when payment fails. */
    @PostMapping("/reservations/{orderRef}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable String orderRef) {
        service.release(orderRef);
    }
}
