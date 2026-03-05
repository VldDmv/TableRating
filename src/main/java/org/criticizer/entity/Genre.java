package org.criticizer.entity;

import jakarta.persistence.*;

import java.util.Objects;

/**
 * Genre entity.
 * Represents a genre that can be associated with movies, books, or shows.
 * <p>
 * Media type associations are stored in genre_applicability table.
 */
@Entity
@Table(name = "genres")
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    protected Genre() {
        // JPA requires protected no-arg constructor
    }

    /**
     * Constructs a new Genre with the specified ID and name.
     */
    public Genre(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Genre genre = (Genre) o;
        return Objects.equals(id, genre.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Genre{id=" + id + ", name='" + name + "'}";
    }
}