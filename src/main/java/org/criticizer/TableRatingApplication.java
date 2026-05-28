package org.criticizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Main Spring Boot Application Class @SpringBootApplication combines: - @Configuration: Tags the
 * class as a source of bean definitions - @EnableAutoConfiguration: Enables Spring Boot's
 * auto-configuration - @ComponentScan: Enables component scanning in the package and all
 * sub-packages
 */
@SpringBootApplication
public class TableRatingApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(TableRatingApplication.class, args);
    }

    /**
     * Configure the application when deployed as WAR (optional - only needed if you want WAR
     * deployment)
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(TableRatingApplication.class);
    }
}
