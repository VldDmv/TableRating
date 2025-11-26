package org.criticizer.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//Represents a genre entity associated with media types such as movies, books, or shows.
public class Genre {
    private int id;
    private String name;
    private List<String> mediaTypes = new ArrayList<>();

    //Constructs a new Genre with the specified ID and name
    public Genre(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getMediaTypes() {
        return mediaTypes;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMediaTypes(List<String> mediaTypes) {
        this.mediaTypes = mediaTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Genre genre = (Genre) o;
        return id == genre.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Genre{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", mediaType='" + mediaTypes + '\'' +
                '}';
    }
}