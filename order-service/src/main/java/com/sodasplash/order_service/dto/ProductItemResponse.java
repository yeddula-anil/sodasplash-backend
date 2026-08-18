package com.sodasplash.order_service.dto;


import java.math.BigDecimal;

public record ProductItemResponse(

        String productId,

        String productName,

        String flavourId,

        String flavourName,

        BigDecimal price

) {
}
