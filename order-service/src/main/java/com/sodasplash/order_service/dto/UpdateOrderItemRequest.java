package com.sodasplash.order_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderItemRequest {

    @NotNull(message = "Order item ID is required")
    private Long id;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Price per case is required")
    @Min(value = 0, message = "Price cannot be negative")
    private BigDecimal pricePerCase;
}
