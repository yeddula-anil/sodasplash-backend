package com.sodasplash.order_service.controller;

import com.sodasplash.order_service.dto.*;
import com.sodasplash.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing all order-service endpoints.
 *
 * Base path: /api/orders
 *
 *   POST   /api/orders                        — create a new order
 *   GET    /api/orders                        — get all orders
 *   PUT    /api/orders/{orderId}              — update order (pricing / discount / items)
 *   PATCH  /api/orders/{orderId}/stage        — update order stage
 *   PATCH  /api/orders/{orderId}/cancel       — cancel order
 *   POST   /api/orders/{orderId}/invoice      — generate invoice
 *   GET    /api/orders/by-email               — get all orders for a given email
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE ORDER
    // POST /api/orders
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order placed successfully", response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET ALL ORDERS
    // GET /api/orders
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(
                ApiResponse.ok("All orders retrieved successfully", orders)
        );
    }

    @GetMapping("/{idOrQuoteNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable String idOrQuoteNumber) {
        OrderResponse response = orderService.getOrderByIdOrQuoteNumber(idOrQuoteNumber);
        return ResponseEntity.ok(
                ApiResponse.ok("Order retrieved successfully", response)
        );
    }


    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE ORDER  (pricing, discount, internal note, item quantities/prices)
    // PUT /api/orders/{orderId}
    // ─────────────────────────────────────────────────────────────────────────

    @PutMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderRequest request
    ) {
        OrderResponse response = orderService.updateOrder(orderId, request);
        return ResponseEntity.ok(
                ApiResponse.ok("Order updated successfully", response)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE ORDER STAGE
    // PATCH /api/orders/{orderId}/stage
    // Body: { "newStatus": "CONTACTED" }
    // ─────────────────────────────────────────────────────────────────────────

    @PatchMapping("/{orderId}/stage")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStage(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        OrderResponse response = orderService.updateOrderStage(orderId, request.newStatus());
        return ResponseEntity.ok(
                ApiResponse.ok("Order stage updated to " + request.newStatus(), response)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL ORDER
    // PATCH /api/orders/{orderId}/cancel
    // ─────────────────────────────────────────────────────────────────────────

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable Long orderId
    ) {
        ApiResponse<Void> response = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GENERATE INVOICE
    // POST /api/orders/{orderId}/invoice
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{orderId}/invoice")
    public ResponseEntity<ApiResponse<Void>> generateInvoice(
            @PathVariable Long orderId
    ) {
        ApiResponse<Void> response = orderService.generateInvoice(orderId);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET ALL ORDERS BY EMAIL
    // GET /api/orders/by-email?email=user@example.com
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/by-email")
    public ResponseEntity<List<OrderResponse>> getOrdersByEmail(
            @RequestParam String email
    ) {
        List<OrderResponse> orders = orderService.getOrdersByEmail(email);
        return ResponseEntity.ok(
                orders
        );
    }

    @GetMapping("/by-referral-email")
    public ResponseEntity<List<OrderResponse>> getOrdersByReferralEmail(
            @RequestParam String email
    ) {
        List<OrderResponse> orders = orderService.getOrdersByEmail(email);
        return ResponseEntity.ok(
                orders
        );
    }
}
