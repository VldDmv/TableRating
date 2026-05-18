package org.criticizer.controller.api;

import org.criticizer.service.external.ExternalApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

@RestController
@RequestMapping("/api/proxy")
public class ApiProxyController {

    private static final Logger log = LoggerFactory.getLogger(ApiProxyController.class);
    private final ExternalApiClient externalApi;

    public ApiProxyController(ExternalApiClient externalApi) {
        this.externalApi = externalApi;
    }

    @GetMapping("/games")
    public ResponseEntity<String> proxyGamesApi(
            @RequestParam String search,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        log.info("[ApiProxy] Games API called but DISABLED - search: {}", search);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\": \"Games autocomplete is temporarily disabled.\"}");
    }

    @GetMapping("/movies")
    public ResponseEntity<String> proxyMoviesApi(@RequestParam String query) {
        log.info("[ApiProxy] Movies API called but DISABLED - query: {}", query);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\": \"Movies autocomplete is temporarily disabled.\"}");
    }

    @GetMapping("/shows")
    public ResponseEntity<String> proxyShowsApi(@RequestParam String query) {
        log.info("[ApiProxy] Shows API called but DISABLED - query: {}", query);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\": \"Shows autocomplete is temporarily disabled.\"}");
    }

    /**
     * Open Library Books API
     */
    @GetMapping("/books")
    public ResponseEntity<String> proxyOpenLibraryApi(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int maxResults
    ) {
        log.info("[ApiProxy] Open Library API called - query: '{}'", q);

        String query = q.trim();

        if (query.length() < 3) {
            log.warn("[ApiProxy] Query '{}' too short (< 3 chars), returning empty", query);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"docs\":[]}");
        }

        String queryLower = query.toLowerCase();
        String[] bannedWords = {"the", "and", "or", "of", "in", "to", "is", "it", "for"};

        for (String banned : bannedWords) {
            if (queryLower.equals(banned)) {
                log.warn("[ApiProxy] Query '{}' is a banned word, returning empty", query);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"docs\":[]}");
            }
        }

        try {
            String body = externalApi.searchOpenLibrary(
                    ExternalApiClient.normalize(query), maxResults);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);

        } catch (RestClientException e) {
            log.error("[ApiProxy] Open Library API error: {}", e.getMessage());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"docs\":[]}");
        }
    }
}