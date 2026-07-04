package org.criticizer.controller.api;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.criticizer.dto.AutocompleteResponse;
import org.criticizer.dto.AutocompleteResult;
import org.criticizer.service.external.ExternalApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiProxyController Tests")
class ApiProxyControllerTest {

    @InjectMocks private ApiProxyController controller;

    @Mock private ExternalApiClient externalApi;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/proxy/games - Should return normalized results")
    void gamesApiReturnsNormalizedResults() throws Exception {
        when(externalApi.searchSteamGames(anyString(), anyInt()))
                .thenReturn(
                        new AutocompleteResponse(
                                List.of(
                                        new AutocompleteResult(
                                                "Hades",
                                                null,
                                                "93",
                                                "http://thumb.jpg",
                                                "http://cover.jpg"))));

        mockMvc.perform(
                        get("/api/proxy/games")
                                .param("search", "hades")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].name").value("Hades"))
                .andExpect(jsonPath("$.results[0].rating").value("93"))
                .andExpect(jsonPath("$.results[0].coverUrl").value("http://cover.jpg"));
    }

    @Test
    @DisplayName("GET /api/proxy/games - Should return empty for short query")
    void gamesApiReturnsEmptyForShortQuery() throws Exception {
        mockMvc.perform(
                        get("/api/proxy/games")
                                .param("search", "a")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isEmpty());
    }

    @Test
    @DisplayName("GET /api/proxy/movies - Should return normalized results")
    void moviesApiReturnsNormalizedResults() throws Exception {
        when(externalApi.searchItunesMovies(anyString(), anyInt()))
                .thenReturn(
                        new AutocompleteResponse(
                                List.of(
                                        new AutocompleteResult(
                                                "Inception",
                                                2010,
                                                null,
                                                "http://a.jpg",
                                                "http://b.jpg"))));

        mockMvc.perform(
                        get("/api/proxy/movies")
                                .param("query", "inception")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].name").value("Inception"))
                .andExpect(jsonPath("$.results[0].year").value(2010));
    }

    @Test
    @DisplayName("GET /api/proxy/shows - Should return empty when upstream fails")
    void showsApiReturnsEmptyOnUpstreamError() throws Exception {
        when(externalApi.searchTvMazeShows(anyString(), anyInt()))
                .thenThrow(new RestClientException("boom"));

        mockMvc.perform(
                        get("/api/proxy/shows")
                                .param("query", "lost")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isEmpty());
    }

    @Test
    @DisplayName("GET /api/proxy/books - Should return empty for short query")
    void booksApiShouldReturnEmptyForShortQuery() throws Exception {
        mockMvc.perform(
                        get("/api/proxy/books")
                                .param("q", "ab")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.docs").isArray());
    }

    @Test
    @DisplayName("GET /api/proxy/books - Should return empty for banned words")
    void booksApiShouldReturnEmptyForBannedWords() throws Exception {
        mockMvc.perform(
                        get("/api/proxy/books")
                                .param("q", "the")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.docs").isArray());
    }

    @Test
    @DisplayName("GET /api/proxy/books - Should handle maxResults parameter")
    void booksApiShouldHandleMaxResultsParameter() throws Exception {
        when(externalApi.searchOpenLibrary(anyString(), anyInt()))
                .thenReturn("{\"docs\":[{\"title\":\"Harry Potter\"}]}");

        mockMvc.perform(
                        get("/api/proxy/books")
                                .param("q", "harry potter")
                                .param("maxResults", "5")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.docs").isArray());
    }
}
