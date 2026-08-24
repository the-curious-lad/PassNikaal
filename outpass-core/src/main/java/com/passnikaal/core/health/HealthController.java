package com.passnikaal.core.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HealthController - Public health check endpoint.
 *
 * GET /api/v1/health -> 200 OK
 *
 * Used by:
 *   - AWS ALB target group health checks (decides whether to route traffic here)
 *   - Developers confirming the service is up after deployment
 *   - Monitoring tools
 *
 * No authentication required (configured in SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    /**
     * Injected from spring.application.name in application.properties.
     * Confirms which service is responding when multiple services share a load balancer.
     */
    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Health check endpoint.
     *
     * Returns a JSON body with:
     *   status    - "UP" while the service is alive and responding
     *   service   - name of this Spring Boot application
     *   timestamp - server time at the moment of the request (confirms clock is correct)
     *
     * LinkedHashMap preserves insertion order so keys appear in the order they were added.
     *
     * @return 200 OK with JSON health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {

        // Build response maintaining a predictable key order in the JSON output
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", applicationName);
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return ResponseEntity.ok(response);
    }
}