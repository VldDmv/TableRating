package org.criticizer.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Show entity with genre associations.
 */
@Entity
@Table(name = "shows", indexes = {
        @Index(name = "idx_shows_user_name", columnList = "user_id, name")
})
public class Show extends BaseEntity {

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "show_genres",
            joinColumns = @JoinColumn(name = "show_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    protected Show() {
    }

    public Show(Integer id, String name, Integer userId, Integer score, boolean completed) {
        super(id, name, userId, score, completed);
    }

    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
    }
}