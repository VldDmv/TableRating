package org.criticizer.controller.category;

import org.criticizer.constants.ContentCategory;
import org.criticizer.dto.helper.PageResponse;
import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.helper.AbstractMediaService;
import org.criticizer.service.helper.MediaTypeResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * REST API for AJAX data requests for category pages.
 * Single endpoint handles all four media types via MediaTypeResolver.
 */
@RestController
@RequestMapping("/api/category")
public class CategoryDataController {

    private static final Logger log = LoggerFactory.getLogger(CategoryDataController.class);

    private final MediaTypeResolver mediaTypeResolver;
    private final SecurityUtil securityUtil;

    public CategoryDataController(MediaTypeResolver mediaTypeResolver, SecurityUtil securityUtil) {
        this.mediaTypeResolver = mediaTypeResolver;
        this.securityUtil = securityUtil;
    }

    /**
     * GET /api/category/{type} — games, movies, books, shows.
     * <p>
     * Games use categoryId as tag ID; movies/books/shows use it as genre ID..
     */
    @GetMapping("/{type:games|movies|books|shows}")
    public ResponseEntity<PageResponse<?>> getCategoryData(
            @PathVariable String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int rows,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(required = false) Integer maxScore,
            @RequestParam(required = false) Boolean completed) {

        User currentUser = securityUtil.getCurrentUser();
        ContentCategory category = ContentCategory.fromString(type);

        log.debug("AJAX request for {} - page: {}, rows: {}, user: {}",
                type, page, rows, currentUser.getName());

        AbstractMediaService<?, ?> service = mediaTypeResolver.resolve(category);

        PageResponse<?> result = service.getUserItemsPageAsDto(
                currentUser.getId(), page, rows, categoryId, search, sortBy, sortOrder,
                minScore, maxScore, completed);

        return ResponseEntity.ok(result);
    }
}