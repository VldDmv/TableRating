package org.criticizer.service.tag;

import java.util.List;
import java.util.stream.Collectors;
import org.criticizer.dto.tag.CreateTagRequest;
import org.criticizer.dto.tag.TagResponse;
import org.criticizer.dto.tag.UpdateTagRequest;
import org.criticizer.entity.Tag;
import org.criticizer.exceptions.data.ItemAlreadyExistsException;
import org.criticizer.exceptions.data.ResourceNotFoundException;
import org.criticizer.exceptions.validation.EmptyNameException;
import org.criticizer.repository.GameRepository;
import org.criticizer.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Spring Service for managing tags. */
@Service
@Transactional(readOnly = true)
public class TagService {

    private static final Logger log = LoggerFactory.getLogger(TagService.class);
    private final TagRepository tagRepository;
    private final GameRepository gameRepository;

    public TagService(TagRepository tagRepository, GameRepository gameRepository) {
        this.tagRepository = tagRepository;
        this.gameRepository = gameRepository;
    }

    /** Get all tags ordered by name. */
    public List<TagResponse> getAllTags() {
        log.debug("Fetching all tags");

        return tagRepository.findAllByOrderByNameAsc().stream().map(TagResponse::from).toList();
    }

    /** Get tags for a specific game. */
    public List<TagResponse> getTagsForGame(Integer gameId) {
        log.debug("Fetching tags for game ID: {}", gameId);
        return tagRepository.findByGameId(gameId).stream()
                .map(TagResponse::from)
                .collect(Collectors.toList());
    }

    /** Create a new tag. */
    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        String trimmedName = validateAndTrimName(request.getName());

        if (tagRepository.existsByNameIgnoreCase(trimmedName)) {
            log.warn("Attempted to create duplicate tag: {}", trimmedName);
            throw new ItemAlreadyExistsException("Tag", trimmedName);
        }

        Tag tag = new Tag(null, trimmedName);
        Tag saved = tagRepository.save(tag);

        log.info("Created tag: {} with ID: {}", saved.getName(), saved.getId());
        return TagResponse.from(saved);
    }

    /** Update an existing tag. */
    @Transactional
    public TagResponse updateTag(UpdateTagRequest request) {
        String trimmedName = validateAndTrimName(request.getName());

        Tag tag =
                tagRepository
                        .findById(request.getId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Tag", "ID: " + request.getId()));

        tagRepository
                .findByNameIgnoreCase(trimmedName)
                .ifPresent(
                        existingTag -> {
                            if (!existingTag.getId().equals(tag.getId())) {
                                throw new ItemAlreadyExistsException("Tag", trimmedName);
                            }
                        });

        tag.setName(trimmedName);
        Tag updated = tagRepository.save(tag);

        log.info("Updated tag ID {} to new name: {}", updated.getId(), updated.getName());
        return TagResponse.from(updated);
    }

    /**
     * Delete a tag and cascade-remove it from all games that use it. Does NOT throw if tag is in
     * use — cleans up instead.
     */
    @Transactional
    public void deleteTag(Integer tagId) {
        Tag tag =
                tagRepository
                        .findById(tagId)
                        .orElseThrow(() -> new ResourceNotFoundException("Tag", "ID: " + tagId));

        gameRepository.removeTagFromAll(tagId);

        tagRepository.deleteById(tagId);
        log.info(
                "Deleted tag '{}' (ID: {}) and removed from all associated games",
                tag.getName(),
                tagId);
    }

    /** Check if a tag is in use by any games. */
    public boolean isTagInUse(Integer tagId) {
        return tagRepository.isTagInUse(tagId);
    }

    // ============= Private Helper Methods =============

    private String validateAndTrimName(String name) {
        if (name == null || name.trim().isEmpty()) {
            log.warn("Attempted to process tag with empty name");
            throw new EmptyNameException("Tag name");
        }
        return name.trim();
    }
}
