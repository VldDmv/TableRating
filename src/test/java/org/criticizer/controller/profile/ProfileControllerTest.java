package org.criticizer.controller.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.criticizer.dto.profile.UpdatePrivacyRequest;
import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.profile.ProfileAccessService;
import org.criticizer.service.user.UserService;
import org.criticizer.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileController Tests")
class ProfileControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private ProfileAccessService accessService;

    @InjectMocks
    private ProfileController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private User privateUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testUser = TestDataBuilder.createRegularUser();
        privateUser = TestDataBuilder.createPrivateUser();
    }

    @Test
    @DisplayName("GET /api/profiles/{username} - Should return public profile")
    void shouldReturnPublicProfile() throws Exception {
        // Given
        when(userService.getUser("testuser")).thenReturn(testUser);
        when(securityUtil.getCurrentUsername()).thenReturn("otheruser");

        ProfileAccessService.ProfileAccessContext context =
                new ProfileAccessService.ProfileAccessContext(false, true, testUser);
        when(accessService.checkAccess(any(User.class), anyString())).thenReturn(context);

        // When & Then
        mockMvc.perform(get("/api/profiles/testuser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(userService).getUser("testuser");
    }

    @Test
    @DisplayName("GET /api/profiles/{username} - Should deny access to private profile")
    void shouldDenyAccessToPrivateProfile() throws Exception {
        // Given
        when(userService.getUser("privateuser")).thenReturn(privateUser);
        when(securityUtil.getCurrentUsername()).thenReturn("otheruser");

        ProfileAccessService.ProfileAccessContext context =
                new ProfileAccessService.ProfileAccessContext(false, false, privateUser);
        when(accessService.checkAccess(any(User.class), anyString())).thenReturn(context);

        // When & Then
        mockMvc.perform(get("/api/profiles/privateuser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(userService).getUser("privateuser");
    }

    @Test
    @DisplayName("GET /api/profiles/me - Should return current user's profile")
    void shouldReturnCurrentUserProfile() throws Exception {
        // Given
        when(securityUtil.getCurrentUser()).thenReturn(testUser);

        // When & Then
        mockMvc.perform(get("/api/profiles/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(securityUtil).getCurrentUser();
    }

    @Test
    @DisplayName("PUT /api/profiles/me/privacy - Should update privacy setting")
    void shouldUpdatePrivacySetting() throws Exception {
        // Given
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        UpdatePrivacyRequest request = new UpdatePrivacyRequest(false);

        // When & Then
        mockMvc.perform(put("/api/profiles/me/privacy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(userService).updateUserPrivacy(testUser.getId(), false);
    }
}