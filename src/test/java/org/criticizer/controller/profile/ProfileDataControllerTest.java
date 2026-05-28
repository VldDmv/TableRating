package org.criticizer.controller.profile;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.criticizer.dto.helper.PageResponse;
import org.criticizer.entity.Game;
import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.game.GameService;
import org.criticizer.service.helper.AbstractMediaService;
import org.criticizer.service.helper.MediaTypeResolver;
import org.criticizer.service.profile.ProfileAccessService;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileDataController Tests")
class ProfileDataControllerTest {

    @Mock private UserService userService;
    @Mock private SecurityUtil securityUtil;
    @Mock private ProfileAccessService accessService;
    @Mock private MediaTypeResolver mediaTypeResolver;
    @Mock private GameService gameService;

    @InjectMocks private ProfileDataController controller;

    private MockMvc mockMvc;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        testUser = TestDataBuilder.createRegularUser();
    }

    @Test
    @DisplayName("GET /profile-data - Should return profile data")
    void shouldReturnProfileData() throws Exception {
        when(userService.getUser("testuser")).thenReturn(testUser);
        when(securityUtil.getCurrentUsername()).thenReturn("testuser");
        when(accessService.canViewProfile(any(User.class), anyString())).thenReturn(true);
        when(mediaTypeResolver.resolve("games")).thenReturn((AbstractMediaService) gameService);

        List<Game> games = TestDataBuilder.createGames(5, testUser.getId());
        PageResponse<Game> pageResponse = TestDataBuilder.createPageResponse(games, 1, 15);

        when(gameService.getUserItemsPageAsDto(
                        anyInt(), eq(1), eq(15), isNull(), isNull(), eq("name"), eq("asc")))
                .thenReturn((PageResponse) pageResponse);

        mockMvc.perform(
                        get("/profile-data")
                                .param("username", "testuser")
                                .param("category", "games")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(5));

        verify(gameService)
                .getUserItemsPageAsDto(
                        anyInt(), eq(1), eq(15), isNull(), isNull(), eq("name"), eq("asc"));
    }

    @Test
    @DisplayName("GET /profile-data - Should deny access to private profile")
    void shouldDenyAccessToPrivateProfile() throws Exception {
        when(userService.getUser("privateuser")).thenReturn(testUser);
        when(securityUtil.getCurrentUsername()).thenReturn("otheruser");
        when(accessService.canViewProfile(any(User.class), anyString())).thenReturn(false);

        mockMvc.perform(
                        get("/profile-data")
                                .param("username", "privateuser")
                                .param("category", "games")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(accessService).canViewProfile(any(User.class), eq("otheruser"));
    }

    @Test
    @DisplayName("GET /profile-data - Should return 400 for invalid category")
    void shouldReturn400ForInvalidCategory() throws Exception {
        when(userService.getUser("testuser")).thenReturn(testUser);
        when(securityUtil.getCurrentUsername()).thenReturn("testuser");
        when(accessService.canViewProfile(any(User.class), anyString())).thenReturn(true);
        when(mediaTypeResolver.resolve("invalid"))
                .thenThrow(new IllegalArgumentException("Invalid category"));

        mockMvc.perform(
                        get("/profile-data")
                                .param("username", "testuser")
                                .param("category", "invalid")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /profile-data - Should pass genre_id for movies instead of tag_id")
    void shouldPassGenreIdForMovies() throws Exception {
        when(userService.getUser("testuser")).thenReturn(testUser);
        when(securityUtil.getCurrentUsername()).thenReturn("testuser");
        when(accessService.canViewProfile(any(User.class), anyString())).thenReturn(true);

        AbstractMediaService movieService = Mockito.mock(AbstractMediaService.class);
        when(mediaTypeResolver.resolve("movies")).thenReturn(movieService);

        PageResponse emptyPage = new PageResponse<>(List.of(), 1, 1, 0L, 15);

        when(movieService.getUserItemsPageAsDto(
                        anyInt(), eq(1), eq(15), eq(10), eq("batman"), eq("score"), eq("desc")))
                .thenReturn(emptyPage);

        mockMvc.perform(
                        get("/profile-data")
                                .param("username", "testuser")
                                .param("category", "movies")
                                .param("genre_id", "10")
                                .param("search", "batman")
                                .param("sortBy", "score")
                                .param("sortOrder", "desc")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(movieService)
                .getUserItemsPageAsDto(
                        anyInt(), eq(1), eq(15), eq(10), eq("batman"), eq("score"), eq("desc"));
    }

    @Test
    @DisplayName("GET /profile-data - Should return 500 on internal exception")
    void shouldReturn500OnInternalException() throws Exception {
        when(userService.getUser("testuser")).thenThrow(new RuntimeException("Database down!"));

        mockMvc.perform(
                        get("/profile-data")
                                .param("username", "testuser")
                                .param("category", "games")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to fetch profile data"));
    }
}
