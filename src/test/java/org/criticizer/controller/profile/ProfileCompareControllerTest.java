package org.criticizer.controller.profile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.criticizer.entity.User;
import org.criticizer.exceptions.data.UserNotFoundException;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.profile.ProfileAccessService;
import org.criticizer.service.profile.ProfileComparisonService;
import org.criticizer.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileCompareController Tests")
class ProfileCompareControllerTest {

    @Mock private UserService userService;
    @Mock private SecurityUtil securityUtil;
    @Mock private ProfileAccessService accessService;
    @Mock private ProfileComparisonService comparisonService;

    @InjectMocks private ProfileCompareController controller;

    private MockMvc mockMvc;
    private User me;
    private User other;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        me = new User("alice", "password");
        me.setId(1);
        other = new User("bob", "password");
        other.setId(2);
    }

    @Test
    @DisplayName("GET /profile-compare - Should return comparison for viewable profile")
    void shouldReturnComparison() throws Exception {
        when(securityUtil.getCurrentUsername()).thenReturn("alice");
        when(userService.getUser("bob")).thenReturn(other);
        when(accessService.canViewProfile(other, "alice")).thenReturn(true);
        when(securityUtil.getCurrentUser()).thenReturn(me);
        when(comparisonService.compare(me, other))
                .thenReturn(Map.of("me", "alice", "other", "bob", "compatibility", 91.5));

        mockMvc.perform(get("/profile-compare").param("username", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.me").value("alice"))
                .andExpect(jsonPath("$.other").value("bob"))
                .andExpect(jsonPath("$.compatibility").value(91.5));
    }

    @Test
    @DisplayName("GET /profile-compare - Should return 401 for anonymous users")
    void shouldRejectAnonymous() throws Exception {
        when(securityUtil.getCurrentUsername()).thenReturn(null);

        mockMvc.perform(get("/profile-compare").param("username", "bob"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());

        verify(comparisonService, never()).compare(any(), any());
    }

    @Test
    @DisplayName("GET /profile-compare - Should return 400 when comparing with yourself")
    void shouldRejectSelfComparison() throws Exception {
        when(securityUtil.getCurrentUsername()).thenReturn("alice");

        mockMvc.perform(get("/profile-compare").param("username", "ALICE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verify(comparisonService, never()).compare(any(), any());
    }

    @Test
    @DisplayName("GET /profile-compare - Should return 403 for private profiles")
    void shouldRejectPrivateProfile() throws Exception {
        when(securityUtil.getCurrentUsername()).thenReturn("alice");
        when(userService.getUser("bob")).thenReturn(other);
        when(accessService.canViewProfile(other, "alice")).thenReturn(false);

        mockMvc.perform(get("/profile-compare").param("username", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Profile is private"));

        verify(comparisonService, never()).compare(any(), any());
    }

    @Test
    @DisplayName("GET /profile-compare - Should return 404 for unknown users")
    void shouldReturn404ForUnknownUser() throws Exception {
        when(securityUtil.getCurrentUsername()).thenReturn("alice");
        when(userService.getUser("ghost")).thenThrow(new UserNotFoundException("ghost"));

        mockMvc.perform(get("/profile-compare").param("username", "ghost"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /profile-compare - Should return 500 when comparison fails")
    void shouldReturn500OnFailure() throws Exception {
        when(securityUtil.getCurrentUsername()).thenReturn("alice");
        when(userService.getUser("bob")).thenReturn(other);
        when(accessService.canViewProfile(other, "alice")).thenReturn(true);
        when(securityUtil.getCurrentUser()).thenReturn(me);
        when(comparisonService.compare(any(), any())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/profile-compare").param("username", "bob"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to compare profiles"));
    }

    @Test
    @DisplayName("GET /profile-compare - Should pass access check with the current username")
    void shouldUseCurrentUsernameForAccessCheck() throws Exception {
        when(securityUtil.getCurrentUsername()).thenReturn("alice");
        when(userService.getUser("bob")).thenReturn(other);
        when(accessService.canViewProfile(other, "alice")).thenReturn(true);
        when(securityUtil.getCurrentUser()).thenReturn(me);
        when(comparisonService.compare(me, other)).thenReturn(Map.of());

        mockMvc.perform(get("/profile-compare").param("username", "bob"))
                .andExpect(status().isOk());

        verify(accessService).canViewProfile(other, "alice");
    }
}
