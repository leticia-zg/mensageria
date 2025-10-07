package com.mensageria.ecommerce.model;

import java.util.List;

public record OrderCreatedMessage(
    String orderId,
    String clientId,
    List<OrderItem> items
) {}
