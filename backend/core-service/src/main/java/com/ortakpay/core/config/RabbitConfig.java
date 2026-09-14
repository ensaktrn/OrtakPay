package com.ortakpay.core.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code JacksonJsonMessageConverter} (Spring AMQP 4.x, Jackson 3 / {@code tools.jackson})
 * is used deliberately instead of the older {@code Jackson2JsonMessageConverter}:
 * Spring Boot 4 defaults to Jackson 3 (see docs/adr/0007-jackson-2-3-coexistence.md),
 * and the Jackson 2 converter is now deprecated - using it here would reintroduce
 * exactly the two-stack split that ADR exists to contain.
 */
@Configuration
public class RabbitConfig {

    public static final String EVENTS_EXCHANGE = "ortakpay.events";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter messageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
