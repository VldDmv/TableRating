package org.criticizer.controller.admin;

import org.criticizer.entity.Role;
import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.user.UserService;
import org.criticizer.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminActionsController Tests")
class AdminActionsControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private AdminActionsController controller;

    private MockMvc mockMvc;
    private User adminUser;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();

        adminUser = TestDataBuilder.createAdminUser();
    }

    @Test
    @DisplayName("Should change user role successfully")
    void shouldChangeUserRole() throws Exception {
        // Given
        when(securityUtil.getCurrentUser()).thenReturn(adminUser);
        doNothing().when(userService).changeUserRole(anyInt(), any(Role.class), any(User.class));

        // When & Then
        mockMvc.perform(post("/admin/changeRole")
                        .param("userId", "2")
                        .param("newRole", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("flashSuccessMessage"));

        verify(userService).changeUserRole(eq(2), eq(Role.ADMIN), eq(adminUser));
    }

    @Test
    @DisplayName("Should handle invalid role gracefully")
    void shouldHandleInvalidRole() throws Exception {
        // Given
        when(securityUtil.getCurrentUser()).thenReturn(adminUser);

        // When & Then
        mockMvc.perform(post("/admin/changeRole")
                        .param("userId", "2")
                        .param("newRole", "INVALID_ROLE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("flashErrorMessage"));

        verify(userService, never()).changeUserRole(anyInt(), any(Role.class), any(User.class));
    }

    @Test
    @DisplayName("Should delete user successfully")
    void shouldDeleteUser() throws Exception {
        // Given
        when(securityUtil.getCurrentUser()).thenReturn(adminUser);
        doNothing().when(userService).deleteUser(anyInt(), any(User.class));

        // When & Then
        mockMvc.perform(post("/admin/deleteUser")
                        .param("userId", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("flashSuccessMessage"));

        verify(userService).deleteUser(eq(2), eq(adminUser));
    }

    @Test
    @DisplayName("Should handle delete user error")
    void shouldHandleDeleteUserError() throws Exception {
        // Given
        when(securityUtil.getCurrentUser()).thenReturn(adminUser);
        doThrow(new RuntimeException("Cannot delete user"))
                .when(userService).deleteUser(anyInt(), any(User.class));

        // When & Then
        mockMvc.perform(post("/admin/deleteUser")
                        .param("userId", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("flashErrorMessage"));

        verify(userService).deleteUser(eq(2), eq(adminUser));
    }
}
