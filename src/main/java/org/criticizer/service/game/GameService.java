package org.criticizer.service.game;

import java.util.HashSet;
import java.util.List;
import org.criticizer.dto.game.GameResponse;
import org.criticizer.dto.tag.TagResponse;
import org.criticizer.entity.Game;
import org.criticizer.entity.MediaStatus;
import org.criticizer.entity.Tag;
import org.criticizer.repository.GameRepository;
import org.criticizer.repository.TagRepository;
import org.criticizer.service.helper.AbstractMediaService;
import org.criticizer.service.helper.ServiceValidator;
import org.springframework.stereotype.Service;

@Service
public class GameService extends AbstractMediaService<Game, GameResponse> {

    private final TagRepository tagRepository;

    public GameService(
            GameRepository gameRepository,
            TagRepository tagRepository,
            ServiceValidator validator) {
        super(gameRepository, validator);
        this.tagRepository = tagRepository;
    }

    @Override
    protected String getEntityName() {
        return "Game";
    }

    @Override
    protected Game createEntity(String name, String coverUrl, Integer userId, Integer score) {
        Game game = new Game(null, name, userId, score, MediaStatus.PLANNED);
        game.setCoverUrl(coverUrl);
        return game;
    }

    @Override
    protected void assignCategories(Game game, List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            game.setTags(new HashSet<>());
            return;
        }
        List<Tag> tags = tagRepository.findAllById(tagIds);

        game.setTags(new HashSet<>(tags));
    }

    @Override
    protected GameResponse toResponse(Game game) {
        List<TagResponse> tagResponses =
                game.getTags() != null
                        ? game.getTags().stream()
                                .map(tag -> new TagResponse(tag.getId(), tag.getName()))
                                .toList()
                        : List.of();

        return new GameResponse(
                game.getId(),
                game.getName(),
                game.getCoverUrl(),
                game.getScore(),
                game.getStatus().name(),
                tagResponses);
    }

    @Override
    public String getMediaType() {
        return "games";
    }
}
