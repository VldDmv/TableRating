package org.criticizer.controller.category;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.criticizer.dto.movie.MovieResponse;
import org.criticizer.entity.Movie;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.movie.MovieService;
import org.criticizer.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MovieController Tests")
class MovieControllerTest {

    @Mock private MovieService movieService;

    @Mock private SecurityUtil securityUtil;

    @InjectMocks private MovieController controller;

    @Test
    @DisplayName("Should return correct entity name")
    void shouldReturnCorrectEntityName() {
        assertEquals("Movie", controller.getEntityName());
    }

    @Test
    @DisplayName("Should convert Movie to MovieResponse")
    void shouldConvertMovieToResponse() {
        // Given
        Movie movie = TestDataBuilder.createMovie(1, "Test Movie", 1, 85);

        // When
        MovieResponse response = controller.convertToResponse(movie);

        // Then
        assertEquals(movie.getId(), response.getId());
        assertEquals(movie.getName(), response.getName());
    }
}
