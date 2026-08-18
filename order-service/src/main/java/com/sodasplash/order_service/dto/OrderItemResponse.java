package com.sodasplash.order_service.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private Long id;

    private Long productId;

    private Long flavourId;

    private String flavourName;

    private Integer quantity;

    private BigDecimal pricePerCase;

    private BigDecimal lineTotal;
}