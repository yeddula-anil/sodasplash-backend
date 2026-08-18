package com.sodasplash.order_service.dto;

import com.sodasplash.order_service.entity.DiscountType;
import com.sodasplash.order_service.entity.OrderStage;
import com.sodasplash.order_service.entity.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;

    private String quoteNumber;

    private String customerName;

    private String email;

    private String phone;

    private String businessName;

    private String businessType;

    private String referralSource;

    private String referralName;

    private String referralEmail;

    private String address;

    private String pincode;

    private LocalDate deliveryDate;

    private String note;

    private String internalNote;

    private OrderStage status;

    private BigDecimal subtotal;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private BigDecimal discountAmount;

    private BigDecimal taxAmount;

    private BigDecimal additionalCharges;

    private BigDecimal total;

    private PaymentStatus paymentStatus;

    private Integer invoiceVersion;

    private String latestInvoiceNumber;

    private LocalDateTime finalizedAt;

    private List<OrderItemResponse> quoteItems;
}