package org.criticizer.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-memory cache for external API proxy responses (RAWG, TMDB, OpenLibrary). Reduces external
 * rate-limit pressure and speeds up repeated autocomplete queries.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String EXTERNAL_API_CACHE = "externalApi";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(EXTERNAL_API_CACHE);
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(1_000)
                        .expireAfterWrite(Duration.ofHours(24))
                        .recordStats());
        return manager;
    }
}
