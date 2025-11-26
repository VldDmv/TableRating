package org.criticizer.service;

import org.criticizer.dao.genre.GenreDao;
import org.criticizer.entity.Genre;
import org.criticizer.exceptions.data.ItemInUseException;
import org.criticizer.exceptions.validation.EmptyNameException;
import org.criticizer.service.genre.GenreServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreServiceImplTest {

    @Mock
    private GenreDao genreDaoMock;

    @InjectMocks
    private GenreServiceImpl genreService;

    @Test
    @DisplayName("createGenre should call dao.addGenre with trimmed name and provided media types")
    void createGenre_withValidNameAndMediaTypes_shouldSucceed() {
        String genreName = "  Sci-Fi  ";
        String expectedGenreName = "Sci-Fi";
        List<String> mediaTypes = List.of("movie", "book");

        genreService.createGenre(genreName, mediaTypes);

        verify(genreDaoMock, times(1)).addGenre(expectedGenreName, mediaTypes);
    }

    @Test
    @DisplayName("createGenre should use 'shared' media type if none is provided")
    void createGenre_withNullOrEmptyMediaTypes_shouldUseDefault() {
        ArgumentCaptor<List<String>> mediaTypesCaptor = ArgumentCaptor.forClass(List.class);

        genreService.createGenre("Action", null);
        verify(genreDaoMock, times(1)).addGenre(eq("Action"), mediaTypesCaptor.capture());
        assertEquals(List.of("shared"), mediaTypesCaptor.getValue());

        genreService.createGenre("Comedy", Collections.emptyList());
        verify(genreDaoMock, times(1)).addGenre(eq("Comedy"), mediaTypesCaptor.capture());
        assertEquals(List.of("shared"), mediaTypesCaptor.getValue());
    }

    @Test
    @DisplayName("createGenre should throw exception for null or empty name")
    void createGenre_withInvalidName_shouldThrowException() {
        assertThrows(EmptyNameException.class, () -> genreService.createGenre(null, List.of("movie")));
        assertThrows(EmptyNameException.class, () -> genreService.createGenre("   ", List.of("movie")));
        verify(genreDaoMock, never()).addGenre(anyString(), anyList());
    }

    @Test
    @DisplayName("editGenre should call dao.updateGenre with trimmed name")
    void editGenre_withValidName_shouldSucceed() {
        int genreId = 1;
        String newGenreName = "  Updated Genre  ";
        String expectedNewGenreName = "Updated Genre";
        List<String> mediaTypes = List.of("show");

        genreService.editGenre(genreId, newGenreName, mediaTypes);

        verify(genreDaoMock, times(1)).updateGenre(genreId, expectedNewGenreName, mediaTypes);
    }

    @Test
    @DisplayName("editGenre should use 'shared' media type if none is provided")
    void editGenre_withNullOrEmptyMediaTypes_shouldUseDefault() {
        ArgumentCaptor<List<String>> mediaTypesCaptor = ArgumentCaptor.forClass(List.class);
        int genreId = 1;

        genreService.editGenre(genreId, "Action", null);
        verify(genreDaoMock, times(1)).updateGenre(eq(genreId), eq("Action"), mediaTypesCaptor.capture());
        assertEquals(List.of("shared"), mediaTypesCaptor.getValue());

        genreService.editGenre(genreId, "Comedy", Collections.emptyList());
        verify(genreDaoMock, times(1)).updateGenre(eq(genreId), eq("Comedy"), mediaTypesCaptor.capture());
        assertEquals(List.of("shared"), mediaTypesCaptor.getValue());
    }

    @Test
    @DisplayName("removeGenre should call dao.deleteGenre when genre is not in use")
    void removeGenre_whenNotInUse_shouldSucceed() {
        int genreId = 1;
        when(genreDaoMock.isGenreInUse(genreId)).thenReturn(false);

        genreService.removeGenre(genreId);

        verify(genreDaoMock, times(1)).isGenreInUse(genreId);
        verify(genreDaoMock, times(1)).deleteGenre(genreId);
    }

    @Test
    @DisplayName("removeGenre should throw exception when genre is in use")
    void removeGenre_whenInUse_shouldThrowException() {
        int genreId = 1;
        when(genreDaoMock.isGenreInUse(genreId)).thenReturn(true);

        Exception exception = assertThrows(ItemInUseException.class, () -> genreService.removeGenre(genreId));
        assertEquals("Operation not permitted: deletegenre - Cannot delete genre: it is currently assigned to movies, books, or shows", exception.getMessage());

        verify(genreDaoMock, times(1)).isGenreInUse(genreId);
        verify(genreDaoMock, never()).deleteGenre(genreId);
    }

    @Test
    @DisplayName("pass-through methods should call their corresponding DAO methods")
    void passThroughMethods_shouldCallDaoMethods() {

        List<Genre> allGenres = List.of(new Genre(1, "Action"));
        when(genreDaoMock.getAllGenres()).thenReturn(allGenres);
        assertSame(allGenres, genreService.getAllGenres());
        verify(genreDaoMock).getAllGenres();

        String mediaType = "movie";
        List<Genre> movieGenres = List.of(new Genre(2, "Sci-Fi"));
        when(genreDaoMock.getAvailableGenresFor(mediaType)).thenReturn(movieGenres);
        assertSame(movieGenres, genreService.getAvailableGenresFor(mediaType));
        verify(genreDaoMock).getAvailableGenresFor(mediaType);

        int movieId = 10;
        List<Genre> genresForMovie = List.of(new Genre(3, "Thriller"));
        when(genreDaoMock.getGenresForMovie(movieId)).thenReturn(genresForMovie);
        assertSame(genresForMovie, genreService.getGenresForMovie(movieId));
        verify(genreDaoMock).getGenresForMovie(movieId);

        int bookId = 20;
        List<Genre> genresForBook = List.of(new Genre(4, "Fantasy"));
        when(genreDaoMock.getGenresForBook(bookId)).thenReturn(genresForBook);
        assertSame(genresForBook, genreService.getGenresForBook(bookId));
        verify(genreDaoMock).getGenresForBook(bookId);

        int showId = 30;
        List<Genre> genresForShow = List.of(new Genre(5, "Comedy"));
        when(genreDaoMock.getGenresForShow(showId)).thenReturn(genresForShow);
        assertSame(genresForShow, genreService.getGenresForShow(showId));
        verify(genreDaoMock).getGenresForShow(showId);
    }
}
