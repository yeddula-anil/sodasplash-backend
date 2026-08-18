package com.sodasplash.product_service.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlavourResponse {

    private Long id;

    private Long productId;

    private String name;

    private String note;

    private BigDecimal pricePerCase;

    private String color;

    private Integer displayOrder;

    private boolean isActive;

    private String emoji;
}