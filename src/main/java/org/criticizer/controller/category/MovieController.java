package org.criticizer.controller.category;

import org.criticizer.controller.helper.AbstractMediaController;
import org.criticizer.dto.movie.CreateMovieRequest;
import org.criticizer.dto.movie.MovieResponse;
import org.criticizer.dto.movie.UpdateMovieRequest;
import org.criticizer.entity.Movie;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.movie.MovieService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movies")
public class MovieController
        extends AbstractMediaController<
                Movie, MovieResponse, CreateMovieRequest, UpdateMovieRequest> {

    public MovieController(MovieService movieService, SecurityUtil securityUtil) {
        super(movieService, securityUtil);
    }

    @Override
    protected String getEntityName() {
        return "Movie";
    }

    @Override
    protected MovieResponse convertToResponse(Movie entity) {
        return MovieResponse.from(entity);
    }
}
