package org.criticizer.service.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.criticizer.config.CacheConfig;
import org.criticizer.dto.AutocompleteResponse;
import org.criticizer.dto.AutocompleteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Wraps calls to external media APIs. All providers are key-less: Steam store search (games),
 * iTunes Search (movies), TVMaze (shows) and OpenLibrary (books). Responses for games, movies and
 * shows are normalized into {@link AutocompleteResponse} so the frontend uses one format for every
 * category. Successful responses are cached in the "externalApi" Caffeine cache so repeated
 * autocomplete queries don't hit the upstream provider (which also keeps us far away from the
 * providers' rate limits).
 */
@Service
public class ExternalApiClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiClient.class);

    private static final String STEAM_SEARCH_URL =
            "https://store.steampowered.com/api/storesearch/?cc=us&l=en&term=";
    private static final String STEAM_HEADER_IMAGE =
            "https://cdn.cloudflare.steamstatic.com/steam/apps/%d/header.jpg";
    private static final String ITUNES_SEARCH_URL =
            "https://itunes.apple.com/search?media=movie&entity=movie&term=";
    private static final String TVMAZE_SEARCH_URL = "https://api.tvmaze.com/search/shows?q=";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ExternalApiClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /** Steam store search: name + capsule thumbnail; the header image serves as the cover. */
    @Cacheable(value = CacheConfig.EXTERNAL_API_CACHE, key = "'steam:' + #query + ':' + #limit")
    public AutocompleteResponse searchSteamGames(String query, int limit) {
        String url = STEAM_SEARCH_URL + encode(query);
        log.debug("[ExternalApi] Steam fetch: {}", url);

        JsonNode root = fetchJson(url);
        if (root == null) return AutocompleteResponse.empty();

        List<AutocompleteResult> results = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            if (results.size() >= limit) break;
            String name = item.path("name").asText(null);
            long appId = item.path("id").asLong(0);
            if (name == null || appId == 0) continue;

            String metascore = item.path("metascore").asText("");
            results.add(
                    new AutocompleteResult(
                            name,
                            null, // Steam store search does not expose a release year
                            metascore.isBlank() ? null : metascore,
                            item.path("tiny_image").asText(null),
                            String.format(STEAM_HEADER_IMAGE, appId)));
        }
        return new AutocompleteResponse(results);
    }

    /** iTunes movie search: artwork URL is upscaled from the 100px thumbnail Apple returns. */
    @Cacheable(value = CacheConfig.EXTERNAL_API_CACHE, key = "'itunes:' + #query + ':' + #limit")
    public AutocompleteResponse searchItunesMovies(String query, int limit) {
        String url = ITUNES_SEARCH_URL + encode(query) + "&limit=" + limit;
        log.debug("[ExternalApi] iTunes fetch: {}", url);

        JsonNode root = fetchJson(url);
        if (root == null) return AutocompleteResponse.empty();

        List<AutocompleteResult> results = new ArrayList<>();
        for (JsonNode item : root.path("results")) {
            if (results.size() >= limit) break;
            String name = item.path("trackName").asText(null);
            if (name == null) continue;

            String artwork = item.path("artworkUrl100").asText(null);
            results.add(
                    new AutocompleteResult(
                            name,
                            yearFrom(item.path("releaseDate").asText("")),
                            null, // iTunes has no public score
                            artwork,
                            artwork == null ? null : artwork.replace("100x100bb", "600x600bb")));
        }
        return new AutocompleteResponse(results);
    }

    /** TVMaze show search: returns an array of {score, show} wrappers. */
    @Cacheable(value = CacheConfig.EXTERNAL_API_CACHE, key = "'tvmaze:' + #query + ':' + #limit")
    public AutocompleteResponse searchTvMazeShows(String query, int limit) {
        String url = TVMAZE_SEARCH_URL + encode(query);
        log.debug("[ExternalApi] TVMaze fetch: {}", url);

        JsonNode root = fetchJson(url);
        if (root == null) return AutocompleteResponse.empty();

        List<AutocompleteResult> results = new ArrayList<>();
        for (JsonNode wrapper : root) {
            if (results.size() >= limit) break;
            JsonNode show = wrapper.path("show");
            String name = show.path("name").asText(null);
            if (name == null) continue;

            JsonNode rating = show.path("rating").path("average");
            JsonNode image = show.path("image");
            results.add(
                    new AutocompleteResult(
                            name,
                            yearFrom(show.path("premiered").asText("")),
                            rating.isNumber() ? String.valueOf(rating.asDouble()) : null,
                            image.path("medium").asText(null),
                            image.path("original").asText(null)));
        }
        return new AutocompleteResponse(results);
    }

    @Cacheable(
            value = CacheConfig.EXTERNAL_API_CACHE,
            key = "'openlibrary:' + #query + ':' + #limit")
    public String searchOpenLibrary(String query, int limit) {
        String url =
                "https://openlibrary.org/search.json?" + "q=" + encode(query) + "&limit=" + limit;

        log.debug("[ExternalApi] OpenLibrary fetch: {}", url);
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return response.getBody();
    }

    /** Build a cache key fragment that ignores case and trims whitespace. */
    public static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    /** Fetches and parses JSON; returns null (→ empty result) on a malformed body. */
    private JsonNode fetchJson(String url) {
        String body = restTemplate.getForEntity(url, String.class).getBody();
        if (body == null) return null;
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            log.warn("[ExternalApi] Unparseable response from {}: {}", url, e.getMessage());
            return null;
        }
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static Integer yearFrom(String date) {
        return date != null
                        && date.length() >= 4
                        && date.chars().limit(4).allMatch(Character::isDigit)
                ? Integer.parseInt(date.substring(0, 4))
                : null;
    }
}
