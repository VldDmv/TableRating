package org.criticizer.service.helper;

/**
 * Marker interface for all media entities. Enforces common contract for Game, Movie, Book, Show.
 */
public interface MediaEntity {
    Integer getId();

    String getName();

    void setName(String name);

    String getCoverUrl();

    void setCoverUrl(String coverUrl);

    Integer getUserId();

    Integer getScore();

    void setScore(Integer score);

    org.criticizer.entity.MediaStatus getStatus();

    void setStatus(org.criticizer.entity.MediaStatus status);

    /** Convenience for stats: an item counts as completed only in the COMPLETED status. */
    default boolean isCompleted() {
        return getStatus() == org.criticizer.entity.MediaStatus.COMPLETED;
    }
}
