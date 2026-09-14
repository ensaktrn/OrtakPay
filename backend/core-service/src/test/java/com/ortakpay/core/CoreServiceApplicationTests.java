package com.ortakpay.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * The only test that boots the full context without also touching RabbitMQ
 * through a real listener/publish path, so it's the one place a missing
 * broker would otherwise go unnoticed - RabbitConfig's RabbitAdmin declares
 * the events exchange on startup, which needs a broker to declare it against.
 * Declared here rather than in AbstractIntegrationTest so only this class
 * pays for a container that no other test currently needs.
 */
class CoreServiceApplicationTests extends AbstractIntegrationTest {

    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    static {
        RABBITMQ.start();
    }

    @Test
    void contextLoads() {}
}
