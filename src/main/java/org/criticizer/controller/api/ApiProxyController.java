package org.criticizer.controller.api;

import org.criticizer.dto.AutocompleteResponse;
import org.criticizer.service.external.ExternalApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

/**
 * Server-side proxy for the autocomplete providers (Steam, iTunes, TVMaze, OpenLibrary). Keeps
 * external calls off the browser, shares one Caffeine cache across users, and serves games / movies
 * / shows in a single normalized response format.
 */
@RestController
@RequestMapping("/api/proxy")
public class ApiProxyController {

    private static final Logger log = LoggerFactory.getLogger(ApiProxyController.class);
    private static final int MIN_QUERY_LENGTH = 2;
    private static final int MAX_RESULTS = 10;

    private final ExternalApiClient externalApi;

    public ApiProxyController(ExternalApiClient externalApi) {
        this.externalApi = externalApi;
    }

    /** Game search via the key-less Steam store search. */
    @GetMapping("/games")
    public ResponseEntity<AutocompleteResponse> proxyGamesApi(
            @RequestParam String search, @RequestParam(defaultValue = "10") int pageSize) {
        String query = search.trim();
        if (query.length() < MIN_QUERY_LENGTH) {
            return ResponseEntity.ok(AutocompleteResponse.empty());
        }
        try {
            return ResponseEntity.ok(
                    externalApi.searchSteamGames(
                            ExternalApiClient.normalize(query), capped(pageSize)));
        } catch (RestClientException e) {
            log.error("[ApiProxy] Steam API error: {}", e.getMessage());
            return ResponseEntity.ok(AutocompleteResponse.empty());
        }
    }

    /** Movie search via the key-less iTunes Search API. */
    @GetMapping("/movies")
    public ResponseEntity<AutocompleteResponse> proxyMoviesApi(@RequestParam String query) {
        String q = query.trim();
        if (q.length() < MIN_QUERY_LENGTH) {
            return ResponseEntity.ok(AutocompleteResponse.empty());
        }
        try {
            return ResponseEntity.ok(
                    externalApi.searchItunesMovies(ExternalApiClient.normalize(q), MAX_RESULTS));
        } catch (RestClientException e) {
            log.error("[ApiProxy] iTunes API error: {}", e.getMessage());
            return ResponseEntity.ok(AutocompleteResponse.empty());
        }
    }

    /** TV show search via the key-less TVMaze API. */
    @GetMapping("/shows")
    public ResponseEntity<AutocompleteResponse> proxyShowsApi(@RequestParam String query) {
        String q = query.trim();
        if (q.length() < MIN_QUERY_LENGTH) {
            return ResponseEntity.ok(AutocompleteResponse.empty());
        }
        try {
            return ResponseEntity.ok(
                    externalApi.searchTvMazeShows(ExternalApiClient.normalize(q), MAX_RESULTS));
        } catch (RestClientException e) {
            log.error("[ApiProxy] TVMaze API error: {}", e.getMessage());
            return ResponseEntity.ok(AutocompleteResponse.empty());
        }
    }

    /** Open Library Books API */
    @GetMapping("/books")
    public ResponseEntity<String> proxyOpenLibraryApi(
            @RequestParam String q, @RequestParam(defaultValue = "10") int maxResults) {
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
            String body =
                    externalApi.searchOpenLibrary(ExternalApiClient.normalize(query), maxResults);

            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);

        } catch (RestClientException e) {
            log.error("[ApiProxy] Open Library API error: {}", e.getMessage());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"docs\":[]}");
        }
    }

    private static int capped(int requested) {
        return Math.max(1, Math.min(requested, MAX_RESULTS));
    }
}
