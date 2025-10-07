package com.mensageria.ecommerce.consumer;

import com.mensageria.ecommerce.model.OrderCreatedMessage;
import com.mensageria.ecommerce.rabbitmq.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void processOrderCreated(OrderCreatedMessage message) {
        log.info("Received OrderCreatedMessage: {}", message);
        // aqui você pode simular lógica de negócio (estoque, etc.)
    }
}
