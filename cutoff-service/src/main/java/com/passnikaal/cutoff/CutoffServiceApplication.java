package com.passnikaal.cutoff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * PassNikaal - Cutoff Service
 *
 * Headless scheduled-job application. No web server (Tomcat disabled via
 * spring.main.web-application-type=none in application.properties).
 *
 * @EnableScheduling activates Spring's scheduling infrastructure.
 * Without this, all @Scheduled methods in this service will silently never run.
 *
 * Jobs this service runs:
 *   9:00 PM  - Market outpass cutoff (APPROVED->EXPIRED, EXITED->NOT_RETURNED,
 *              RETURNED->EXPIRED + hostel confirmation triggered)
 *   9:30 PM  - Hostel confirmation reminder 1
 *   10:00 PM - Hostel confirmation reminder 2
 *   10:30 PM - Hostel confirmation reminder 3
 *   9 PM     - S3 QR image cleanup for expired outpasses
 *
 * Package layout:
 *   job/          - @Scheduled job classes (CutoffJob, ReminderJob)
 *   processor/    - per-outpass state transition logic
 *   repository/   - JPA repositories (same DB as outpass-core)
 *   entity/       - JPA entity mirrors (read-only perspective)
 *   notification/ - email dispatch for hostel confirmation
 *   s3/           - S3 batch cleanup
 *   config/       - DataSource, S3, scheduler configuration
 */
@SpringBootApplication
@EnableScheduling
public class CutoffServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CutoffServiceApplication.class, args);
    }
}