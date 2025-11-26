package org.criticizer.service.tag;

import org.criticizer.entity.Tag;

import java.util.List;

public interface TagService {
    void createTag(String tagName);

    void editTag(int tagId, String newTagName);

    void removeTag(int tagId);

    List<Tag> getAllTags();

    List<Tag> getTagsForGame(int gameId);
}