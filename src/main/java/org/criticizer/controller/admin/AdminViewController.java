
package org.criticizer.controller.admin;

import org.criticizer.dto.helper.PageResponse;
import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.dashboard.DashboardService;
import org.criticizer.service.genre.GenreService;
import org.criticizer.service.tag.TagService;
import org.criticizer.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * View Controller for Admin pages.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminViewController {
    private static final Logger log = LoggerFactory.getLogger(AdminViewController.class);

    private final DashboardService dashboardService;
    private final UserService userService;
    private final TagService tagService;
    private final GenreService genreService;
    private final SecurityUtil securityUtil;


    public AdminViewController(DashboardService dashboardService,
                               UserService userService,
                               TagService tagService,
                               GenreService genreService,
                               SecurityUtil securityUtil
    ) {
        this.dashboardService = dashboardService;
        this.userService = userService;
        this.tagService = tagService;
        this.genreService = genreService;
        this.securityUtil = securityUtil;

    }

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        var currentUser = securityUtil.getCurrentUser();
        log.debug("Loading admin dashboard for user: {}", currentUser.getName());

        var stats = dashboardService.getAdminDashboardStats();

        model.addAttribute("stats", stats);
        model.addAttribute("currentAdmin", currentUser.getName());

        return "admin/adminDashboard";
    }

    @GetMapping("/users")
    public String usersManagement(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            Model model) {

        log.debug("Loading user management - page: {}, search: {}", page, search);

        // Get current user ID
        var currentUser = securityUtil.getCurrentUser();
        int currentUserId = currentUser.getId();


        PageResponse<User> result = userService.getUsersPage(search, page, size, false);

        model.addAttribute("userList", result.getItems());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("searchTerm", search);
        model.addAttribute("currentUserId", currentUserId);

        return "admin/userList";
    }

    @GetMapping("/management")
    public String management(
            @RequestParam(required = false, defaultValue = "tags") String type,
            Model model) {

        log.debug("Loading management page for type: {}", type);

        if ("tags".equals(type)) {
            model.addAttribute("items", tagService.getAllTags());
            model.addAttribute("itemType", "Tag");
        } else if ("genres".equals(type)) {
            model.addAttribute("items", genreService.getAllGenres());
            model.addAttribute("itemType", "Genre");
        } else {
            log.warn("Invalid management type: {}", type);
            return "redirect:/admin/management?type=tags";
        }

        model.addAttribute("type", type);


        return "admin/management";
    }
}