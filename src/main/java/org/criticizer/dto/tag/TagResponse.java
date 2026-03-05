package org.criticizer.dto.tag;

import org.criticizer.entity.Tag;

/**
 * Response DTO for a tag.
 */
public class TagResponse {

    private final Integer id;
    private final String name;

    public TagResponse(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}