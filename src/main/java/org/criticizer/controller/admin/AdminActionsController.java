package org.criticizer.controller.admin;

import org.criticizer.entity.Role;
import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for admin actions (change role, delete user).
 * Uses form submissions with flash messages.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminActionsController {
    private static final Logger log = LoggerFactory.getLogger(AdminActionsController.class);

    private final UserService userService;
    private final SecurityUtil securityUtil;

    public AdminActionsController(UserService userService, SecurityUtil securityUtil) {
        this.userService = userService;
        this.securityUtil = securityUtil;
    }

    /**
     * POST /admin/changeRole
     * Changes a user's role
     */
    @PostMapping("/changeRole")
    public String changeRole(
            @RequestParam int userId,
            @RequestParam String newRole,
            RedirectAttributes redirectAttributes) {

        User admin = securityUtil.getCurrentUser();

        try {
            log.info("Admin {} changing role for user ID {} to {}",
                    admin.getName(), userId, newRole);

            Role role = Role.valueOf(newRole.toUpperCase());
            userService.changeUserRole(userId, role, admin);

            redirectAttributes.addFlashAttribute("flashSuccessMessage",
                    "User role updated successfully");

        } catch (IllegalArgumentException e) {
            log.error("Invalid role: {}", newRole);
            redirectAttributes.addFlashAttribute("flashErrorMessage",
                    "Invalid role specified");
        } catch (Exception e) {
            log.error("Error changing user role", e);
            redirectAttributes.addFlashAttribute("flashErrorMessage",
                    "Error changing user role: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    /**
     * POST /admin/deleteUser
     * Deletes a user and all their data
     */
    @PostMapping("/deleteUser")
    public String deleteUser(
            @RequestParam int userId,
            RedirectAttributes redirectAttributes) {

        User admin = securityUtil.getCurrentUser();

        try {
            log.info("Admin {} deleting user ID {}", admin.getName(), userId);

            userService.deleteUser(userId, admin);

            redirectAttributes.addFlashAttribute("flashSuccessMessage",
                    "User deleted successfully");

        } catch (Exception e) {
            log.error("Error deleting user", e);
            redirectAttributes.addFlashAttribute("flashErrorMessage",
                    "Error deleting user: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }
}