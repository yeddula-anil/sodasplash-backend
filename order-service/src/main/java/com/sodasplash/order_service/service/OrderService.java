package com.sodasplash.order_service.service;

import com.sodasplash.order_service.dto.ApiResponse;
import com.sodasplash.order_service.dto.CreateOrderRequest;
import com.sodasplash.order_service.dto.OrderResponse;
import com.sodasplash.order_service.dto.UpdateOrderRequest;
import com.sodasplash.order_service.entity.OrderStage;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    List<OrderResponse> getAllOrders();

    OrderResponse updateOrder(Long orderId, UpdateOrderRequest request);

    OrderResponse updateOrderStage(Long orderId, OrderStage newStage);

    ApiResponse<Void> cancelOrder(Long orderId);

    ApiResponse<Void> generateInvoice(Long orderId);

    List<OrderResponse> getOrdersByEmail(String email);

    List<OrderResponse> getOrdersByReferralEmail(String email);

    OrderResponse getOrderByIdOrQuoteNumber(String idOrQuoteNumber);
}