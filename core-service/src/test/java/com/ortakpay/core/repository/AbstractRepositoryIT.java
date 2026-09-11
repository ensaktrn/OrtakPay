package com.ortakpay.core.repository;

import com.ortakpay.core.config.JpaAuditingConfig;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for repository tests: boots one shared Postgres container for the whole test
 * JVM run and wires it into the Spring context via {@code @ServiceConnection}, so the
 * real datasource/Flyway/Hibernate autoconfiguration runs against real Postgres - no H2.
 *
 * <p>This deliberately skips the JUnit5 {@code @Testcontainers}/{@code @Container}
 * extension: that extension ties container start/stop to a single test class's
 * lifecycle, so when the static field is inherited by many subclasses it stops (and
 * has to fully re-provision) the container after every subclass's tests instead of
 * sharing one instance across all of them - which turned every repository test class
 * into a ~30s container-restart-then-timeout failure. Starting it once in a static
 * initializer and never stopping it is Testcontainers' documented "singleton
 * container" pattern; Ryuk reaps it when the JVM exits.
 *
 * <p>{@code Replace.NONE} stops {@code @DataJpaTest} from swapping in an embedded DB,
 * which is its default behavior and would silently defeat the point of this setup.
 * {@code @Import(JpaAuditingConfig.class)} is required because the {@code @DataJpaTest}
 * slice excludes plain {@code @Configuration} beans from its component scan, so
 * {@code @EnableJpaAuditing} would otherwise never activate and createdAt/updatedAt
 * would stay null in these tests.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
abstract class AbstractRepositoryIT {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
