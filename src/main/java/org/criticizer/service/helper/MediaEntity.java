
package org.criticizer.service.helper;

/**
 * Marker interface for all media entities.
 * Enforces common contract for Game, Movie, Book, Show.
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

    boolean isCompleted();

    void setCompleted(boolean completed);
}