package com.sodasplash.product_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "flavours")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flavour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            length = 100
    )
    private String name;

    @Column(length = 500)
    private String note;

    @Column(
            name = "price_per_case",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal pricePerCase;

    @Column(length = 50)
    private String color;

    @Column(
            name = "display_order",
            nullable = false
    )
    private Integer displayOrder;

    @Builder.Default
    @Column(
            name = "is_active",
            nullable = false
    )
    private boolean isActive = true;

    @Column(length = 20)
    private String emoji;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Product product;
}