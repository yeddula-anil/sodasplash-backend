package com.sodasplash.order_service.service;

import com.sodasplash.order_service.client.ProductServiceClient;
import com.sodasplash.order_service.config.DeliveryProperties;
import com.sodasplash.order_service.dto.*;
import com.sodasplash.order_service.entity.DiscountType;
import com.sodasplash.order_service.entity.Order;
import com.sodasplash.order_service.entity.OrderItem;
import com.sodasplash.order_service.entity.OrderStage;
import com.sodasplash.order_service.entity.PaymentStatus;
import com.sodasplash.order_service.exception.DeliveryDistanceException;
import com.sodasplash.order_service.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final GeocodingService geocodingService;
    private final DistanceService distanceService;
    private final ProductServiceClient productServiceClient;
    private final DeliveryProperties deliveryProperties;

    // =====================================================
    // CREATE ORDER
    // =====================================================

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        validateReferral(request);

        // 1. Geocode delivery address
        log.info("Geocoding delivery address");

        GeocodingResult geocoded = geocodingService.geocode(
                request.getDeliveryAddress(),
                request.getPincode()
        );

        if (geocoded == null) {
            throw new RuntimeException(
                    "Unable to locate delivery address"
            );
        }

        // 2. Calculate delivery distance
        double distanceKm = distanceService.calculateDistance(
                deliveryProperties.getLatitude(),
                deliveryProperties.getLongitude(),
                geocoded.latitude(),
                geocoded.longitude()
        );

        double maxDistanceKm =
                deliveryProperties.getMaxDistanceKm();

        log.info(
                "Delivery distance: {} km | Max allowed: {} km",
                distanceKm,
                maxDistanceKm
        );

        // 3. Check maximum delivery distance
        if (distanceKm > maxDistanceKm) {
            throw new DeliveryDistanceException(
                    String.format(
                            "Delivery address is too far. Your location is %.1f km away, " +
                                    "but we only deliver within %.0f km from our store.",
                            distanceKm,
                            maxDistanceKm
                    )
            );
        }

        // 4. Create order (items linked after subtotal is computed)
        Order order = Order.builder()
                .quoteNumber(generateQuoteNumber())
                .customerName(request.getContactName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .businessName(request.getBusinessName())
                .businessType(request.getBusinessType())
                .referralSource(request.getReferralSource())
                .referralName(request.getReferralName())
                .referralEmail(request.getReferralEmail())
                .address(request.getDeliveryAddress())
                .pincode(request.getPincode())
                .deliveryDate(request.getDeliveryDate())
                .note(request.getDeliveryNote())
                .status(OrderStage.SUBMITTED)
                .paymentStatus(PaymentStatus.PENDING)
                .discountType(null)
                .discountValue(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .additionalCharges(BigDecimal.ZERO)
                .invoiceVersion(1)
                .build();

        // 5. Fetch products/flavours and create order items
        List<OrderItem> orderItems = new ArrayList<>();

        BigDecimal subtotal = BigDecimal.ZERO;

        Map<CreateOrderItemRequest, ProductItemResponse> products = request.getItems()
                .stream()
                .collect(Collectors.toMap(
                        item -> item,
                        item -> CompletableFuture.supplyAsync(() ->
                                productServiceClient.getProductItem(
                                        item.getProductId().toString(),
                                        item.getFlavourId().toString()
                                )
                        )
                ))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().join()
                ));

        for (CreateOrderItemRequest itemRequest : request.getItems()) {

            log.info(
                    "Fetching product {} / flavour {}",
                    itemRequest.getProductId(),
                    itemRequest.getFlavourId()
            );

            ProductItemResponse product = products.get(itemRequest);

            if (product == null) {
                throw new RuntimeException(
                        "Product or flavour not found"
                );
            }

            if (product.price() == null) {
                throw new RuntimeException(
                        "Price not available for flavour "
                                + itemRequest.getFlavourId()
                );
            }

            BigDecimal pricePerCase = product.price();

            BigDecimal lineTotal = pricePerCase.multiply(
                    BigDecimal.valueOf(
                            itemRequest.getQuantity()
                    )
            );

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(Long.valueOf(product.productId()))
                    .flavourId(Long.valueOf(product.flavourId()))
                    .flavourName(product.flavourName())
                    .quantity(itemRequest.getQuantity())
                    .pricePerCase(pricePerCase)
                    .lineTotal(lineTotal)
                    .build();

            orderItems.add(orderItem);

            subtotal = subtotal.add(lineTotal);
        }

        // 6. Calculate totals
        order.setSubtotal(subtotal);

        BigDecimal discountAmount = calculateDiscount(
                subtotal,
                order.getDiscountType(),
                order.getDiscountValue()
        );

        order.setDiscountAmount(discountAmount);

        BigDecimal total = subtotal
                .subtract(discountAmount)
                .add(order.getTaxAmount())
                .add(order.getAdditionalCharges());

        order.setTotal(total);

        // 7. Link items with order
        order.getQuoteItems().addAll(orderItems);

        // 8. Save
        Order savedOrder = orderRepository.save(order);

        log.info(
                "Order {} created successfully",
                savedOrder.getId()
        );

        return toResponse(savedOrder);
    }

    // =====================================================
    // GET ALL ORDERS
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {

        return orderRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =====================================================
    // UPDATE ORDER
    // =====================================================

    @Override
    @Transactional
    public OrderResponse updateOrder(
            Long orderId,
            UpdateOrderRequest request
    ) {
        Order order = findOrderOrThrow(orderId);

        // Update payment status
        if (request.getPaymentStatus() != null) {
            order.setPaymentStatus(
                    request.getPaymentStatus()
            );
        }

        // Update internal note
        order.setInternalNote(
                request.getInternalNote()
        );

        // Update discount
        order.setDiscountType(
                request.getDiscountType()
        );

        BigDecimal discountValue =
                request.getDiscountValue() != null
                        ? request.getDiscountValue()
                        : BigDecimal.ZERO;

        order.setDiscountValue(discountValue);

        // Update order items
        if (request.getItems() != null) {

            for (UpdateOrderItemRequest itemRequest :
                    request.getItems()) {

                OrderItem orderItem = order.getQuoteItems()
                        .stream()
                        .filter(item ->
                                item.getId().equals(
                                        itemRequest.getId()
                                )
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Order item not found: "
                                                + itemRequest.getId()
                                )
                        );

                orderItem.setQuantity(
                        itemRequest.getQuantity()
                );

                orderItem.setPricePerCase(
                        itemRequest.getPricePerCase()
                );

                BigDecimal lineTotal =
                        itemRequest.getPricePerCase()
                                .multiply(
                                        BigDecimal.valueOf(
                                                itemRequest.getQuantity()
                                        )
                                );

                orderItem.setLineTotal(lineTotal);
            }
        }

        // Recalculate subtotal
        BigDecimal subtotal =
                order.getQuoteItems()
                        .stream()
                        .map(OrderItem::getLineTotal)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        order.setSubtotal(subtotal);

        // Recalculate discount
        BigDecimal discountAmount =
                calculateDiscount(
                        subtotal,
                        order.getDiscountType(),
                        order.getDiscountValue()
                );

        order.setDiscountAmount(discountAmount);

        // Recalculate total
        BigDecimal total =
                subtotal
                        .subtract(discountAmount)
                        .add(order.getTaxAmount())
                        .add(order.getAdditionalCharges());

        order.setTotal(total);

        Order updatedOrder =
                orderRepository.save(order);

        return toResponse(updatedOrder);
    }

    // =====================================================
    // UPDATE ORDER STAGE
    // =====================================================

    @Override
    @Transactional
    public OrderResponse updateOrderStage(
            Long orderId,
            OrderStage newStage
    ) {

        Order order = findOrderOrThrow(orderId);

        OrderStage currentStage =
                order.getStatus();

        if (currentStage == OrderStage.CANCELLED) {
            throw new RuntimeException(
                    "Cancelled order cannot be updated"
            );
        }

        if (currentStage == OrderStage.DELIVERED) {
            throw new RuntimeException(
                    "Delivered order cannot be updated"
            );
        }

        if (newStage == OrderStage.CANCELLED) {
            throw new RuntimeException(
                    "Use the cancel order endpoint"
            );
        }

        if (!isValidNextStage(currentStage, newStage)) {

            throw new RuntimeException(
                    "Invalid order stage transition: "
                            + currentStage
                            + " -> "
                            + newStage
            );
        }

        order.setStatus(newStage);

        // Invoice is generated ONLY when order moves to CONFIRMED
        if (newStage == OrderStage.CONFIRMED) {

            order.setInvoiceVersion(
                    order.getInvoiceVersion() + 1
            );

            order.setLatestInvoiceNumber(
                    generateInvoiceNumber(order)
            );

            order.setFinalizedAt(
                    LocalDateTime.now()
            );
        }

        Order updatedOrder =
                orderRepository.save(order);

        return toResponse(updatedOrder);
    }

    // =====================================================
    // CANCEL ORDER
    // =====================================================

    @Override
    @Transactional
    public ApiResponse<Void> cancelOrder(Long orderId) {

        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() == OrderStage.CANCELLED) {
            return new ApiResponse<>(false, "Order is already cancelled", null);
        }

        if (order.getStatus() == OrderStage.DELIVERED) {
            return new ApiResponse<>(false, "Delivered order cannot be cancelled", null);
        }

        order.setStatus(OrderStage.CANCELLED);
        orderRepository.save(order);

        return new ApiResponse<>(true, "Order cancelled successfully", null);
    }

    // =====================================================
    // GENERATE INVOICE
    // =====================================================

    @Override
    @Transactional
    public ApiResponse<Void> generateInvoice(Long orderId) {

        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() == OrderStage.CANCELLED) {
            return new ApiResponse<>(false, "Invoice cannot be generated for a cancelled order", null);
        }

        if (order.getStatus() == OrderStage.SUBMITTED
                || order.getStatus() == OrderStage.CONTACTED
                || order.getStatus() == OrderStage.NEGOTIATING) {
            return new ApiResponse<>(false, "Invoice can only be generated after order confirmation", null);
        }

        if (order.getLatestInvoiceNumber() == null) {
            order.setLatestInvoiceNumber(generateInvoiceNumber(order));
            orderRepository.save(order);
        }

        return new ApiResponse<>(true, "Invoice number: " + order.getLatestInvoiceNumber(), null);
    }

    // =====================================================
    // GET ORDERS BY EMAIL
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByEmail(String email) {

        return orderRepository.findByEmail(email)
                .stream()
                .map(this::toResponse)
                .toList();
    }
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByReferralEmail(String email) {

        return orderRepository.findByReferralEmail(email)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdOrQuoteNumber(String idOrQuoteNumber) {
        Order order = null;
        try {
            Long id = Long.parseLong(idOrQuoteNumber);
            order = orderRepository.findById(id).orElse(null);
        } catch (NumberFormatException ignored) {
        }

        if (order == null) {
            order = orderRepository.findByQuoteNumber(idOrQuoteNumber)
                    .orElseThrow(() -> new EntityNotFoundException("Order not found: " + idOrQuoteNumber));
        }

        return toResponse(order);
    }

    // =====================================================
    // VALIDATE ORDER STAGE
    // =====================================================

    private boolean isValidNextStage(
            OrderStage current,
            OrderStage next
    ) {

        return switch (current) {

            case SUBMITTED ->
                    next == OrderStage.CONTACTED;

            case CONTACTED ->
                    next == OrderStage.NEGOTIATING;

            case NEGOTIATING ->
                    next == OrderStage.CONFIRMED;

            case CONFIRMED ->
                    next == OrderStage.READY;

            case READY ->
                    next == OrderStage.SHIPPED;

            case SHIPPED ->
                    next == OrderStage.DELIVERED;

            case DELIVERED,
                 CANCELLED ->
                    false;
        };
    }

    // =====================================================
    // VALIDATE REFERRAL
    // =====================================================

    private void validateReferral(
            CreateOrderRequest request
    ) {
        String source = request.getReferralSource();
        if ("SALESPERSON".equalsIgnoreCase(source) || "BD".equalsIgnoreCase(source)) {

            if (request.getReferralName() == null
                    || request.getReferralName().isBlank()) {

                throw new IllegalArgumentException(
                        "Referral name is required when referral source is salesperson"
                );
            }

            if (request.getReferralEmail() == null
                    || request.getReferralEmail().isBlank()) {

                request.setReferralEmail(request.getReferralName().toLowerCase().replaceAll("\\s+", "") + "@sodasplash.com");
            }

        } else {

            request.setReferralName(null);
            request.setReferralEmail(null);
        }
    }


    // =====================================================
    // CALCULATE DISCOUNT
    // =====================================================

    private BigDecimal calculateDiscount(
            BigDecimal subtotal,
            DiscountType discountType,
            BigDecimal discountValue
    ) {

        if (discountType == null
                || discountValue == null
                || discountValue.compareTo(BigDecimal.ZERO) <= 0) {

            return BigDecimal.ZERO;
        }

        if (discountType == DiscountType.FLAT) {

            return discountValue.min(subtotal);
        }

        if (discountType == DiscountType.PERCENTAGE) {

            return subtotal
                    .multiply(discountValue)
                    .divide(
                            BigDecimal.valueOf(100),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        return BigDecimal.ZERO;
    }

    // =====================================================
    // FIND ORDER
    // =====================================================

    private Order findOrderOrThrow(Long orderId) {

        return orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Order not found with id: " + orderId
                        )
                );
    }

    // =====================================================
    // GENERATE QUOTE NUMBER
    // =====================================================

    private String generateQuoteNumber() {

        int year = Year.now().getValue();

        long number =
                ThreadLocalRandom.current()
                        .nextLong(100000, 1000000);

        return "Q-" + year + "-" + number;
    }

    // =====================================================
    // GENERATE INVOICE NUMBER
    // =====================================================

    private String generateInvoiceNumber(Order order) {

        int year = Year.now().getValue();

        return String.format(
                "INV-%d-%06d-V%d",
                year,
                order.getId(),
                order.getInvoiceVersion()
        );
    }

    // =====================================================
    // ENTITY -> RESPONSE
    // =====================================================

    private OrderResponse toResponse(Order order) {

        List<OrderItemResponse> itemResponses =
                order.getQuoteItems()
                        .stream()
                        .map(item ->
                                OrderItemResponse.builder()
                                        .id(item.getId())
                                        .productId(item.getProductId())
                                        .flavourId(item.getFlavourId())
                                        .flavourName(item.getFlavourName())
                                        .quantity(item.getQuantity())
                                        .pricePerCase(item.getPricePerCase())
                                        .lineTotal(item.getLineTotal())
                                        .build()
                        )
                        .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .quoteNumber(order.getQuoteNumber())
                .customerName(order.getCustomerName())
                .email(order.getEmail())
                .phone(order.getPhone())
                .businessName(order.getBusinessName())
                .businessType(order.getBusinessType())
                .referralSource(order.getReferralSource())
                .referralName(order.getReferralName())
                .referralEmail(order.getReferralEmail())
                .address(order.getAddress())
                .pincode(order.getPincode())
                .deliveryDate(order.getDeliveryDate())
                .note(order.getNote())
                .internalNote(order.getInternalNote())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .discountType(order.getDiscountType())
                .discountValue(order.getDiscountValue())
                .discountAmount(order.getDiscountAmount())
                .taxAmount(order.getTaxAmount())
                .additionalCharges(order.getAdditionalCharges())
                .total(order.getTotal())
                .paymentStatus(order.getPaymentStatus())
                .invoiceVersion(order.getInvoiceVersion())
                .latestInvoiceNumber(order.getLatestInvoiceNumber())
                .finalizedAt(order.getFinalizedAt())
                .quoteItems(itemResponses)
                .build();
    }
}
