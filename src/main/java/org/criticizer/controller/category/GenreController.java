package org.criticizer.controller.category;

import jakarta.validation.Valid;
import java.util.List;
import org.criticizer.dto.genre.CreateGenreRequest;
import org.criticizer.dto.genre.GenreResponse;
import org.criticizer.dto.genre.UpdateGenreRequest;
import org.criticizer.dto.helper.ExistsResponse;
import org.criticizer.dto.helper.MessageResponse;
import org.criticizer.service.genre.GenreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/genres")
@PreAuthorize("hasRole('ADMIN')")
public class GenreController {

    private static final Logger log = LoggerFactory.getLogger(GenreController.class);
    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GenreResponse>> getAllGenres() {
        log.debug("GET /api/genres - Fetching all genres");
        List<GenreResponse> genres = genreService.getAllGenres();
        return ResponseEntity.ok(genres);
    }

    @GetMapping("/available/{mediaType}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GenreResponse>> getAvailableGenres(@PathVariable String mediaType) {

        log.debug("GET /api/genres/available/{} - Fetching available genres", mediaType);
        List<GenreResponse> genres = genreService.getAvailableGenresFor(mediaType);
        return ResponseEntity.ok(genres);
    }

    @GetMapping("/movie/{movieId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GenreResponse>> getGenresForMovie(@PathVariable Integer movieId) {

        log.debug("GET /api/genres/movie/{} - Fetching genres", movieId);
        List<GenreResponse> genres = genreService.getGenresForMovie(movieId);
        return ResponseEntity.ok(genres);
    }

    @GetMapping("/book/{bookId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GenreResponse>> getGenresForBook(@PathVariable Integer bookId) {

        log.debug("GET /api/genres/book/{} - Fetching genres", bookId);
        List<GenreResponse> genres = genreService.getGenresForBook(bookId);
        return ResponseEntity.ok(genres);
    }

    @GetMapping("/show/{showId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GenreResponse>> getGenresForShow(@PathVariable Integer showId) {

        log.debug("GET /api/genres/show/{} - Fetching genres", showId);
        List<GenreResponse> genres = genreService.getGenresForShow(showId);
        return ResponseEntity.ok(genres);
    }

    @PostMapping
    public ResponseEntity<GenreResponse> createGenre(
            @Valid @RequestBody CreateGenreRequest request) {

        log.info("POST /api/genres - Creating genre: {}", request.name());
        GenreResponse created = genreService.createGenre(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenreResponse> updateGenre(
            @PathVariable Integer id, @Valid @RequestBody UpdateGenreRequest request) {

        UpdateGenreRequest updatedRequest =
                new UpdateGenreRequest(id, request.name(), request.mediaTypes());

        log.info("PUT /api/genres/{} - Updating genre", id);
        GenreResponse updated = genreService.updateGenre(updatedRequest);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteGenre(@PathVariable Integer id) {
        log.info("DELETE /api/genres/{} - Deleting genre", id);
        genreService.deleteGenre(id);
        return ResponseEntity.ok(new MessageResponse("Genre deleted successfully"));
    }

    @GetMapping("/{id}/in-use")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExistsResponse> checkGenreInUse(@PathVariable Integer id) {

        log.debug("GET /api/genres/{}/in-use - Checking if genre is in use", id);
        boolean inUse = genreService.isGenreInUse(id);
        return ResponseEntity.ok(new ExistsResponse(inUse));
    }
}
