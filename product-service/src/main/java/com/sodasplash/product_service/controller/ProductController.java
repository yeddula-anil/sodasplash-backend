package com.sodasplash.product_service.controller;

import com.sodasplash.product_service.dto.ProductItemResponse;
import com.sodasplash.product_service.dto.ProductRequest;
import com.sodasplash.product_service.dto.ProductResponse;
import com.sodasplash.product_service.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // =====================================================
    // CREATE PRODUCT
    // =====================================================

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request
    ) {
        ProductResponse response = productService.createProduct(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =====================================================
    // GET ALL PRODUCTS
    // =====================================================

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(
                productService.getAllProducts()
        );
    }

    // =====================================================
    // UPDATE PRODUCT
    // =====================================================

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(
                productService.updateProduct(
                        productId,
                        request
                )
        );
    }

    // =====================================================
    // TOGGLE PRODUCT ACTIVE STATUS
    // =====================================================

    @PatchMapping("/{productId}/toggle-status")
    public ResponseEntity<ProductResponse> toggleProductStatus(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
                productService.toggleProductStatus(productId)
        );
    }

    // =====================================================
    // DELETE PRODUCT
    // =====================================================

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long productId
    ) {
        productService.deleteProduct(productId);

        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{productId}/items/{itemId}")
    public ResponseEntity<ProductItemResponse> getProductItem(
            @PathVariable Long productId,
            @PathVariable Long itemId
    ) {
        return ResponseEntity.ok(
                productService.getProductItem(
                        productId,
                        itemId
                )
        );
    }
}