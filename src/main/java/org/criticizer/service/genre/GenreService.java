package org.criticizer.service.genre;

import org.criticizer.entity.Genre;

import java.util.List;

public interface GenreService {
    List<Genre> getAllGenres();

    void createGenre(String genreName, List<String> mediaTypes);

    void editGenre(int genreId, String newGenreName, List<String> mediaTypes);

    void removeGenre(int genreId);

    List<Genre> getAvailableGenresFor(String mediaType);


    List<Genre> getGenresForMovie(int movieId);


    List<Genre> getGenresForBook(int bookId);

    List<Genre> getGenresForShow(int showId);
}

