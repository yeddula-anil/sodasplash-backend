package com.sodasplash.product_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlavourRequest {

    @NotBlank(message = "Flavour name is required")
    private String name;

    private String note;

    private String color;

    @NotNull(message = "Price per case is required")
    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "Price per case must be greater than 0"
    )
    private BigDecimal pricePerCase;

    @NotNull(message = "Display order is required")
    private Integer displayOrder;

    @Builder.Default
    private boolean isActive = true;

    private String emoji;
}