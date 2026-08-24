package com.passnikaal.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PassNikaal - Outpass Core
 *
 * Entry point for the outpass-core Spring Boot service.
 *
 * @SpringBootApplication combines:
 *   @Configuration        - this class can define @Bean methods
 *   @EnableAutoConfiguration - auto-configures beans from classpath
 *   @ComponentScan        - scans this package and sub-packages
 *
 * Package layout:
 *   auth/         - registration, login, JWT filter, refresh token
 *   student/      - student profile endpoints
 *   approver/     - approval and rejection workflow
 *   outpass/      - outpass creation and state machine
 *   gate/         - QR scan, roll-number fallback, exit/entry
 *   notification/ - in-app and email notifications
 *   admin/        - admin-only account and hostel management
 *   common/       - shared DTOs and enums
 *   config/       - Spring Security, JWT, S3, email configs
 *   exception/    - global exception handler and domain exceptions
 *   health/       - simple health check endpoint
 */
@SpringBootApplication
public class OutpassCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(OutpassCoreApplication.class, args);
    }
}