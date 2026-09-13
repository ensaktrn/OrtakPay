package com.ortakpay.notification;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Base for full-stack tests: real Postgres AND real RabbitMQ via Testcontainers,
 * both wired in via {@code @ServiceConnection}. Same singleton-container pattern
 * as core-service's AbstractIntegrationTest/AbstractRepositoryIT (manual start in
 * a static initializer, no {@code @Testcontainers}/{@code @Container} JUnit5
 * extension) for the same reason: that extension ties container lifecycle to one
 * test class, which breaks sharing a container across many subclasses.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    static {
        POSTGRES.start();
        RABBITMQ.start();
    }
}
