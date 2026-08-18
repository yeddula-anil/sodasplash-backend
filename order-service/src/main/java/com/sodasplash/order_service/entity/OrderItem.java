package com.sodasplash.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "flavour_id", nullable = false)
    private Long flavourId;

    @Column(name = "flavour_name", nullable = false, length = 150)
    private String flavourName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "price_per_case", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerCase;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;
}