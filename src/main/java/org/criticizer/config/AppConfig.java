package org.criticizer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Main application configuration. Replaces AppContextListener and web.xml configuration. */
@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        mapper.registerModule(new JavaTimeModule());

        mapper.configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);

        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }

    /** Application-specific properties. */
    @Bean
    @ConfigurationProperties(prefix = "app")
    public AppProperties appProperties() {
        return new AppProperties();
    }

    /** Properties class for app-specific configuration. */
    public static class AppProperties {
        private Pagination pagination = new Pagination();
        private Security security = new Security();

        public Pagination getPagination() {
            return pagination;
        }

        public void setPagination(Pagination pagination) {
            this.pagination = pagination;
        }

        public Security getSecurity() {
            return security;
        }

        public void setSecurity(Security security) {
            this.security = security;
        }

        public static class Pagination {
            private int defaultPageSize = 10;
            private int maxPageSize = 100;

            public int getDefaultPageSize() {
                return defaultPageSize;
            }

            public void setDefaultPageSize(int defaultPageSize) {
                this.defaultPageSize = defaultPageSize;
            }

            public int getMaxPageSize() {
                return maxPageSize;
            }

            public void setMaxPageSize(int maxPageSize) {
                this.maxPageSize = maxPageSize;
            }
        }

        public static class Security {
            private int bcryptStrength = 10;
            private String rememberMeKey;

            public int getBcryptStrength() {
                return bcryptStrength;
            }

            public void setBcryptStrength(int bcryptStrength) {
                this.bcryptStrength = bcryptStrength;
            }

            public String getRememberMeKey() {
                return rememberMeKey;
            }

            public void setRememberMeKey(String rememberMeKey) {
                this.rememberMeKey = rememberMeKey;
            }
        }
    }
}
