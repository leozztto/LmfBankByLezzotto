package com.lezztto.LmfBank.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Starts a single PostgreSQL container shared by every integration test in the JVM.
 *
 * <p>The container is created in a static initializer (the "singleton container" pattern)
 * instead of {@code @Container}, so it is started once and reused across all test classes
 * rather than restarted per class. Testcontainers' Ryuk sidecar stops it when the JVM ends.
 */
public abstract class PostgresContainerSupport {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("lmf_bank")
                    .withUsername("postgres")
                    .withPassword("root")
                    .withReuse(false);

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }
}
