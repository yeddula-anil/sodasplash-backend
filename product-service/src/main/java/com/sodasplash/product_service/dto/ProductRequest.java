package com.sodasplash.product_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    private String imageUrl;
    private Integer displayOrder;

    @Builder.Default
    private boolean isActive = true;
}