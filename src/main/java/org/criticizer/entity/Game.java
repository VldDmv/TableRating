package org.criticizer.entity;

import java.util.List;

//Represents a game entity in the system, associated with a user and tags
public class Game {
    private final int id;
    private final String name;
    private final int userId;
    private final int score;
    private final boolean completed;
    private List<Tag> tags;

    //Constructs a new Game with the specified attributes
    public Game(int id, String name, int userId, int score, boolean completed) {
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

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }
}