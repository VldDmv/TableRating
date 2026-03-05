package org.criticizer.controller.user;

import org.criticizer.dto.helper.ExistsResponse;
import org.criticizer.dto.helper.PageResponse;
import org.criticizer.dto.user.UserPublicResponse;
import org.criticizer.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for public user operations.
 * Enhanced with sorting and statistics.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Gets paginated list of users with optional search, sort, and statistics.
     *
     * GET /api/users?page=1&size=10&search=john&sortBy=totalItems&sortOrder=desc
     *
     * @param page Page number (default: 1)
     * @param size Page size (default: 20)
     * @param search Optional search term
     * @param sortBy Sort field: name, gamesCount, totalItems, etc (default: totalItems)
     * @param sortOrder Sort direction: asc, desc (default: desc)
     * @return PageResponse with list of users and statistics
     */
    @GetMapping
    public ResponseEntity<PageResponse<UserPublicResponse>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "totalItems") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        PageResponse<UserPublicResponse> response = userService.getUsersPageWithStats(
                search, page, size, sortBy, sortOrder);
        return ResponseEntity.ok(response);
    }

    /**
     * Checks if username exists.
     *
     * GET /api/users/{username}/exists
     *
     * @param username Username to check
     * @return ExistsResponse with boolean exists field
     */
    @GetMapping("/{username}/exists")
    public ResponseEntity<ExistsResponse> checkUsernameExists(@PathVariable String username) {
        return ResponseEntity.ok(new ExistsResponse(userService.existsByUsername(username)));
    }
}