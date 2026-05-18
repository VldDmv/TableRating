package org.criticizer.service.show;

import org.criticizer.dto.genre.GenreResponse;
import org.criticizer.dto.show.ShowResponse;
import org.criticizer.entity.Genre;
import org.criticizer.entity.Show;
import org.criticizer.repository.GenreRepository;
import org.criticizer.repository.ShowRepository;
import org.criticizer.service.helper.ServiceValidator;
import org.criticizer.service.helper.AbstractMediaService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
public class ShowService extends AbstractMediaService<Show, ShowResponse> {

    private final GenreRepository genreRepository;

    public ShowService(ShowRepository showRepository,
                       GenreRepository genreRepository,
                       ServiceValidator validator) {
        super(showRepository, validator);
        this.genreRepository = genreRepository;
    }

    @Override
    protected String getEntityName() {
        return "Show";
    }

    @Override
    protected Show createEntity(String name, String coverUrl, Integer userId, Integer score) {
        Show show = new Show(null, name, userId, score, false);
        show.setCoverUrl(coverUrl);
        return show;
    }

    @Override
    protected void assignCategories(Show show, List<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            show.setGenres(new HashSet<>());
            return;
        }
        List<Genre> genres = genreRepository.findAllById(genreIds);

        show.setGenres(new HashSet<>(genres));
    }

    @Override
    protected ShowResponse toResponse(Show show) {
        List<GenreResponse> genreResponses = show.getGenres() != null
                ? show.getGenres().stream()
                .map(GenreResponse::from)
                .toList()
                : List.of();

        return new ShowResponse(
                show.getId(),
                show.getName(),
                show.getCoverUrl(),
                show.getScore(),
                show.isCompleted(),
                show.getStatus(),
                genreResponses
        );
    }
    @Override
    public String getMediaType() {
        return "shows";
    }
}