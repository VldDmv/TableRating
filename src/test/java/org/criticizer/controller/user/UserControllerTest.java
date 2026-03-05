package org.criticizer.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.criticizer.dto.helper.PageResponse;
import org.criticizer.dto.user.UserPublicResponse;
import org.criticizer.service.user.UserService;
import org.criticizer.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("GET /api/users - Should return paginated users")
    void shouldReturnPaginatedUsers() throws Exception {
        var users = List.of(
                TestDataBuilder.createUser(1, "user1", org.criticizer.entity.Role.USER),
                TestDataBuilder.createUser(2, "user2", org.criticizer.entity.Role.USER)
        );
        List<UserPublicResponse> responseList = users.stream()
                .map(u -> Mockito.mock(UserPublicResponse.class))
                .collect(Collectors.toList());
        PageResponse<UserPublicResponse> pageResponse =
                TestDataBuilder.createPageResponse(responseList, 1, 20);

        when(userService.getUsersPageWithStats(
                isNull(), eq(1), eq(20), eq("totalItems"), eq("desc")))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/users").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2));

        verify(userService).getUsersPageWithStats(isNull(), eq(1), eq(20), eq("totalItems"), eq("desc"));
    }

    @Test
    @DisplayName("GET /api/users - Should support search")
    void shouldSupportSearch() throws Exception {
        var users = List.of(TestDataBuilder.createUser(1, "john", org.criticizer.entity.Role.USER));
        List<UserPublicResponse> responseList = users.stream()
                .map(u -> Mockito.mock(UserPublicResponse.class))
                .collect(Collectors.toList());
        PageResponse<UserPublicResponse> pageResponse =
                TestDataBuilder.createPageResponse(responseList, 1, 20);

        when(userService.getUsersPageWithStats(
                eq("john"), eq(1), eq(20), eq("totalItems"), eq("desc")))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/users")
                        .param("search", "john")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));

        verify(userService).getUsersPageWithStats(eq("john"), eq(1), eq(20), eq("totalItems"), eq("desc"));
    }

    @Test
    @DisplayName("GET /api/users/{username}/exists - Should return true for existing user")
    void shouldReturnTrueForExistingUser() throws Exception {
        when(userService.existsByUsername("testuser")).thenReturn(true);

        mockMvc.perform(get("/api/users/testuser/exists")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

        verify(userService).existsByUsername("testuser");
    }

    @Test
    @DisplayName("GET /api/users/{username}/exists - Should return false for non-existent user")
    void shouldReturnFalseForNonExistentUser() throws Exception {
        when(userService.existsByUsername("nonexistent")).thenReturn(false);

        mockMvc.perform(get("/api/users/nonexistent/exists")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));

        verify(userService).existsByUsername("nonexistent");
    }

    @Test
    @DisplayName("GET /api/users - Should support custom sorting")
    void shouldSupportCustomSorting() throws Exception {
        PageResponse<UserPublicResponse> pageResponse =
                TestDataBuilder.createEmptyPageResponse(1, 20);

        when(userService.getUsersPageWithStats(
                isNull(), eq(1), eq(20), eq("name"), eq("asc")))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/users")
                        .param("sortBy", "name")
                        .param("sortOrder", "asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(userService).getUsersPageWithStats(isNull(), eq(1), eq(20), eq("name"), eq("asc"));
    }
}