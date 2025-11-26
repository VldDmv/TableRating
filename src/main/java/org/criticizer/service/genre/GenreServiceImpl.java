package org.criticizer.service.genre;

import org.criticizer.dao.genre.GenreDao;
import org.criticizer.entity.Genre;
import org.criticizer.exceptions.validation.EmptyNameException;
import org.criticizer.exceptions.data.ItemInUseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Implementation of the GenreService interface for managing genre-related operations.
 */
public class GenreServiceImpl implements GenreService {
    private static final Logger log = LoggerFactory.getLogger(GenreServiceImpl.class);
    private final GenreDao genreDao;

    public GenreServiceImpl(GenreDao genreDao) {
        this.genreDao = genreDao;
    }

    /**
     * Retrieves all genres from the database.
     */
    @Override
    public List<Genre> getAllGenres() {
        return genreDao.getAllGenres();
    }

    /**
     * Creates a new genre with the specified name and media types.
     */
    @Override
    public void createGenre(String genreName, List<String> mediaTypes) {
        if (genreName == null || genreName.trim().isEmpty()) {
            log.warn("Attempted to create genre with empty name");
            throw new EmptyNameException("Genre name");
        }

        // Default to "shared" if no media types specified
        if (mediaTypes == null || mediaTypes.isEmpty()) {
            mediaTypes = List.of("shared");
        }

        String trimmedName = genreName.trim();
        log.info("Creating genre: {} with media types: {}", trimmedName, mediaTypes);
        genreDao.addGenre(trimmedName, mediaTypes);
    }

    /**
     * Updates an existing genre's name and media types.
     */
    @Override
    public void editGenre(int genreId, String newGenreName, List<String> mediaTypes) {
        if (newGenreName == null || newGenreName.trim().isEmpty()) {
            log.warn("Attempted to update genre {} with empty name", genreId);
            throw new EmptyNameException("Genre name");
        }

        // Default to "shared" if no media types specified
        if (mediaTypes == null || mediaTypes.isEmpty()) {
            mediaTypes = List.of("shared");
        }

        String trimmedName = newGenreName.trim();
        log.info("Updating genre ID {}: {} with media types: {}", genreId, trimmedName, mediaTypes);
        genreDao.updateGenre(genreId, trimmedName, mediaTypes);
    }

    /**
     * Removes a genre from the database.
     */
    @Override
    public void removeGenre(int genreId) {
        if (genreDao.isGenreInUse(genreId)) {
            log.warn("Attempted to delete genre {} that is in use", genreId);
            throw new ItemInUseException("genre", "it is currently assigned to movies, books, or shows");
        }

        log.info("Deleting genre ID: {}", genreId);
        genreDao.deleteGenre(genreId);
    }

    /**
     * Retrieves genres applicable to a specific media type.
     */
    @Override
    public List<Genre> getAvailableGenresFor(String mediaType) {
        return genreDao.getAvailableGenresFor(mediaType);
    }

    /**
     * Retrieves genres associated with a specific movie.
     */
    @Override
    public List<Genre> getGenresForMovie(int movieId) {
        return genreDao.getGenresForMovie(movieId);
    }

    /**
     * Retrieves genres associated with a specific book.
     */
    @Override
    public List<Genre> getGenresForBook(int bookId) {
        return genreDao.getGenresForBook(bookId);
    }

    /**
     * Retrieves genres associated with a specific show.
     */
    @Override
    public List<Genre> getGenresForShow(int showId) {
        return genreDao.getGenresForShow(showId);
    }
}