package com.mensageria.ecommerce.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mensageria.ecommerce.model.OrderCreatedMessage;
import com.mensageria.ecommerce.service.OrderPublisher;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderPublisher orderPublisher;

     public OrderController(OrderPublisher orderPublisher) {
        this.orderPublisher = orderPublisher;
    }

    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody OrderCreatedMessage message) {
        // Publica a mensagem no broker
        orderPublisher.publishOrderCreated(message);
        // Retorna resposta simples para o cliente
        return ResponseEntity.ok("Order message published: " + message.orderId());
    }
}