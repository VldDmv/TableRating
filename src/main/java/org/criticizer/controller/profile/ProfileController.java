package org.criticizer.controller.profile;

import jakarta.validation.Valid;
import org.criticizer.dto.helper.MessageResponse;
import org.criticizer.dto.helper.PrivacyUpdateResponse;
import org.criticizer.dto.profile.ProfileResponse;
import org.criticizer.dto.profile.UpdatePrivacyRequest;
import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.profile.ProfileAccessService;
import org.criticizer.service.profile.ProfileAccessService.ProfileAccessContext;
import org.criticizer.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for user profiles.
 */
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {
    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final UserService userService;
    private final SecurityUtil securityUtil;
    private final ProfileAccessService accessService;

    public ProfileController(
            UserService userService,
            SecurityUtil securityUtil,
            ProfileAccessService accessService
    ) {
        this.userService = userService;
        this.securityUtil = securityUtil;
        this.accessService = accessService;
    }

    /**
     * GET /api/profiles/{username}
     * Get user profile information.
     */
    @GetMapping("/{username}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable String username) {
        log.debug("Profile request for user: {}", username);

        User profileOwner = userService.getUser(username);
        String currentUsername = securityUtil.getCurrentUsername();

        ProfileAccessContext context = accessService.checkAccess(profileOwner, currentUsername);

        if (!context.canView()) {
            log.debug("Access denied to private profile: {}", username);
            return ResponseEntity.status(403).build();
        }

        ProfileResponse response = context.isOwner()
                ? ProfileResponse.forOwner(profileOwner)
                : ProfileResponse.forViewer(profileOwner, context.canView());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/profiles/me
     * Get current user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getCurrentUserProfile() {
        User currentUser = securityUtil.getCurrentUser();
        log.debug("Current user {} requesting their own profile", currentUser.getName());
        return ResponseEntity.ok(ProfileResponse.forOwner(currentUser));
    }

    /**
     * PUT /api/profiles/me/privacy
     * Update current user's privacy setting.
     */
    @PutMapping("/me/privacy")
    public ResponseEntity<PrivacyUpdateResponse> updatePrivacy(
            @Valid @RequestBody UpdatePrivacyRequest request) {

        User currentUser = securityUtil.getCurrentUser();
        log.info("User {} updating privacy to: {}",
                currentUser.getName(), request.isPublic());

        userService.updateUserPrivacy(currentUser.getId(), request.isPublic());

        return ResponseEntity.ok(new PrivacyUpdateResponse(
                "Privacy setting updated successfully",
                request.isPublic()
        ));
    }

    /**
     * GET /api/profiles/{username}/games
     * Get user's games (respects privacy).
     */
    @GetMapping("/{username}/games")
    public ResponseEntity<MessageResponse> getUserGames(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("Requesting games for user: {}", username);

        User profileOwner = userService.getUser(username);
        String currentUsername = securityUtil.getCurrentUsername();

        if (!accessService.canViewProfile(profileOwner, currentUsername)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(new MessageResponse(
                "Use /api/games endpoint with authentication"
        ));
    }
}