

package org.criticizer.dao.genre;

import org.criticizer.entity.Genre;

import java.util.List;

public interface GenreDao {

    void addGenre(String genreName, List<String> mediaTypes);

    void updateGenre(int genreId, String newGenreName, List<String> mediaTypes);

    void deleteGenre(int genreId);

    boolean isGenreInUse(int genreId);

    List<Genre> getAvailableGenresFor(String mediaType);

    List<Genre> getAllGenres();

    List<Genre> getGenresForMovie(int movieId);

    List<Genre> getGenresForBook(int bookId);

    List<Genre> getGenresForShow(int showId);
}