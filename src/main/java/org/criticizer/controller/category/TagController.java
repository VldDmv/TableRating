package org.criticizer.controller.category;

import jakarta.validation.Valid;
import org.criticizer.dto.helper.ExistsResponse;
import org.criticizer.dto.helper.MessageResponse;
import org.criticizer.dto.tag.CreateTagRequest;
import org.criticizer.dto.tag.TagResponse;
import org.criticizer.dto.tag.UpdateTagRequest;
import org.criticizer.service.tag.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/tags")
@PreAuthorize("hasRole('ADMIN')")
public class TagController {

    private static final Logger log = LoggerFactory.getLogger(TagController.class);
    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TagResponse>> getAllTags() {
        log.debug("GET /api/tags - Fetching all tags");
        List<TagResponse> tags = tagService.getAllTags();
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/game/{gameId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TagResponse>> getTagsForGame(@PathVariable Integer gameId) {
        log.debug("GET /api/tags/game/{} - Fetching tags for game", gameId);
        List<TagResponse> tags = tagService.getTagsForGame(gameId);
        return ResponseEntity.ok(tags);
    }

    @PostMapping
    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody CreateTagRequest request) {
        log.info("POST /api/tags - Creating tag: {}", request.name());
        TagResponse created = tagService.createTag(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TagResponse> updateTag(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateTagRequest request) {

        UpdateTagRequest updatedRequest = new UpdateTagRequest(id, request.name());

        log.info("PUT /api/tags/{} - Updating tag", id);
        TagResponse updated = tagService.updateTag(updatedRequest);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteTag(@PathVariable Integer id) {
        log.info("DELETE /api/tags/{} - Deleting tag", id);
        tagService.deleteTag(id);
        return ResponseEntity.ok(new MessageResponse("Tag deleted successfully"));
    }

    @GetMapping("/{id}/in-use")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExistsResponse> checkTagInUse(@PathVariable Integer id) {
        log.debug("GET /api/tags/{}/in-use - Checking if tag is in use", id);
        boolean inUse = tagService.isTagInUse(id);
        return ResponseEntity.ok(new ExistsResponse(inUse));
    }
}