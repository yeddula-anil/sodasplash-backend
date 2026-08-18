package com.sodasplash.product_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            unique = true,
            length = 100
    )
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

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

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<Flavour> flavours = new ArrayList<>();
}