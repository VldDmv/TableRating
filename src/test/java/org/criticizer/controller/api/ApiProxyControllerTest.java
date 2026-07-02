package org.criticizer.controller.api;

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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiProxyController Tests")
class ApiProxyControllerTest {

    @InjectMocks
    private ApiProxyController controller;

    @Mock
    private ExternalApiClient externalApi;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/proxy/games - Should return service unavailable")
    void gamesApiShouldBeDisabled() throws Exception {
        mockMvc.perform(get("/api/proxy/games")
                        .param("search", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /api/proxy/movies - Should return service unavailable")
    void moviesApiShouldBeDisabled() throws Exception {
        mockMvc.perform(get("/api/proxy/movies")
                        .param("query", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /api/proxy/shows - Should return service unavailable")
    void showsApiShouldBeDisabled() throws Exception {
        mockMvc.perform(get("/api/proxy/shows")
                        .param("query", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /api/proxy/books - Should return empty for short query")
    void booksApiShouldReturnEmptyForShortQuery() throws Exception {
        mockMvc.perform(get("/api/proxy/books")
                        .param("q", "ab")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.docs").isArray());
    }

    @Test
    @DisplayName("GET /api/proxy/books - Should return empty for banned words")
    void booksApiShouldReturnEmptyForBannedWords() throws Exception {
        mockMvc.perform(get("/api/proxy/books")
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

        mockMvc.perform(get("/api/proxy/books")
                        .param("q", "harry potter")
                        .param("maxResults", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.docs").isArray());
    }
}