package com.sodasplash.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "quote_number",
            nullable = false,
            unique = true,
            length = 50
    )
    private String quoteNumber;

    @Column(
            name = "customer_name",
            nullable = false,
            length = 100
    )
    private String customerName;

    @Column(
            nullable = false,
            length = 150
    )
    private String email;

    @Column(
            nullable = false,
            length = 20
    )
    private String phone;

    @Column(
            name = "business_name",
            length = 150
    )
    private String businessName;

    @Column(
            name = "business_type",
            nullable = false,
            length = 100
    )
    private String businessType;

    @Column(
            name = "referral_source",
            nullable = false,
            length = 100
    )
    private String referralSource;

    @Column(
            name = "referral_name",
            length = 100
    )
    private String referralName;

    @Column(
            name = "referral_email",
            length = 150
    )
    private String referralEmail;

    @Column(
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String address;

    @Column(
            nullable = false,
            length = 10
    )
    private String pincode;

    @Column(
            name = "delivery_date",
            nullable = false
    )
    private LocalDate deliveryDate;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(
            name = "internal_note",
            columnDefinition = "TEXT"
    )
    private String internalNote;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    @Builder.Default
    private OrderStage status = OrderStage.SUBMITTED;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal subtotal;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "discount_type",
            length = 20
    )
    private DiscountType discountType;

    @Column(
            name = "discount_value",
            nullable = false,
            precision = 12,
            scale = 2
    )
    @Builder.Default
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Column(
            name = "discount_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(
            name = "tax_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(
            name = "additional_charges",
            nullable = false,
            precision = 12,
            scale = 2
    )
    @Builder.Default
    private BigDecimal additionalCharges = BigDecimal.ZERO;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_status",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(
            name = "invoice_version",
            nullable = false
    )
    @Builder.Default
    private Integer invoiceVersion = 1;

    @Column(
            name = "latest_invoice_number",
            length = 50
    )
    private String latestInvoiceNumber;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<OrderItem> quoteItems = new ArrayList<>();
}