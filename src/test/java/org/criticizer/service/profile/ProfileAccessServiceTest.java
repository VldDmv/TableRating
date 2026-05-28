package org.criticizer.service.profile;

import static org.assertj.core.api.Assertions.*;

import org.criticizer.entity.User;
import org.criticizer.exceptions.security.InsufficientPermissionsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for ProfileAccessService. Tests profile access control logic. */
@DisplayName("ProfileAccessService Tests")
class ProfileAccessServiceTest {

    private ProfileAccessService profileAccessService;
    private User publicProfileUser;
    private User privateProfileUser;

    @BeforeEach
    void setUp() {
        profileAccessService = new ProfileAccessService();

        publicProfileUser = new User("public_user", "password");
        publicProfileUser.setId(1);
        publicProfileUser.setProfileIsPublic(true);

        privateProfileUser = new User("private_user", "password");
        privateProfileUser.setId(2);
        privateProfileUser.setProfileIsPublic(false);
    }

    // ==================== CAN VIEW PROFILE TESTS ====================

    @Nested
    @DisplayName("canViewProfile() Tests")
    class CanViewProfileTests {

        @Test
        @DisplayName("Should allow viewing public profile by anyone")
        void shouldAllowViewingPublicProfile() {
            // When - Anonymous user
            boolean resultAnonymous = profileAccessService.canViewProfile(publicProfileUser, null);

            // When - Different user
            boolean resultDifferentUser =
                    profileAccessService.canViewProfile(publicProfileUser, "other_user");

            // Then
            assertThat(resultAnonymous).isTrue();
            assertThat(resultDifferentUser).isTrue();
        }

