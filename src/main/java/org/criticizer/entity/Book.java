package org.criticizer.entity;

import java.util.List;

//Represents a book entity in the system, associated with a user and genres
public class Book {
    private final int id;
    private final String name;
    private final int userId;
    private final int score;
    private final boolean completed;
    private List<Genre> genres;

    //Constructs a new Book with the specified attributes
    public Book(int id, String name, int userId, int score, boolean completed) {
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.score = score;
        this.completed = completed;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getUserId() {
        return userId;
    }

    public int getScore() {
        return score;
    }

    public boolean isCompleted() {
        return completed;
    }

    public List<Genre> getGenres() {
        return genres;
    }

    public void setGenres(List<Genre> genres) {
        this.genres = genres;
    }
}

