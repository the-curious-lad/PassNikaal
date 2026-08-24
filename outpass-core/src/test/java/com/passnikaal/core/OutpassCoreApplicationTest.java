package com.passnikaal.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: verifies the entire Spring application context loads without errors.
 * If any @Bean is broken or any required property is missing, this test fails.
 */
@SpringBootTest
@ActiveProfiles("dev")
class OutpassCoreApplicationTest {

    @Test
    void contextLoads() {
        // Empty body is intentional. If the context fails to start,
        // Spring throws an exception before this method runs.
    }
}