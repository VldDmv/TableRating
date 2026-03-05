package org.criticizer.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.criticizer.config.GlobalExceptionHandler;
import org.criticizer.dto.admin.AdminStats;
import org.criticizer.dto.admin.ChangeRoleRequest;
import org.criticizer.dto.helper.PageResponse;
import org.criticizer.entity.Role;
import org.criticizer.entity.User;
import org.criticizer.exceptions.data.UserNotFoundException;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.dashboard.DashboardService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController Tests")
class AdminControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private AdminController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User adminUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        adminUser = TestDataBuilder.createAdminUser();
    }

    @Test
    @DisplayName("GET /api/admin/stats - Should return dashboard statistics")
    void shouldGetStatistics() throws Exception {
        AdminStats stats = TestDataBuilder.createAdminStats();
        when(dashboardService.getAdminDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/stats").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(100))
                .andExpect(jsonPath("$.totalGames").value(50));

        verify(dashboardService).getAdminDashboardStats();
    }

    @Test
    @DisplayName("GET /api/admin/users - Should return paginated users")
    void shouldGetUsers() throws Exception {
        List<User> users = List.of(
                TestDataBuilder.createUser(1, "user1", Role.USER),
                TestDataBuilder.createUser(2, "user2", Role.USER)
        );
        PageResponse<User> pageResponse = TestDataBuilder.createPageResponse(users, 1, 10);
        when(userService.getUsersPage(any(), eq(1), eq(10), eq(false))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0").param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2));

        verify(userService).getUsersPage(any(), eq(1), eq(10), eq(false));
    }

    @Test
    @DisplayName("GET /api/admin/users - Should support search")
    void shouldGetUsersWithSearch() throws Exception {
        List<User> users = List.of(TestDataBuilder.createUser(1, "john", Role.USER));
        PageResponse<User> pageResponse = TestDataBuilder.createPageResponse(users, 1, 10);
        when(userService.getUsersPage(eq("john"), eq(1), eq(10), eq(false))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0").param("size", "10").param("search", "john")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));

        verify(userService).getUsersPage(eq("john"), eq(1), eq(10), eq(false));
    }

    @Test
    @DisplayName("PUT /api/admin/users/{userId}/role - Should change user role")
    void shouldChangeUserRole() throws Exception {
        when(securityUtil.getCurrentUser()).thenReturn(adminUser);
        doNothing().when(userService).changeUserRole(anyInt(), any(Role.class), any(User.class));

        ChangeRoleRequest request = new ChangeRoleRequest(Role.ADMIN);

        mockMvc.perform(put("/api/admin/users/2/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User role updated successfully"));

        verify(userService).changeUserRole(eq(2), eq(Role.ADMIN), eq(adminUser));
    }

    @Test
    @DisplayName("DELETE /api/admin/users/{userId} - Should delete user")
    void shouldDeleteUser() throws Exception {
        when(securityUtil.getCurrentUser()).thenReturn(adminUser);
        doNothing().when(userService).deleteUser(anyInt(), any(User.class));

        mockMvc.perform(delete("/api/admin/users/2").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        verify(userService).deleteUser(eq(2), eq(adminUser));
    }

    @Test
    @DisplayName("GET /api/admin/users/{userId} - Should get user details")
    void shouldGetUserDetails() throws Exception {

        User user = TestDataBuilder.createUser(2, "testuser", Role.USER);
        when(userService.getUserById(2)).thenReturn(user);

        mockMvc.perform(get("/api/admin/users/2").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("testuser"));

        verify(userService).getUserById(2);
    }

    @Test
    @DisplayName("GET /api/admin/users/{userId} - Should return 404 for non-existent user")
    void shouldReturn404ForNonExistentUser() throws Exception {
        when(userService.getUserById(999))
                .thenThrow(new UserNotFoundException("User not found: 999"));

        mockMvc.perform(get("/api/admin/users/999").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(userService).getUserById(999);
    }
}