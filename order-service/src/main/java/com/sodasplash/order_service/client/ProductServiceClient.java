package com.sodasplash.order_service.client;


import com.sodasplash.order_service.dto.ProductItemResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "product-service",
        url = "${product-service.url}"
)
public interface ProductServiceClient {

    @GetMapping("/api/products/{productId}/items/{itemId}")
    ProductItemResponse getProductItem(
            @PathVariable String productId,
            @PathVariable String itemId
    );
}
