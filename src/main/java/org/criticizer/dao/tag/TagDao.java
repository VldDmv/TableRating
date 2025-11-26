package org.criticizer.dao.tag;

import org.criticizer.entity.Tag;

import java.util.List;

public interface TagDao {
    List<Tag> getAllTags();

    List<Tag> getTagsForGame(int gameId);

    void addTag(String tagName);

    void updateTag(int tagId, String newTagName);

    void deleteTag(int tagId);

    boolean isTagInUse(int tagId);
}
