package org.criticizer.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Book entity with genre associations.
 */
@Entity
@Table(name = "books", indexes = {
        @Index(name = "idx_books_user_name", columnList = "user_id, name")
})
public class Book extends BaseEntity {

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "book_genres",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    protected Book() {
    }

    public Book(Integer id, String name, Integer userId, Integer score, boolean completed) {
        super(id, name, userId, score, completed);
    }

    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
    }
}