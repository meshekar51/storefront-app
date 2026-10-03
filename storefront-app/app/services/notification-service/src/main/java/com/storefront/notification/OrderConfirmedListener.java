package com.storefront.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * "Consumed by" notification service → "Email, SMS or push notification".
 * This demo logs the message; swap the body for Amazon SES / SNS calls.
 * If this method throws, the message is NOT requeued forever — it goes to the
 * dead-letter queue (spring.rabbitmq.listener.simple.default-requeue-rejected=false).
 */
@Component
public class OrderConfirmedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmedListener.class);

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        log.info("📧 Email to customer: order {} confirmed — {} item(s), total {} {}",
                event.orderRef(), event.itemCount(), event.total(), event.currency());
    }
}
