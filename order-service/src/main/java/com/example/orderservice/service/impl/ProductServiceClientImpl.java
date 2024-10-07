package com.example.orderservice.service.impl;

import com.example.orderservice.dto.response.ApiResponse;
import com.example.orderservice.dto.response.ProductResponse;
import com.example.orderservice.service.ProductServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductServiceClientImpl {
    private final ProductServiceClient productServiceClient;

    public ApiResponse<ProductResponse> getProductById(Long id) {
        return productServiceClient.getProductById(id);
    }

    public ApiResponse<List<ProductResponse>> getProductsByIds(Set<Long> productIds) {
        return productServiceClient.getProductsByIds(productIds);
    }

    public void updateStockQuantity(Long id, Integer quantity){
        productServiceClient.updateStockQuantity(id, quantity);
    }
}
