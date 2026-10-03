package com.storefront.order.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * order-service only owns the EXCHANGE. Each consumer (notification today,
 * delivery tomorrow) declares and binds its own queue — the publisher never
 * needs to know who is listening.
 */
@Configuration
public class RabbitConfig {

    public static final String ORDERS_EXCHANGE = "storefront.orders";
    public static final String ORDER_CONFIRMED = "order.confirmed";

    @Bean
    TopicExchange ordersExchange() {
        return new TopicExchange(ORDERS_EXCHANGE, true, false);
    }

    /** Messages are JSON, readable by any language and in the RabbitMQ console. */
    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
