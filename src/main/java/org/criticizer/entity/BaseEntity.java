package org.criticizer.entity;

import jakarta.persistence.*;
import org.criticizer.service.helper.MediaEntity;

/**
 * Base entity for all media types (Game, Movie, Book, Show). Contains all common fields to
 * eliminate duplication.
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MediaStatus status = MediaStatus.PLANNED;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    protected BaseEntity() {}

    protected BaseEntity(
            Integer id, String name, Integer userId, Integer score, MediaStatus status) {
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.score = score;
        this.status = status == null ? MediaStatus.PLANNED : status;
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
    public MediaStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(MediaStatus status) {
        this.status = status == null ? MediaStatus.PLANNED : status;
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
