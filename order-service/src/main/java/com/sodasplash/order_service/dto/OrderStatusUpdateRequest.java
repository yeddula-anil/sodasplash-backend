package com.sodasplash.order_service.dto;


import com.sodasplash.order_service.entity.OrderStage;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(

        @NotNull(message = "New status is required")
        OrderStage newStatus

) {
}
