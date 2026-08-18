package com.sodasplash.product_service.service;

import com.sodasplash.product_service.dto.FlavourRequest;
import com.sodasplash.product_service.dto.FlavourResponse;
import com.sodasplash.product_service.entity.Flavour;
import com.sodasplash.product_service.entity.Product;
import com.sodasplash.product_service.repository.FlavourRepository;
import com.sodasplash.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FlavourService {

    private final FlavourRepository flavourRepository;
    private final ProductRepository productRepository;

    // =====================================================
    // ADD FLAVOUR
    // =====================================================

    @Transactional
    public FlavourResponse addFlavour(
            Long productId,
            FlavourRequest request
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found")
                );

        Flavour flavour = Flavour.builder()
                .name(request.getName().trim())
                .note(request.getNote())
                .color(request.getColor())
                .pricePerCase(request.getPricePerCase())
                .displayOrder(request.getDisplayOrder())
                .isActive(request.isActive())
                .emoji(request.getEmoji())
                .product(product)
                .build();

        Flavour savedFlavour = flavourRepository.save(flavour);

        return toResponse(savedFlavour);
    }

    // =====================================================
    // GET ALL FLAVOURS ACROSS ALL PRODUCTS
    // =====================================================

    @Transactional(readOnly = true)
    public List<FlavourResponse> getAllFlavours() {
        return flavourRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =====================================================
    // GET ALL FLAVOURS OF A PRODUCT
    // =====================================================


    @Transactional(readOnly = true)
    public List<FlavourResponse> getFlavoursByProduct(
            Long productId
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found")
                );

        return product.getFlavours()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =====================================================
    // GET FLAVOUR BY ID
    // =====================================================

    @Transactional(readOnly = true)
    public FlavourResponse getFlavour(
            Long productId,
            Long flavourId
    ) {
        Flavour flavour = findFlavour(productId, flavourId);

        return toResponse(flavour);
    }

    // =====================================================
    // UPDATE FLAVOUR
    // =====================================================

    @Transactional
    public FlavourResponse updateFlavour(
            Long productId,
            Long flavourId,
            FlavourRequest request
    ) {
        Flavour flavour = findFlavour(productId, flavourId);

        flavour.setName(request.getName().trim());
        flavour.setNote(request.getNote());
        flavour.setColor(request.getColor());
        flavour.setPricePerCase(request.getPricePerCase());
        flavour.setDisplayOrder(request.getDisplayOrder());
        flavour.setEmoji(request.getEmoji());

        Flavour updatedFlavour = flavourRepository.save(flavour);

        return toResponse(updatedFlavour);
    }

    // =====================================================
    // TOGGLE FLAVOUR ACTIVE STATUS
    // =====================================================

    @Transactional
    public FlavourResponse toggleFlavourStatus(
            Long productId,
            Long flavourId
    ) {
        Flavour flavour = findFlavour(productId, flavourId);

        flavour.setActive(!flavour.isActive());

        Flavour updatedFlavour = flavourRepository.save(flavour);

        return toResponse(updatedFlavour);
    }

    // =====================================================
    // DELETE FLAVOUR
    // =====================================================

    @Transactional
    public void deleteFlavour(
            Long productId,
            Long flavourId
    ) {
        Flavour flavour = findFlavour(productId, flavourId);

        flavourRepository.delete(flavour);
    }

    // =====================================================
    // FIND FLAVOUR
    // =====================================================

    private Flavour findFlavour(
            Long productId,
            Long flavourId
    ) {
        Flavour flavour = flavourRepository.findById(flavourId)
                .orElseThrow(() ->
                        new RuntimeException("Flavour not found")
                );

        if (!flavour.getProduct().getId().equals(productId)) {
            throw new RuntimeException(
                    "Flavour does not belong to this product"
            );
        }

        return flavour;
    }

    // =====================================================
    // ENTITY -> RESPONSE
    // =====================================================

    private FlavourResponse toResponse(Flavour flavour) {
        return FlavourResponse.builder()
                .id(flavour.getId())
                .productId(flavour.getProduct().getId())
                .name(flavour.getName())
                .note(flavour.getNote())
                .color(flavour.getColor())
                .pricePerCase(flavour.getPricePerCase())
                .displayOrder(flavour.getDisplayOrder())
                .isActive(flavour.isActive())
                .emoji(flavour.getEmoji())
                .build();
    }
}