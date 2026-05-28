package org.criticizer.controller.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.HttpSession;
import org.criticizer.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Mock private UserService userService;

    @Mock private AuthenticationManager authenticationManager;

    @Mock private HttpSession httpSession;

    @InjectMocks private AuthController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");

        mockMvc =
                MockMvcBuilders.standaloneSetup(controller).setViewResolvers(viewResolver).build();
    }

    @Test
    @DisplayName("POST /auth/register - Should register user successfully")
    void shouldRegisterUser() throws Exception {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        doNothing().when(userService).registerUser(anyString(), anyString());

        // When & Then
        mockMvc.perform(
                        post("/auth/register")
                                .param("username", "newuser")
                                .param("password", "password123")
                                .param("confirmPassword", "password123")
                                .sessionAttr("SPRING_SECURITY_CONTEXT", httpSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(userService).registerUser("newuser", "password123");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("POST /auth/register - Should reject when passwords don't match")
    void shouldRejectWhenPasswordsDontMatch() throws Exception {
        // When & Then
        mockMvc.perform(
                        post("/auth/register")
                                .param("username", "newuser")
                                .param("password", "password123")
                                .param("confirmPassword", "different"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index?showRegister=true"))
                .andExpect(flash().attributeExists("flashErrorMessage"));

        verify(userService, never()).registerUser(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /auth/register - Should reject when fields are missing")
    void shouldRejectWhenFieldsAreMissing() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/register").param("username", "newuser"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index?showRegister=true"))
                .andExpect(flash().attributeExists("flashErrorMessage"));

        verify(userService, never()).registerUser(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /auth/register - Should handle registration failure")
    void shouldHandleRegistrationFailure() throws Exception {
        // Given
        doThrow(new RuntimeException("Username already exists"))
                .when(userService)
                .registerUser(anyString(), anyString());

        // When & Then
        mockMvc.perform(
                        post("/auth/register")
                                .param("username", "existinguser")
                                .param("password", "password123")
                                .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index?showRegister=true"))
                .andExpect(flash().attributeExists("flashErrorMessage"));

        verify(userService).registerUser("existinguser", "password123");
    }

    @Test
    @DisplayName("GET /auth/check-username - Should return available true for new username")
    void shouldReturnAvailableForNewUsername() throws Exception {
        // Given
        when(userService.existsByUsername("newuser")).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/auth/check-username").param("username", "newuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));

        verify(userService).existsByUsername("newuser");
    }

    @Test
    @DisplayName("GET /auth/check-username - Should return available false for existing username")
    void shouldReturnAvailableForExistingUsername() throws Exception {
        // Given
        when(userService.existsByUsername("existinguser")).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/auth/check-username").param("username", "existinguser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));

        verify(userService).existsByUsername("existinguser");
    }

    @Test
    @DisplayName("GET /auth/check-username - Should return available false for empty username")
    void shouldReturnAvailableForEmptyUsername() throws Exception {
        // When & Then
        mockMvc.perform(get("/auth/check-username").param("username", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));

        verify(userService, never()).existsByUsername(anyString());
    }

    @Test
    @DisplayName("GET /auth/check-username - Should trim username before checking")
    void shouldTrimUsername() throws Exception {
        // Given
        when(userService.existsByUsername("newuser")).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/auth/check-username").param("username", "  newuser  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));

        verify(userService).existsByUsername("newuser");
    }
}
