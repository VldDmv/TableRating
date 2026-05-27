package org.criticizer.entity;

import jakarta.persistence.*;
import org.criticizer.service.helper.MediaEntity;

/**
 * Base entity for all media types (Game, Movie, Book, Show).
 * Contains all common fields to eliminate duplication.
 */
@MappedSuperclass
public abstract class BaseEntity implements MediaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    protected BaseEntity() {
    }

    protected BaseEntity(Integer id, String name, Integer userId, Integer score, boolean completed) {
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.score = score;
        this.completed = completed;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @Override
    public Integer getScore() {
        return score;
    }

    @Override
    public void setScore(Integer score) {
        this.score = score;
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    @Override
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public String getCoverUrl() {
        return coverUrl;
    }

    @Override
    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }
}