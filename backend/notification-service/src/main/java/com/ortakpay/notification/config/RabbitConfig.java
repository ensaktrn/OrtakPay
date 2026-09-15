package com.ortakpay.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * JacksonJsonMessageConverter for the same reason as core-service's RabbitConfig
 * (see docs/adr/0007-jackson-2-3-coexistence.md): the deprecated Jackson2 variant
 * would reintroduce the two-Jackson-stack split that ADR exists to avoid.
 *
 * <p>Each queue gets its own dead-letter queue via a shared dead-letter exchange:
 * a rejected message keeps the routing key it was originally delivered with, so
 * binding "<queue>.dlq" to the DLX with that same key is enough to catch it -
 * no x-dead-letter-routing-key override needed. defaultRequeueRejected(false) on
 * the listener container factory means a failing message is rejected (not
 * requeued) and goes straight to its DLQ instead of looping forever.
 */
@Configuration
public class RabbitConfig {

    public static final String EVENTS_EXCHANGE = "ortakpay.events";
    public static final String DEAD_LETTER_EXCHANGE = "ortakpay.events.dlx";

    public static final String EXPENSE_CREATED_QUEUE = "notification.expense-created";
    public static final String GROUP_MEMBER_ADDED_QUEUE = "notification.group-member-added";
    public static final String SETTLEMENT_RECORDED_QUEUE = "notification.settlement-recorded";
    public static final String SETTLEMENT_REMINDER_QUEUE = "notification.settlement-reminder";

    private static final String EXPENSE_CREATED_ROUTING_KEY = "expense.created";
    private static final String GROUP_MEMBER_ADDED_ROUTING_KEY = "group.member.added";
    private static final String SETTLEMENT_RECORDED_ROUTING_KEY = "settlement.recorded";
    private static final String SETTLEMENT_REMINDER_ROUTING_KEY = "settlement.reminder";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Declarables expenseCreatedDeclarables() {
        return queueWithDeadLetter(EXPENSE_CREATED_QUEUE, EXPENSE_CREATED_ROUTING_KEY);
    }

    @Bean
    public Declarables groupMemberAddedDeclarables() {
        return queueWithDeadLetter(GROUP_MEMBER_ADDED_QUEUE, GROUP_MEMBER_ADDED_ROUTING_KEY);
    }

    @Bean
    public Declarables settlementRecordedDeclarables() {
        return queueWithDeadLetter(SETTLEMENT_RECORDED_QUEUE, SETTLEMENT_RECORDED_ROUTING_KEY);
    }

    @Bean
    public Declarables settlementReminderDeclarables() {
        return queueWithDeadLetter(SETTLEMENT_REMINDER_QUEUE, SETTLEMENT_REMINDER_ROUTING_KEY);
    }

    private Declarables queueWithDeadLetter(String queueName, String routingKey) {
        Queue queue = QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .build();
        Queue deadLetterQueue = QueueBuilder.durable(queueName + ".dlq").build();
        Binding binding = BindingBuilder.bind(queue).to(eventsExchange()).with(routingKey);
        Binding deadLetterBinding =
                BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange()).with(routingKey);
        return new Declarables(queue, deadLetterQueue, binding, deadLetterBinding);
    }

    @Bean
    public MessageConverter messageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
