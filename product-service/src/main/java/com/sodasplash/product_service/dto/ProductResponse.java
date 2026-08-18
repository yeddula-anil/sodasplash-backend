package com.sodasplash.product_service.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;

    private String name;

    private String description;

    private String imageUrl;

    private boolean isActive;

    private Integer displayOrder;

    private List<FlavourResponse> flavours;
}