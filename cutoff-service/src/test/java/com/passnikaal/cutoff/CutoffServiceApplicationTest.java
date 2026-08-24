package com.passnikaal.cutoff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: verifies the cutoff-service Spring context loads without errors.
 */
@SpringBootTest
@ActiveProfiles("dev")
class CutoffServiceApplicationTest {

    @Test
    void contextLoads() {
        // If the context starts, this test passes.
    }
}