package com.ortakpay.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.ortakpay.notification.AbstractIntegrationTest;
import com.ortakpay.notification.config.RabbitConfig;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Publishes a body that isn't valid JSON at all, bypassing convertAndSend (which
 * would just serialize a String as one) so JacksonJsonMessageConverter genuinely
 * fails to parse it on the consumer side. That failure is fatal (not requeued)
 * per RabbitConfig's defaultRequeueRejected(false), so RabbitMQ routes the
 * rejected message straight to the queue's dead-letter queue.
 */
class NotificationListenerDeadLetterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void malformedMessage_isRoutedToDeadLetterQueue() {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        Message malformedMessage = new Message("{not valid json".getBytes(StandardCharsets.UTF_8), properties);

        rabbitTemplate.send(RabbitConfig.EVENTS_EXCHANGE, "expense.created", malformedMessage);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Message deadLettered = rabbitTemplate.receive(RabbitConfig.EXPENSE_CREATED_QUEUE + ".dlq", 200);
            assertThat(deadLettered).isNotNull();
        });
    }
}
