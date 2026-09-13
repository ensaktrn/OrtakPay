package com.ortakpay.notification;

import org.junit.jupiter.api.Test;

/**
 * Extends AbstractIntegrationTest (Postgres + RabbitMQ Testcontainers) instead
 * of using a bare @SpringBootTest: RabbitConfig's RabbitAdmin declares queues/
 * exchanges on context startup, which needs a real broker to declare them
 * against - without Testcontainers this would silently depend on whatever
 * RabbitMQ happens to be reachable at localhost:5672.
 */
class NotificationServiceApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {}
}
