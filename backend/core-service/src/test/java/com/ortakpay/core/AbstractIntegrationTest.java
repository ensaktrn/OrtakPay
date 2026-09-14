package com.ortakpay.core;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for full-stack (controller + security filter chain + real Postgres) tests.
 * Deliberately separate from {@code repository.AbstractRepositoryIT}, which only
 * boots the {@code @DataJpaTest} slice - these need the whole application context
 * (security config, controllers, MockMvc) instead.
 *
 * <p>Uses the same Testcontainers singleton-container pattern as
 * {@code AbstractRepositoryIT} (manual start, no {@code @Testcontainers}/
 * {@code @Container} lifecycle) for the same reason: that JUnit5 extension ties
 * container start/stop to one test class, which breaks sharing across subclasses.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
