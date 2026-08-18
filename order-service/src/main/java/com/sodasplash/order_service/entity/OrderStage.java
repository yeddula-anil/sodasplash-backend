package com.sodasplash.order_service.entity;

public enum OrderStage {
    SUBMITTED,
    CONTACTED,
    NEGOTIATING,
    CONFIRMED,
    READY,
    SHIPPED,
    DELIVERED,
    CANCELLED
}