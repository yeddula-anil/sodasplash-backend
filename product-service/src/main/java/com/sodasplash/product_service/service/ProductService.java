package com.sodasplash.product_service.service;

import com.sodasplash.product_service.dto.FlavourResponse;
import com.sodasplash.product_service.dto.ProductItemResponse;
import com.sodasplash.product_service.dto.ProductRequest;
import com.sodasplash.product_service.dto.ProductResponse;
import com.sodasplash.product_service.entity.Flavour;
import com.sodasplash.product_service.entity.Product;
import com.sodasplash.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    // =====================================================
    // CREATE PRODUCT
    // =====================================================

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        String name = request.getName().trim();

        if (productRepository.existsByNameIgnoreCase(name)) {
            throw new RuntimeException("Product already exists");
        }

        Product product = Product.builder()
                .name(name)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .displayOrder(request.getDisplayOrder())
                .isActive(request.isActive())
                .build();

        Product savedProduct = productRepository.save(product);

        return toProductResponse(savedProduct);
    }

    // =====================================================
    // GET ALL PRODUCTS
    // =====================================================

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {

        return productRepository.findAll()
                .stream()
                .map(this::toProductResponse)
                .toList();
    }

    // =====================================================
    // UPDATE PRODUCT
    // =====================================================

    @Transactional
    public ProductResponse updateProduct(
            Long id,
            ProductRequest request
    ) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product not found")
                );

        String name = request.getName().trim();

        if (!product.getName().equalsIgnoreCase(name)
                && productRepository.existsByNameIgnoreCase(name)) {

            throw new RuntimeException("Product already exists");
        }

        product.setName(name);
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setDisplayOrder(request.getDisplayOrder());

        Product updatedProduct = productRepository.save(product);

        return toProductResponse(updatedProduct);
    }

    // =====================================================
    // TOGGLE PRODUCT ACTIVE STATUS
    // =====================================================

    @Transactional
    public ProductResponse toggleProductStatus(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product not found")
                );

        product.setActive(!product.isActive());

        Product updatedProduct = productRepository.save(product);

        return toProductResponse(updatedProduct);
    }

    // =====================================================
    // DELETE PRODUCT
    // =====================================================

    @Transactional
    public void deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product not found")
                );

        productRepository.delete(product);
    }

    // =====================================================
    // ENTITY -> RESPONSE
    // =====================================================

    private ProductResponse toProductResponse(Product product) {

        List<FlavourResponse> flavours = product.getFlavours()
                .stream()
                .map(flavour -> FlavourResponse.builder()
                        .id(flavour.getId())
                        .productId(product.getId())
                        .name(flavour.getName())
                        .note(flavour.getNote())
                        .color(flavour.getColor())
                        .pricePerCase(flavour.getPricePerCase())
                        .displayOrder(flavour.getDisplayOrder())
                        .isActive(flavour.isActive())
                        .emoji(flavour.getEmoji())
                        .build())
                .toList();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .isActive(product.isActive())
                .flavours(flavours)
                .build();
    }


    @Transactional(readOnly = true)
    public ProductItemResponse getProductItem(
            Long productId,
            Long flavourId
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found")
                );

        Flavour flavour = product.getFlavours()
                .stream()
                .filter(item -> item.getId().equals(flavourId))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException(
                                "Flavour not found for this product"
                        )
                );

        if (!product.isActive()) {
            throw new RuntimeException(
                    "Product is inactive"
            );
        }

        if (!flavour.isActive()) {
            throw new RuntimeException(
                    "Flavour is inactive"
            );
        }

        return new ProductItemResponse(
                product.getId().toString(),
                product.getName(),
                flavour.getId().toString(),
                flavour.getName(),
                flavour.getPricePerCase()
        );
    }
}