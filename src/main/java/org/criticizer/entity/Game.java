package org.criticizer.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/** Game entity with tag associations. */
@Entity
@Table(
        name = "games",
        indexes = {@Index(name = "idx_games_user_name", columnList = "user_id, name")})
public class Game extends BaseEntity {

    @ManyToMany(
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "game_tags",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    protected Game() {}

    public Game(Integer id, String name, Integer userId, Integer score, boolean completed) {
        super(id, name, userId, score, completed);
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }
}