        @Test
        @DisplayName("Should allow owner to view their own private profile")
        void shouldAllowOwnerViewPrivateProfile() {
            // When
            boolean result =
                    profileAccessService.canViewProfile(privateProfileUser, "private_user");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should allow owner with different case to view profile")
        void shouldAllowOwnerWithDifferentCase() {
            // When
            boolean result =
                    profileAccessService.canViewProfile(privateProfileUser, "PRIVATE_USER");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should deny anonymous user from viewing private profile")
        void shouldDenyAnonymousFromPrivateProfile() {
            // When
            boolean result = profileAccessService.canViewProfile(privateProfileUser, null);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should deny other user from viewing private profile")
        void shouldDenyOtherUserFromPrivateProfile() {
            // When
            boolean result = profileAccessService.canViewProfile(privateProfileUser, "other_user");

            // Then
            assertThat(result).isFalse();
        }
    }

    // ==================== CHECK ACCESS TESTS ====================

    @Nested
    @DisplayName("checkAccess() Tests")
    class CheckAccessTests {

        @Test
        @DisplayName("Should return correct context for owner viewing public profile")
        void shouldReturnContextForOwnerPublic() {
            // When
            ProfileAccessService.ProfileAccessContext context =
                    profileAccessService.checkAccess(publicProfileUser, "public_user");

            // Then
            assertThat(context.isOwner()).isTrue();
            assertThat(context.canView()).isTrue();
            assertThat(context.profileOwner()).isEqualTo(publicProfileUser);
        }

        @Test
        @DisplayName("Should return correct context for owner viewing private profile")
        void shouldReturnContextForOwnerPrivate() {
            // When
            ProfileAccessService.ProfileAccessContext context =
                    profileAccessService.checkAccess(privateProfileUser, "private_user");

            // Then
            assertThat(context.isOwner()).isTrue();
            assertThat(context.canView()).isTrue();
            assertThat(context.profileOwner()).isEqualTo(privateProfileUser);
        }

        @Test
        @DisplayName("Should return correct context for other user viewing public profile")
        void shouldReturnContextForOtherUserPublic() {
            // When
            ProfileAccessService.ProfileAccessContext context =
                    profileAccessService.checkAccess(publicProfileUser, "other_user");

            // Then
            assertThat(context.isOwner()).isFalse();
            assertThat(context.canView()).isTrue();
            assertThat(context.profileOwner()).isEqualTo(publicProfileUser);
        }

        @Test
        @DisplayName("Should return correct context for other user viewing private profile")
        void shouldReturnContextForOtherUserPrivate() {
            // When
            ProfileAccessService.ProfileAccessContext context =
                    profileAccessService.checkAccess(privateProfileUser, "other_user");

            // Then
            assertThat(context.isOwner()).isFalse();
            assertThat(context.canView()).isFalse();
            assertThat(context.profileOwner()).isEqualTo(privateProfileUser);
        }

        @Test
        @DisplayName("Should return correct context for anonymous user viewing public profile")
        void shouldReturnContextForAnonymousPublic() {
            // When
            ProfileAccessService.ProfileAccessContext context =
                    profileAccessService.checkAccess(publicProfileUser, null);

            // Then
            assertThat(context.isOwner()).isFalse();
            assertThat(context.canView()).isTrue();
            assertThat(context.profileOwner()).isEqualTo(publicProfileUser);
        }

        @Test
        @DisplayName("Should return correct context for anonymous user viewing private profile")
        void shouldReturnContextForAnonymousPrivate() {
            // When
            ProfileAccessService.ProfileAccessContext context =
                    profileAccessService.checkAccess(privateProfileUser, null);

            // Then
            assertThat(context.isOwner()).isFalse();
            assertThat(context.canView()).isFalse();
            assertThat(context.profileOwner()).isEqualTo(privateProfileUser);
        }

        @Test
        @DisplayName("Should handle case-insensitive username comparison")
        void shouldHandleCaseInsensitive() {
            // When
            ProfileAccessService.ProfileAccessContext context =
                    profileAccessService.checkAccess(publicProfileUser, "PUBLIC_USER");

            // Then
            assertThat(context.isOwner()).isTrue();
            assertThat(context.canView()).isTrue();
        }
    }

    // ==================== REQUIRE VIEW ACCESS TESTS ====================

    @Nested
    @DisplayName("requireViewAccess() Tests")
    class RequireViewAccessTests {

        @Test
        @DisplayName("Should not throw when access is allowed")
        void shouldNotThrowWhenAccessAllowed() {
            // Given
            ProfileAccessService.ProfileAccessContext context =
                    profileAccessService.checkAccess(publicProfileUser, "any_user");

            // When & Then
            assertThatCode(() -> context.requireViewAccess()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw InsufficientPermissionsException when access denied")
        void shouldThrowWhenAccessDenied() {
            // Given
            ProfileAccessService.ProfileAccessContext context =
                    profileAccessService.checkAccess(privateProfileUser, "other_user");

            // When & Then
            assertThatThrownBy(() -> context.requireViewAccess())
                    .isInstanceOf(InsufficientPermissionsException.class)
                    .hasMessageContaining("VIEW_PRIVATE_PROFILE");
        }

        @Test
        @DisplayName("Should throw for anonymous user viewing private profile")
        void shouldThrowForAnonymousViewingPrivate() {
            // Given
            ProfileAccessService.ProfileAccessContext context =
                    profileAccessService.checkAccess(privateProfileUser, null);

            // When & Then
            assertThatThrownBy(() -> context.requireViewAccess())
                    .isInstanceOf(InsufficientPermissionsException.class);
        }

        @Test
        @DisplayName("Should not throw for owner viewing their private profile")
        void shouldNotThrowForOwnerViewingPrivate() {
            // Given
            ProfileAccessService.ProfileAccessContext context =
                    profileAccessService.checkAccess(privateProfileUser, "private_user");

            // When & Then
            assertThatCode(() -> context.requireViewAccess()).doesNotThrowAnyException();
        }
    }

    // ==================== EDGE CASES TESTS ====================

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle empty string as current username")
        void shouldHandleEmptyString() {
            // When
            boolean result = profileAccessService.canViewProfile(privateProfileUser, "");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should handle whitespace-only username")
        void shouldHandleWhitespace() {
            // When
            ProfileAccessService.ProfileAccessContext context =
                    profileAccessService.checkAccess(privateProfileUser, "   ");

            // Then
            assertThat(context.isOwner()).isFalse();
            assertThat(context.canView()).isFalse();
        }

        @Test
        @DisplayName("Should properly compare usernames with special characters")
        void shouldCompareSpecialCharacters() {
            // Given
            User userWithSpecialChars = new User("user_123", "password");
            userWithSpecialChars.setProfileIsPublic(false);

            // When
            ProfileAccessService.ProfileAccessContext context =
                    profileAccessService.checkAccess(userWithSpecialChars, "user_123");

            // Then
            assertThat(context.isOwner()).isTrue();
            assertThat(context.canView()).isTrue();
        }

        @Test
        @DisplayName("Should handle profile owner with mixed case username")
        void shouldHandleMixedCase() {
            // Given
            User mixedCaseUser = new User("MixedCase", "password");
            mixedCaseUser.setProfileIsPublic(false);

            // When - User tries with different case
            boolean resultLowerCase =
                    profileAccessService.canViewProfile(mixedCaseUser, "mixedcase");
            boolean resultUpperCase =
                    profileAccessService.canViewProfile(mixedCaseUser, "MIXEDCASE");

            // Then
            assertThat(resultLowerCase).isTrue();
            assertThat(resultUpperCase).isTrue();
        }
    }

    // ==================== INTEGRATION SCENARIOS ====================

    @Nested
    @DisplayName("Integration Scenarios")
    class IntegrationScenarios {

        @Test
        @DisplayName("Scenario: Anonymous user browsing public profiles")
        void scenarioAnonymousBrowsingPublic() {
            // When
            ProfileAccessService.ProfileAccessContext context =
                    profileAccessService.checkAccess(publicProfileUser, null);

            // Then - Should see profile but not be marked as owner
            assertThat(context.canView()).isTrue();
            assertThat(context.isOwner()).isFalse();
            assertThatCode(() -> context.requireViewAccess()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Scenario: Logged-in user viewing their own profile")
        void scenarioUserViewingOwnProfile() {
            // When
            ProfileAccessService.ProfileAccessContext context =
                    profileAccessService.checkAccess(privateProfileUser, "private_user");

            // Then - Should have full access as owner
            assertThat(context.canView()).isTrue();
            assertThat(context.isOwner()).isTrue();
            assertThat(context.profileOwner().getName()).isEqualTo("private_user");
        }

        @Test
        @DisplayName("Scenario: User trying to access another's private profile")
        void scenarioAccessingOthersPrivateProfile() {
            // When
            ProfileAccessService.ProfileAccessContext context =
                    profileAccessService.checkAccess(privateProfileUser, "different_user");

            // Then - Should be denied
            assertThat(context.canView()).isFalse();
            assertThat(context.isOwner()).isFalse();
            assertThatThrownBy(() -> context.requireViewAccess())
                    .isInstanceOf(InsufficientPermissionsException.class);
        }

        @Test
        @DisplayName("Scenario: User browsing other public profiles")
        void scenarioBrowsingOthersPublicProfiles() {
            // When
            ProfileAccessService.ProfileAccessContext context =
                    profileAccessService.checkAccess(publicProfileUser, "different_user");

            // Then - Should see profile but not be owner
            assertThat(context.canView()).isTrue();
            assertThat(context.isOwner()).isFalse();
        }
    }
}
