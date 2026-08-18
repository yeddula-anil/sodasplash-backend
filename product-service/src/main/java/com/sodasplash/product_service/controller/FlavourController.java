package com.sodasplash.product_service.controller;

import com.sodasplash.product_service.dto.FlavourRequest;
import com.sodasplash.product_service.dto.FlavourResponse;
import com.sodasplash.product_service.service.FlavourService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/flavours")
@RequiredArgsConstructor
public class FlavourController {

    private final FlavourService flavourService;

    // =====================================================
    // ADD FLAVOUR
    // =====================================================

    @PostMapping
    public ResponseEntity<FlavourResponse> addFlavour(
            @PathVariable Long productId,
            @Valid @RequestBody FlavourRequest request
    ) {
        FlavourResponse response = flavourService.addFlavour(
                productId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =====================================================
    // GET ALL FLAVOURS OF PRODUCT
    // =====================================================

    @GetMapping
    public ResponseEntity<List<FlavourResponse>> getFlavours(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
                flavourService.getFlavoursByProduct(productId)
        );
    }

    // =====================================================
    // GET SINGLE FLAVOUR
    // =====================================================

    @GetMapping("/{flavourId}")
    public ResponseEntity<FlavourResponse> getFlavour(
            @PathVariable Long productId,
            @PathVariable Long flavourId
    ) {
        return ResponseEntity.ok(
                flavourService.getFlavour(
                        productId,
                        flavourId
                )
        );
    }

    // =====================================================
    // UPDATE FLAVOUR
    // =====================================================

    @PutMapping("/{flavourId}")
    public ResponseEntity<FlavourResponse> updateFlavour(
            @PathVariable Long productId,
            @PathVariable Long flavourId,
            @Valid @RequestBody FlavourRequest request
    ) {
        return ResponseEntity.ok(
                flavourService.updateFlavour(
                        productId,
                        flavourId,
                        request
                )
        );
    }

    // =====================================================
    // TOGGLE FLAVOUR ACTIVE STATUS
    // =====================================================

    @PatchMapping("/{flavourId}/toggle-status")
    public ResponseEntity<FlavourResponse> toggleFlavourStatus(
            @PathVariable Long productId,
            @PathVariable Long flavourId
    ) {
        return ResponseEntity.ok(
                flavourService.toggleFlavourStatus(
                        productId,
                        flavourId
                )
        );
    }

    // =====================================================
    // DELETE FLAVOUR
    // =====================================================

    @DeleteMapping("/{flavourId}")
    public ResponseEntity<Void> deleteFlavour(
            @PathVariable Long productId,
            @PathVariable Long flavourId
    ) {
        flavourService.deleteFlavour(
                productId,
                flavourId
        );

        return ResponseEntity.noContent().build();
    }
}