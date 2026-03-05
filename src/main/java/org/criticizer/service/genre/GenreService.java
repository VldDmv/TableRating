package org.criticizer.service.genre;

import org.criticizer.dto.genre.CreateGenreRequest;
import org.criticizer.dto.genre.GenreResponse;
import org.criticizer.dto.genre.UpdateGenreRequest;
import org.criticizer.entity.Genre;
import org.criticizer.exceptions.data.ItemAlreadyExistsException;
import org.criticizer.exceptions.data.ResourceNotFoundException;
import org.criticizer.exceptions.validation.EmptyNameException;
import org.criticizer.exceptions.validation.InvalidInputException;
import org.criticizer.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing genres.
 * Contains all business logic related to genres.
 */
@Service
@Transactional(readOnly = true)
public class GenreService {

    private static final Logger log = LoggerFactory.getLogger(GenreService.class);
    private static final List<String> VALID_MEDIA_TYPES =
            Arrays.asList("movie", "book", "show", "shared");

    private final GenreRepository genreRepository;
    private final GenreApplicabilityRepository applicabilityRepository;
    private final MovieRepository movieRepository;
    private final BookRepository bookRepository;
    private final ShowRepository showRepository;

    public GenreService(
            GenreRepository genreRepository,
            GenreApplicabilityRepository applicabilityRepository,
            MovieRepository movieRepository,
            BookRepository bookRepository,
            ShowRepository showRepository
    ) {
        this.genreRepository = genreRepository;
        this.applicabilityRepository = applicabilityRepository;
        this.movieRepository = movieRepository;
        this.bookRepository = bookRepository;
        this.showRepository = showRepository;
    }

    /**
     * Get all genres with their media types.
     * Uses batch loading to avoid N+1 queries.
     */
    public List<GenreResponse> getAllGenres() {
        log.debug("Fetching all genres");
        List<Genre> genres = genreRepository.findAllByOrderByNameAsc();
        return buildResponsesWithBatchedMediaTypes(genres);
    }

    /**
     * Get genres available for a specific media type.
     * Uses batch loading to avoid N+1 queries.
     */
    public List<GenreResponse> getAvailableGenresFor(String mediaType) {
        validateMediaType(mediaType);
        log.debug("Fetching available genres for media type: {}", mediaType);
        List<Genre> genres = genreRepository.findAvailableGenresFor(mediaType);
        return buildResponsesWithBatchedMediaTypes(genres);
    }

    public List<GenreResponse> getGenresForMovie(Integer movieId) {
        log.debug("Fetching genres for movie ID: {}", movieId);
        return genreRepository.findByMovieId(movieId)
                .stream()
                .map(this::toSimpleResponse)
                .collect(Collectors.toList());
    }

    public List<GenreResponse> getGenresForBook(Integer bookId) {
        log.debug("Fetching genres for book ID: {}", bookId);
        return genreRepository.findByBookId(bookId)
                .stream()
                .map(this::toSimpleResponse)
                .collect(Collectors.toList());
    }

    public List<GenreResponse> getGenresForShow(Integer showId) {
        log.debug("Fetching genres for show ID: {}", showId);
        return genreRepository.findByShowId(showId)
                .stream()
                .map(this::toSimpleResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public GenreResponse createGenre(CreateGenreRequest request) {
        String trimmedName = validateAndTrimName(request.getName());
        List<String> validatedMediaTypes = validateMediaTypes(request.getMediaTypes());

        if (genreRepository.existsByNameIgnoreCase(trimmedName)) {
            log.warn("Attempted to create duplicate genre: {}", trimmedName);
            throw new ItemAlreadyExistsException("Genre", trimmedName);
        }

        Genre genre = new Genre(null, trimmedName);
        Genre saved = genreRepository.save(genre);

        saveMediaTypeApplicability(saved.getId(), validatedMediaTypes);

        log.info("Created genre: {} with ID: {} for media types: {}",
                saved.getName(), saved.getId(), validatedMediaTypes);

        return toResponseWithMediaTypes(saved);
    }

    @Transactional
    public GenreResponse updateGenre(UpdateGenreRequest request) {
        String trimmedName = validateAndTrimName(request.getName());
        List<String> validatedMediaTypes = validateMediaTypes(request.getMediaTypes());

        Genre genre = genreRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Genre", "ID: " + request.getId()));

        genreRepository.findByNameIgnoreCase(trimmedName)
                .ifPresent(existingGenre -> {
                    if (!existingGenre.getId().equals(genre.getId())) {
                        throw new ItemAlreadyExistsException("Genre", trimmedName);
                    }
                });

        List<String> oldMediaTypes = applicabilityRepository.findMediaTypesByGenreId(genre.getId());

        genre.setName(trimmedName);
        Genre updated = genreRepository.save(genre);

        applicabilityRepository.deleteByGenreId(updated.getId());
        saveMediaTypeApplicability(updated.getId(), validatedMediaTypes);

        removeGenreFromRemovedMediaTypes(genre, oldMediaTypes, validatedMediaTypes);

        log.info("Updated genre ID {} to name: {} with media types: {}",
                updated.getId(), updated.getName(), validatedMediaTypes);

        return toResponseWithMediaTypes(updated);
    }

    /**
     * Delete a genre and cascade-remove it from all associated media items.
     */
    @Transactional
    public void deleteGenre(Integer genreId) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new ResourceNotFoundException("Genre", "ID: " + genreId));

        removeGenreFromAllItems(genre);

        applicabilityRepository.deleteByGenreId(genreId);
        genreRepository.deleteById(genreId);

        log.info("Deleted genre with ID: {} and removed from all associated media", genreId);
    }

