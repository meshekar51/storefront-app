package com.storefront.notification;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *   storefront.orders (topic) ──order.confirmed──▶ notification.order-confirmed
 *                                                        │ message fails
 *                                                        ▼
 *   storefront.dlx (fanout) ───────────────────────▶ notification.order-confirmed.dlq
 *
 * Spring declares all of this on startup (idempotent), so nothing is created by hand
 * in the Amazon MQ console.
 */
@Configuration
public class RabbitConfig {

    public static final String ORDERS_EXCHANGE = "storefront.orders";
    public static final String QUEUE = "notification.order-confirmed";
    public static final String DLX = "storefront.dlx";
    public static final String DLQ = QUEUE + ".dlq";

    @Bean
    TopicExchange ordersExchange() {
        return new TopicExchange(ORDERS_EXCHANGE, true, false);
    }

    @Bean
    FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DLX, true, false);
    }

    @Bean
    Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE).deadLetterExchange(DLX).build();
    }

    @Bean
    Queue notificationDeadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(ordersExchange()).with("order.confirmed");
    }

    @Bean
    Binding deadLetterBinding() {
        return BindingBuilder.bind(notificationDeadLetterQueue()).to(deadLetterExchange());
    }

    /**
     * The publisher's class name travels in a header; ignore it and convert to
     * the listener's own parameter type instead. Services share a JSON
     * contract, not Java classes.
     */
    @Bean
    MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}
