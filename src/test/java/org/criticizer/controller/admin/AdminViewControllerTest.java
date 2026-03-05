package org.criticizer.controller.admin;

import org.criticizer.dto.admin.AdminStats;
import org.criticizer.dto.helper.PageResponse;
import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.dashboard.DashboardService;
import org.criticizer.service.genre.GenreService;
import org.criticizer.service.tag.TagService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminViewController Tests")
class AdminViewControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private UserService userService;

    @Mock
    private TagService tagService;

    @Mock
    private GenreService genreService;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private AdminViewController controller;

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
    @DisplayName("GET /admin/dashboard - Should load admin dashboard")
    void shouldLoadAdminDashboard() throws Exception {
        // Given
        AdminStats stats = TestDataBuilder.createAdminStats();
        when(securityUtil.getCurrentUser()).thenReturn(adminUser);
        when(dashboardService.getAdminDashboardStats()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/adminDashboard"))
                .andExpect(model().attributeExists("stats"))
                .andExpect(model().attribute("currentAdmin", "admin"));

        verify(securityUtil).getCurrentUser();
        verify(dashboardService).getAdminDashboardStats();
    }

    @Test
    @DisplayName("GET /admin/users - Should load users management page")
    void shouldLoadUsersManagement() throws Exception {
        // Given
        when(securityUtil.getCurrentUser()).thenReturn(adminUser);
        
        List<User> users = List.of(
                TestDataBuilder.createUser(1, "user1", org.criticizer.entity.Role.USER),
                TestDataBuilder.createUser(2, "user2", org.criticizer.entity.Role.USER)
        );
        PageResponse<User> pageResponse = TestDataBuilder.createPageResponse(users, 1, 20);
        
        when(userService.getUsersPage(isNull(), eq(1), eq(20), eq(false)))
                .thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/userList"))
                .andExpect(model().attributeExists("userList"))
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(model().attribute("currentUserId", 1));

        verify(userService).getUsersPage(isNull(), eq(1), eq(20), eq(false));
    }

    @Test
    @DisplayName("GET /admin/users - Should support search")
    void shouldLoadUsersManagementWithSearch() throws Exception {
        // Given
        when(securityUtil.getCurrentUser()).thenReturn(adminUser);
        
        List<User> users = List.of(TestDataBuilder.createUser(1, "john", org.criticizer.entity.Role.USER));
        PageResponse<User> pageResponse = TestDataBuilder.createPageResponse(users, 1, 20);
        
        when(userService.getUsersPage(eq("john"), eq(1), eq(20), eq(false)))
                .thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/admin/users")
                        .param("search", "john"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/userList"))
                .andExpect(model().attribute("searchTerm", "john"));

        verify(userService).getUsersPage(eq("john"), eq(1), eq(20), eq(false));
    }

    @Test
    @DisplayName("GET /admin/management - Should load tags management")
    void shouldLoadTagsManagement() throws Exception {
        // Given
        when(tagService.getAllTags()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/admin/management")
                        .param("type", "tags"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/management"))
                .andExpect(model().attribute("type", "tags"))
                .andExpect(model().attribute("itemType", "Tag"))
                .andExpect(model().attributeExists("items"));

        verify(tagService).getAllTags();
    }

    @Test
    @DisplayName("GET /admin/management - Should load genres management")
    void shouldLoadGenresManagement() throws Exception {
        // Given
        when(genreService.getAllGenres()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/admin/management")
                        .param("type", "genres"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/management"))
                .andExpect(model().attribute("type", "genres"))
                .andExpect(model().attribute("itemType", "Genre"))
                .andExpect(model().attributeExists("items"));

        verify(genreService).getAllGenres();
    }

    @Test
    @DisplayName("GET /admin/management - Should redirect on invalid type")
    void shouldRedirectOnInvalidType() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/management")
                        .param("type", "invalid"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?type=tags"));
    }

    @Test
    @DisplayName("GET /admin/management - Should default to tags")
    void shouldDefaultToTags() throws Exception {
        // Given
        when(tagService.getAllTags()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/admin/management"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/management"))
                .andExpect(model().attribute("type", "tags"));

        verify(tagService).getAllTags();
    }
}