    /**
     * Check if a genre is in use by any media.
     * Uses early exit to avoid unnecessary queries.
     */
    public boolean isGenreInUse(Integer genreId) {
        if (genreRepository.countMoviesWithGenre(genreId) > 0) return true;
        if (genreRepository.countBooksWithGenre(genreId) > 0) return true;
        return genreRepository.countShowsWithGenre(genreId) > 0;
    }

    // ============= Private Helper Methods =============

    /**
     * Builds GenreResponse list using a single batch query for media types.
     *
     * @param genres List of genres to build responses for
     * @return List of GenreResponse with media types populated
     */
    private List<GenreResponse> buildResponsesWithBatchedMediaTypes(List<Genre> genres) {
        if (genres.isEmpty()) {
            return List.of();
        }

        List<Integer> genreIds = genres.stream()
                .map(Genre::getId)
                .collect(Collectors.toList());

        Map<Integer, List<String>> mediaTypesByGenreId = applicabilityRepository
                .findMediaTypesByGenreIds(genreIds)
                .stream()
                .collect(Collectors.groupingBy(
                        row -> (Integer) row[0],
                        Collectors.mapping(row -> (String) row[1], Collectors.toList())
                ));

        return genres.stream()
                .map(genre -> GenreResponse.from(
                        genre,
                        mediaTypesByGenreId.getOrDefault(genre.getId(), List.of())
                ))
                .collect(Collectors.toList());
    }

    private void removeGenreFromAllItems(Genre genre) {
        movieRepository.removeGenreFromAll(genre.getId());
        bookRepository.removeGenreFromAll(genre.getId());
        showRepository.removeGenreFromAll(genre.getId());

        log.debug("Removed genre '{}' from all associated media items", genre.getName());
    }

    private void removeGenreFromRemovedMediaTypes(Genre genre,
                                                  List<String> oldTypes,
                                                  List<String> newTypes) {
        boolean wasShared = oldTypes.contains("shared");
        boolean isShared = newTypes.contains("shared");

        boolean wasMovie = wasShared || oldTypes.contains("movie");
        boolean wasBook = wasShared || oldTypes.contains("book");
        boolean wasShow = wasShared || oldTypes.contains("show");

        boolean isMovie = isShared || newTypes.contains("movie");
        boolean isBook = isShared || newTypes.contains("book");
        boolean isShow = isShared || newTypes.contains("show");

        if (wasMovie && !isMovie) {
            movieRepository.removeGenreFromAll(genre.getId());
            log.debug("Removed genre '{}' from all movies (no longer applicable)", genre.getName());
        }

        if (wasBook && !isBook) {
            bookRepository.removeGenreFromAll(genre.getId());
            log.debug("Removed genre '{}' from all books (no longer applicable)", genre.getName());
        }

        if (wasShow && !isShow) {
            showRepository.removeGenreFromAll(genre.getId());
            log.debug("Removed genre '{}' from all shows (no longer applicable)", genre.getName());
        }
    }

    private String validateAndTrimName(String name) {
        if (name == null || name.trim().isEmpty()) {
            log.warn("Attempted to process genre with empty name");
            throw new EmptyNameException("Genre name");
        }
        return name.trim();
    }

    private List<String> validateMediaTypes(List<String> mediaTypes) {
        if (mediaTypes == null || mediaTypes.isEmpty()) {
            log.debug("No media types specified, defaulting to 'shared'");
            return List.of("shared");
        }

        for (String mediaType : mediaTypes) {
            if (!VALID_MEDIA_TYPES.contains(mediaType.toLowerCase())) {
                throw new InvalidInputException("mediaType",
                        "must be one of: " + String.join(", ", VALID_MEDIA_TYPES));
            }
        }

        return mediaTypes.stream()
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
    }

    private void validateMediaType(String mediaType) {
        if (mediaType == null || !VALID_MEDIA_TYPES.contains(mediaType.toLowerCase())) {
            throw new InvalidInputException("mediaType",
                    "must be one of: " + String.join(", ", VALID_MEDIA_TYPES));
        }
    }

    private void saveMediaTypeApplicability(Integer genreId, List<String> mediaTypes) {
        mediaTypes.forEach(mediaType ->
                applicabilityRepository.insertApplicability(genreId, mediaType));
    }

    // Used only for single-genre responses (create/update result)
    private GenreResponse toResponseWithMediaTypes(Genre genre) {
        List<String> mediaTypes = applicabilityRepository
                .findMediaTypesByGenreId(genre.getId());
        return GenreResponse.from(genre, mediaTypes);
    }

    private GenreResponse toSimpleResponse(Genre genre) {
        return GenreResponse.from(genre);
    }
}