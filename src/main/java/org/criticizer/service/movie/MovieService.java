package org.criticizer.service.movie;

import java.util.HashSet;
import java.util.List;
import org.criticizer.dto.genre.GenreResponse;
import org.criticizer.dto.movie.MovieResponse;
import org.criticizer.entity.Genre;
import org.criticizer.entity.Movie;
import org.criticizer.repository.GenreRepository;
import org.criticizer.repository.MovieRepository;
import org.criticizer.service.helper.AbstractMediaService;
import org.criticizer.service.helper.ServiceValidator;
import org.springframework.stereotype.Service;

@Service
public class MovieService extends AbstractMediaService<Movie, MovieResponse> {

    private final GenreRepository genreRepository;

    public MovieService(
            MovieRepository movieRepository,
            GenreRepository genreRepository,
            ServiceValidator validator) {
        super(movieRepository, validator);
        this.genreRepository = genreRepository;
    }

    @Override
    protected String getEntityName() {
        return "Movie";
    }

    @Override
    protected Movie createEntity(String name, String coverUrl, Integer userId, Integer score) {
        Movie movie = new Movie(null, name, userId, score, false);
        movie.setCoverUrl(coverUrl);
        return movie;
    }

    @Override
    protected void assignCategories(Movie movie, List<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            movie.setGenres(new HashSet<>());
            return;
        }
        List<Genre> genres = genreRepository.findAllById(genreIds);

        movie.setGenres(new HashSet<>(genres));
    }

    @Override
    protected MovieResponse toResponse(Movie movie) {
        List<GenreResponse> genreResponses =
                movie.getGenres() != null
                        ? movie.getGenres().stream().map(GenreResponse::from).toList()
                        : List.of();

        return new MovieResponse(
                movie.getId(),
                movie.getName(),
                movie.getCoverUrl(),
                movie.getScore(),
                movie.isCompleted(),
                genreResponses);
    }

    @Override
    public String getMediaType() {
        return "movies";
    }
}
