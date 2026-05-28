package org.criticizer.service.helper;

import java.util.Map;
import org.criticizer.constants.ContentCategory;
import org.criticizer.service.book.BookService;
import org.criticizer.service.game.GameService;
import org.criticizer.service.movie.MovieService;
import org.criticizer.service.show.ShowService;
import org.springframework.stereotype.Service;

/**
 * Resolves a content category string to its corresponding media service. Implements the Strategy
 * pattern for media type dispatch.
 */
@Service
public class MediaTypeResolver {

    private final Map<ContentCategory, AbstractMediaService<?, ?>> services;

    public MediaTypeResolver(
            GameService gameService,
            MovieService movieService,
            BookService bookService,
            ShowService showService) {
        this.services =
                Map.of(
                        ContentCategory.GAMES, gameService,
                        ContentCategory.MOVIES, movieService,
                        ContentCategory.BOOKS, bookService,
                        ContentCategory.SHOWS, showService);
    }

    /**
     * Resolves a category string (e.g. "games") to its service.
     *
     * @param typeString category string from request
     * @return corresponding AbstractMediaService
     * @throws IllegalArgumentException if type string is not recognised
     */
    public AbstractMediaService<?, ?> resolve(String typeString) {
        ContentCategory type = ContentCategory.fromString(typeString);
        return resolve(type);
    }

    /**
     * Resolves a ContentCategory enum value to its service.
     *
     * @param type ContentCategory enum value
     * @return corresponding AbstractMediaService
     */
    public AbstractMediaService<?, ?> resolve(ContentCategory type) {
        AbstractMediaService<?, ?> service = services.get(type);
        if (service == null) {
            throw new IllegalStateException("No service registered for category: " + type);
        }
        return service;
    }

    public boolean isSupported(String typeString) {
        return ContentCategory.isValid(typeString);
    }
}
