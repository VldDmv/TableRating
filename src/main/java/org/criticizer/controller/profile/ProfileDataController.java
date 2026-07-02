package org.criticizer.controller.profile;

import org.criticizer.dto.helper.PageResponse;
import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.helper.AbstractMediaService;
import org.criticizer.service.helper.MediaTypeResolver;
import org.criticizer.service.profile.ProfileAccessService;
import org.criticizer.service.profile.ProfileStatsService;
import org.criticizer.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST API for fetching profile data (games, movies, books, shows).
 * Supports pagination and filtering.
 */
@RestController
public class ProfileDataController {
    private static final Logger log = LoggerFactory.getLogger(ProfileDataController.class);

    private final UserService userService;
    private final SecurityUtil securityUtil;
    private final ProfileAccessService accessService;
    private final MediaTypeResolver mediaTypeResolver;
    private final ProfileStatsService statsService;

    public ProfileDataController(
            UserService userService,
            SecurityUtil securityUtil,
            ProfileAccessService accessService,
            MediaTypeResolver mediaTypeResolver,
            ProfileStatsService statsService
    ) {
        this.userService = userService;
        this.securityUtil = securityUtil;
        this.accessService = accessService;
        this.mediaTypeResolver = mediaTypeResolver;
        this.statsService = statsService;
    }

    /**
     * GET /profile-stats
     * Per-category rating statistics for a user's profile.
     */
    @GetMapping("/profile-stats")
    public ResponseEntity<?> getProfileStats(
            @RequestParam String username,
            @RequestParam String category) {

        try {
            User profileOwner = userService.getUser(username);
            String currentUsername = securityUtil.getCurrentUsername();

            if (!accessService.canViewProfile(profileOwner, currentUsername)) {
                return ResponseEntity.status(403).body(Map.of("error", "Profile is private"));
            }

            return ResponseEntity.ok(statsService.getStats(category, profileOwner.getId()));
        } catch (Exception e) {
            log.error("Error fetching profile stats for {} / {}", username, category, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch profile stats"));
        }
    }

    /**
     * GET /profile-data
     * Fetch paginated media data for a user's profile.
     *
     * @param username Profile owner's username
     * @param category Media type (games, movies, books, shows)
     * @param page Page number (1-based)
     * @param pageSize Items per page
     * @param sortBy Sort field (name, score, completed)
     * @param sortOrder Sort direction (asc, desc)
     * @param search Optional search term
     * @param tag_id Optional tag ID (for games)
     * @param genre_id Optional genre ID (for movies/books/shows)
     * @return Paginated response with media items
     */
    @GetMapping("/profile-data")
    public ResponseEntity<?> getProfileData(
            @RequestParam String username,
            @RequestParam String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int pageSize,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer tag_id,
            @RequestParam(required = false) Integer genre_id) {

        log.debug("📥 Fetching profile data: username={}, category={}, page={}, pageSize={}, search={}, tag_id={}, genre_id={}",
                username, category, page, pageSize, search, tag_id, genre_id);

        try {
            // Validate user exists
            User profileOwner = userService.getUser(username);
            log.debug("User found: id={}, name={}", profileOwner.getId(), profileOwner.getName());

            // Check access permissions
            String currentUsername = securityUtil.getCurrentUsername();
            if (!accessService.canViewProfile(profileOwner, currentUsername)) {
                log.warn("🚫 Access denied to private profile: {}", username);
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Profile is private"));
            }
            log.debug("Access granted for user: {}", currentUsername);

            // Resolve media service
            AbstractMediaService<?, ?> service;
            try {
                service = mediaTypeResolver.resolve(category);
                log.debug("Media service resolved for category: {}", category);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid media type requested: {}", category);
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Invalid category: " + category,
                                "validCategories", List.of("games", "movies", "books", "shows")
                        ));
            }

            Integer filterId = "games".equals(category) ? tag_id : genre_id;

            PageResponse<?> result = service.getUserItemsPageAsDto(
                    profileOwner.getId(),
                    page,
                    pageSize,
                    filterId,
                    search,
                    sortBy,
                    sortOrder
            );

            log.debug("Fetched {} items for user {} (category: {}, page: {}/{})",
                    result.getItems().size(), username, category,
                    result.getCurrentPage(), result.getTotalPages());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error fetching profile data for user: {}, category: {}",
                    username, category, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch profile data"));
        }
    }
}