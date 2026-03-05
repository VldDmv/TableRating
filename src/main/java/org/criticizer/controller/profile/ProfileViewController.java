package org.criticizer.controller.profile;

import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.profile.ProfileAccessService;
import org.criticizer.service.profile.ProfileAccessService.ProfileAccessContext;
import org.criticizer.service.user.UserService;
import org.criticizer.service.game.GameService;
import org.criticizer.service.tag.TagService;
import org.criticizer.service.genre.GenreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * View Controller for user profile pages.
 */
@Controller
public class ProfileViewController {
    private static final Logger log = LoggerFactory.getLogger(ProfileViewController.class);

    private final UserService userService;
    private final GameService gameService;
    private final SecurityUtil securityUtil;
    private final ProfileAccessService accessService;
    private final TagService tagService;
    private final GenreService genreService;

    public ProfileViewController(
            UserService userService,
            GameService gameService,
            SecurityUtil securityUtil,
            ProfileAccessService accessService,
            TagService tagService,
            GenreService genreService
    ) {
        this.userService = userService;
        this.gameService = gameService;
        this.securityUtil = securityUtil;
        this.accessService = accessService;
        this.tagService = tagService;
        this.genreService = genreService;
    }

    @GetMapping("/profile")
    public String profilePage(@RequestParam String username, Model model) {
        log.debug("Loading profile page for user: {}", username);

        User profileOwner = userService.getUser(username);
        String currentUsername = securityUtil.getCurrentUsername();

        ProfileAccessContext context = accessService.checkAccess(profileOwner, currentUsername);

        log.debug("Access check: isOwner={}, canView={}",
                context.isOwner(), context.canView());

        model.addAttribute("profileOwner", profileOwner);
        model.addAttribute("isOwnerViewing", context.isOwner());
        model.addAttribute("canView", context.canView());

        if (context.canView()) {
            var initialData = gameService.getUserItemsPage(
                    profileOwner.getId(),
                    1,      // page
                    10,     // pageSize
                    null,   // tagId
                    null,   // searchTerm
                    "name", // sortBy
                    "asc"   // sortOrder
            );

            log.debug("Initial data loaded: {} items, page {}/{}",
                    initialData.getItems().size(),
                    initialData.getCurrentPage(),
                    initialData.getTotalPages());

            model.addAttribute("initialData", initialData);

            model.addAttribute("gameTags", tagService.getAllTags());
            model.addAttribute("movieGenres", genreService.getAvailableGenresFor("movie"));
            model.addAttribute("bookGenres", genreService.getAvailableGenresFor("book"));
            model.addAttribute("showGenres", genreService.getAvailableGenresFor("show"));

            log.debug("Tags/Genres loaded for filtering");
        } else {
            log.debug("User cannot view profile - not adding initialData");
        }

        return "users/profile";
    }

    @PostMapping("/profile")
    public String updatePrivacy(
            @RequestParam(required = false) String privacy,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = securityUtil.getCurrentUser();

        boolean isPublic = "public".equals(privacy);
        userService.updateUserPrivacy(currentUser.getId(), isPublic);

        log.info("User {} updated privacy to: {}", currentUser.getName(), isPublic);

        redirectAttributes.addFlashAttribute("flashSuccessMessage",
                "Privacy settings updated successfully");

        return "redirect:/profile?username=" + currentUser.getName();
    }
}