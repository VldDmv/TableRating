package org.criticizer.controller.admin;

import jakarta.validation.Valid;
import org.criticizer.dto.admin.ChangeRoleRequest;
import org.criticizer.dto.admin.UserAdminResponse;
import org.criticizer.dto.helper.MessageResponse;
import org.criticizer.dto.helper.PageResponse;
import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.dto.admin.AdminStats;
import org.criticizer.service.dashboard.DashboardService;
import org.criticizer.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Admin operations.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final UserService userService;
    private final DashboardService dashboardService;
    private final SecurityUtil securityUtil;

    public AdminController(UserService userService,
                           DashboardService dashboardService,
                           SecurityUtil securityUtil) {
        this.userService = userService;
        this.dashboardService = dashboardService;
        this.securityUtil = securityUtil;
    }

    /**
     * GET /api/admin/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminStats> getStatistics() {
        log.debug("Admin requesting dashboard statistics");
        return ResponseEntity.ok(dashboardService.getAdminDashboardStats());
    }

    /**
     * GET /api/admin/users
     */
    @GetMapping("/users")
    public ResponseEntity<PageResponse<UserAdminResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        log.debug("Admin requesting users list - page: {}, size: {}, search: {}",
                page, size, search);

        PageResponse<User> result = userService.getUsersPage(
                search,
                page + 1,
                size,
                false
        );

        List<UserAdminResponse> userResponses = result.getItems().stream()
                .map(UserAdminResponse::from)
                .toList();

        Page<UserAdminResponse> pageResult = new PageImpl<>(
                userResponses,
                PageRequest.of(page, size),
                result.getTotalItems()
        );

        return ResponseEntity.ok(PageResponse.of(pageResult));
    }

    /**
     * PUT /api/admin/users/{userId}/role
     */
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<MessageResponse> changeUserRole(
            @PathVariable int userId,
            @Valid @RequestBody ChangeRoleRequest request) {

        User admin = securityUtil.getCurrentUser();
        log.info("Admin {} changing role for user ID {} to {}",
                admin.getName(), userId, request.role());

        userService.changeUserRole(userId, request.role(), admin);

        return ResponseEntity.ok(new MessageResponse("User role updated successfully"));
    }

    /**
     * DELETE /api/admin/users/{userId}
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable int userId) {
        User admin = securityUtil.getCurrentUser();
        log.info("Admin {} deleting user ID {}", admin.getName(), userId);

        userService.deleteUser(userId, admin);

        return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
    }

    /**
     * GET /api/admin/users/{userId}
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserAdminResponse> getUserDetails(@PathVariable int userId) {
        log.debug("Admin requesting details for user ID {}", userId);

        User user = userService.getUserById(userId);

        return ResponseEntity.ok(UserAdminResponse.from(user));
    }
}