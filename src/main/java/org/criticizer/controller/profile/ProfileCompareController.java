package org.criticizer.controller.profile;

import java.util.Map;
import org.criticizer.entity.User;
import org.criticizer.exceptions.data.UserNotFoundException;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.profile.ProfileAccessService;
import org.criticizer.service.profile.ProfileComparisonService;
import org.criticizer.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API for comparing the current user's media lists with another user's. */
@RestController
public class ProfileCompareController {
    private static final Logger log = LoggerFactory.getLogger(ProfileCompareController.class);

    private final UserService userService;
    private final SecurityUtil securityUtil;
    private final ProfileAccessService accessService;
    private final ProfileComparisonService comparisonService;

    public ProfileCompareController(
            UserService userService,
            SecurityUtil securityUtil,
            ProfileAccessService accessService,
            ProfileComparisonService comparisonService) {
        this.userService = userService;
        this.securityUtil = securityUtil;
        this.accessService = accessService;
        this.comparisonService = comparisonService;
    }

    /** GET /profile-compare Compare the logged-in user's lists with {@code username}'s. */
    @GetMapping("/profile-compare")
    public ResponseEntity<?> compareProfiles(@RequestParam String username) {
        String currentUsername = securityUtil.getCurrentUsername();
        if (currentUsername == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Log in to compare profiles"));
        }
        if (currentUsername.equalsIgnoreCase(username)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot compare a profile with itself"));
        }

        try {
            User other = userService.getUser(username);

            if (!accessService.canViewProfile(other, currentUsername)) {
                return ResponseEntity.status(403).body(Map.of("error", "Profile is private"));
            }

            User me = securityUtil.getCurrentUser();
            return ResponseEntity.ok(comparisonService.compare(me, other));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found: " + username));
        } catch (Exception e) {
            log.error("Error comparing profiles {} / {}", currentUsername, username, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to compare profiles"));
        }
    }
}
