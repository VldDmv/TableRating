package org.criticizer.service.tag;

import org.criticizer.dao.tag.TagDao;
import org.criticizer.entity.Tag;
import org.criticizer.exceptions.data.ItemInUseException;
import org.criticizer.exceptions.validation.EmptyNameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Implementation of the TagService interface for managing tag-related operations.
 */
public class TagServiceImpl implements TagService {
    private static final Logger log = LoggerFactory.getLogger(TagServiceImpl.class);
    private final TagDao tagDao;

    public TagServiceImpl(TagDao tagDao) {
        this.tagDao = tagDao;
    }

    /**
     * Creates a new tag with the specified name.
     * Throws EmptyNameException if tag name is null or empty.
     */
    @Override
    public void createTag(String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            log.warn("Attempted to create tag with empty name");
            throw new EmptyNameException("Tag name");
        }

        String trimmedName = tagName.trim();
        log.info("Creating tag: {}", trimmedName);
        tagDao.addTag(trimmedName);
    }

    /**
     * Updates an existing tag's name.
     * Throws EmptyNameException if new tag name is null or empty.
     */
    @Override
    public void editTag(int tagId, String newTagName) {
        if (newTagName == null || newTagName.trim().isEmpty()) {
            log.warn("Attempted to update tag {} with empty name", tagId);
            throw new EmptyNameException("Tag name");
        }

        String trimmedName = newTagName.trim();
        log.info("Updating tag ID {}: {}", tagId, trimmedName);
        tagDao.updateTag(tagId, trimmedName);
    }

    /**
     * Removes a tag from the database.
     * Throws ItemInUseException if tag is currently assigned to games.
     */
    @Override
    public void removeTag(int tagId) {
        if (tagDao.isTagInUse(tagId)) {
            log.warn("Attempted to delete tag {} that is in use", tagId);
            throw new ItemInUseException("tag", "it is currently assigned to one or more games");
        }

        log.info("Deleting tag ID: {}", tagId);
        tagDao.deleteTag(tagId);
    }

    /**
     * Retrieves all tags from the database.
     */
    @Override
    public List<Tag> getAllTags() {
        return tagDao.getAllTags();
    }

    /**
     * Retrieves tags associated with a specific game.
     */
    @Override
    public List<Tag> getTagsForGame(int gameId) {
        return tagDao.getTagsForGame(gameId);
    }
}