package com.lezztto.LmfBank;

import com.lezztto.LmfBank.support.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: the full application context starts against a real PostgreSQL
 * (Testcontainers) with Kafka listeners disabled by the {@code test} profile.
 */
@SpringBootTest
@ActiveProfiles("test")
class LmfBankApplicationIT extends PostgresContainerSupport {

    @Test
    void contextLoads() {
    }
}
