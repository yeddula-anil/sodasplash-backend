package com.sodasplash.order_service.dto;

import com.sodasplash.order_service.entity.DiscountType;
import com.sodasplash.order_service.entity.PaymentStatus;
import jakarta.validation.Valid;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderRequest {

    private PaymentStatus paymentStatus;

    private String internalNote;

    private DiscountType discountType;

    private BigDecimal discountValue;

    @Valid
    private List<UpdateOrderItemRequest> items;
}