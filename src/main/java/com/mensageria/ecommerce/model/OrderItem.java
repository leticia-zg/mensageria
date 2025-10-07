package com.mensageria.ecommerce.model;

public record OrderItem(
    String productId,
    int quantity
) {}
