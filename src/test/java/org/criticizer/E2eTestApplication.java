package org.criticizer;

import org.springframework.boot.SpringApplication;

/**
 * Entry point for browser (Playwright) tests: boots the real application on the test classpath with
 * the e2e profile — in-memory H2 instead of MySQL, so no external services are needed. Started by
 * Playwright's webServer via {@code mvn spring-boot:test-run} (see playwright.config.js).
 */
public class E2eTestApplication {

    public static void main(String[] args) {
        SpringApplication.from(TableRatingApplication::main)
                .withAdditionalProfiles("e2e")
                .run(args);
    }
}
