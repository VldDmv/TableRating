package org.criticizer.controller.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.criticizer.dto.genre.CreateGenreRequest;
import org.criticizer.dto.genre.GenreResponse;
import org.criticizer.service.genre.GenreService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenreController Tests")
class GenreControllerTest {

    @Mock
    private GenreService genreService;

    @InjectMocks
    private GenreController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GET /api/genres - Should return all genres")
    void shouldReturnAllGenres() throws Exception {
        // Given
        when(genreService.getAllGenres()).thenReturn(List.of(
                new GenreResponse(1, "Action", List.of("movie")),
                new GenreResponse(2, "Comedy", List.of("movie", "show"))
        ));

        // When & Then
        mockMvc.perform(get("/api/genres")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(genreService).getAllGenres();
    }

    @Test
    @DisplayName("POST /api/genres - Should create genre")
    void shouldCreateGenre() throws Exception {
        // Given
        CreateGenreRequest request = new CreateGenreRequest("Drama", List.of("movie"));
        GenreResponse response = new GenreResponse(1, "Drama", List.of("movie"));
        when(genreService.createGenre(any(CreateGenreRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/genres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Drama"));

        verify(genreService).createGenre(any(CreateGenreRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/genres/{id} - Should delete genre")
    void shouldDeleteGenre() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/genres/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Genre deleted successfully"));

        verify(genreService).deleteGenre(1);
    }
}
