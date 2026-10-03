package com.storefront.order.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final RabbitTemplate rabbit;

    public OrderEventPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    /**
     * Best effort: the order is already confirmed and paid, so a broker
     * outage must not fail the checkout. Production upgrade: transactional
     * outbox table + relay, so no event is ever lost.
     */
    public void orderConfirmed(OrderConfirmedEvent event) {
        try {
            rabbit.convertAndSend(RabbitConfig.ORDERS_EXCHANGE, RabbitConfig.ORDER_CONFIRMED, event);
            log.info("Published {} for {}", RabbitConfig.ORDER_CONFIRMED, event.orderRef());
        } catch (AmqpException e) {
            log.error("Could not publish event for {} — notification will be missing", event.orderRef(), e);
        }
    }
}
