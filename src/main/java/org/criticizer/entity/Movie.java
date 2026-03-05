package org.criticizer.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Movie entity with genre associations.
 */
@Entity
@Table(name = "movies", indexes = {
        @Index(name = "idx_movies_user_name", columnList = "user_id, name")
})
public class Movie extends BaseEntity {

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "movie_genres",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    protected Movie() {
    }

    public Movie(Integer id, String name, Integer userId, Integer score, boolean completed) {
        super(id, name, userId, score, completed);
    }

    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
    }
}