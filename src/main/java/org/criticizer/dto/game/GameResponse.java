package org.criticizer.dto.game;

import org.criticizer.dto.tag.TagResponse;
import org.criticizer.entity.Game;
import org.criticizer.entity.MediaStatus;

import java.util.List;

/**
 * Response DTO for a game.
 */
public class GameResponse {

    private final int id;
    private final String name;
    private final String coverUrl;
    private final int score;
    private final boolean completed;
    private final MediaStatus status;
    private final List<TagResponse> tags;

    public GameResponse(int id, String name, String coverUrl, int score,
                        boolean completed, MediaStatus status, List<TagResponse> tags) {
        this.id = id;
        this.name = name;
        this.coverUrl = coverUrl;
        this.score = score;
        this.completed = completed;
        this.status = status == null ? MediaStatus.NONE : status;
        this.tags = tags != null ? List.copyOf(tags) : List.of();
    }

    public static GameResponse from(Game game) {
        return new GameResponse(
                game.getId(),
                game.getName(),
                game.getCoverUrl(),
                game.getScore(),
                game.isCompleted(),
                game.getStatus(),
                game.getTags().stream()
                        .map(tag -> new TagResponse(tag.getId(), tag.getName()))
                        .toList()
        );
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public int getScore() {
        return score;
    }

    public boolean isCompleted() {
        return completed;
    }

    public MediaStatus getStatus() {
        return status;
    }

    public List<TagResponse> getTags() {
        return tags;
    }
}