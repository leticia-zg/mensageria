package com.mensageria.ecommerce.service;

import com.mensageria.ecommerce.model.OrderCreatedMessage;
import com.mensageria.ecommerce.rabbitmq.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public OrderPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrderCreated(OrderCreatedMessage message) {
        rabbitTemplate.convertAndSend(
            RabbitConfig.EXCHANGE_NAME,
            RabbitConfig.ROUTING_KEY,
            message
        );
        log.info("Published OrderCreatedMessage: {}", message);
        // ou, se preferir sem SLF4J:
        // System.out.println("Published OrderCreatedMessage: " + message);
    }
}
