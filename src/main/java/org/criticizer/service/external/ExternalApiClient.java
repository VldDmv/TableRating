package org.criticizer.service.external;

import org.criticizer.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Wraps calls to external media APIs (RAWG, TMDB, OpenLibrary).
 * Caches successful responses in the "externalApi" Caffeine cache so that
 * repeated autocomplete queries don't hit the upstream provider.
 */
@Service
public class ExternalApiClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiClient.class);

    private final RestTemplate restTemplate;

    public ExternalApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable(value = CacheConfig.EXTERNAL_API_CACHE, key = "'openlibrary:' + #query + ':' + #limit")
    public String searchOpenLibrary(String query, int limit) {
        String url = "https://openlibrary.org/search.json?"
                + "q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&limit=" + limit;

        log.debug("[ExternalApi] OpenLibrary fetch: {}", url);
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return response.getBody();
    }

    /**
     * Build a cache key fragment that ignores case and trims whitespace.
     */
    public static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
