package org.criticizer.controller.profile;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.criticizer.dto.helper.PageResponse;
import org.criticizer.entity.Game;
import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.game.GameService;
import org.criticizer.service.genre.GenreService;
import org.criticizer.service.profile.ProfileAccessService;
import org.criticizer.service.tag.TagService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileViewController Tests")
class ProfileViewControllerTest {

    @Mock private UserService userService;

    @Mock private GameService gameService;

    @Mock private SecurityUtil securityUtil;

    @Mock private ProfileAccessService accessService;
    @Mock private TagService tagService;

    @Mock private GenreService genreService;
    @InjectMocks private ProfileViewController controller;

    private MockMvc mockMvc;
    private User testUser;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");

        mockMvc =
                MockMvcBuilders.standaloneSetup(controller).setViewResolvers(viewResolver).build();

        testUser = TestDataBuilder.createRegularUser();
    }

    @Test
    @DisplayName("GET /profile - Should load profile page")
    void shouldLoadProfilePage() throws Exception {
        // Given
        when(userService.getUser("testuser")).thenReturn(testUser);
        when(securityUtil.getCurrentUsername()).thenReturn("testuser");

        ProfileAccessService.ProfileAccessContext context =
                new ProfileAccessService.ProfileAccessContext(true, true, testUser);
        when(accessService.checkAccess(any(User.class), anyString())).thenReturn(context);

        // Bypass TestDataBuilder to avoid protected access reflection errors
        List<Game> games =
                Stream.generate(() -> Mockito.mock(Game.class))
                        .limit(5)
                        .collect(Collectors.toList());

        PageResponse<Game> pageResponse = TestDataBuilder.createPageResponse(games, 1, 10);
        when(gameService.getUserItemsPage(
                        anyInt(), eq(1), eq(10), isNull(), isNull(), eq("name"), eq("asc")))
                .thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/profile").param("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attribute("isOwnerViewing", true))
                .andExpect(model().attribute("canView", true))
                .andExpect(model().attributeExists("initialData"));

        verify(userService).getUser("testuser");
        verify(gameService)
                .getUserItemsPage(
                        anyInt(), eq(1), eq(10), isNull(), isNull(), eq("name"), eq("asc"));
    }

    @Test
    @DisplayName("POST /profile - Should update privacy settings")
    void shouldUpdatePrivacySettings() throws Exception {
        // Given
        when(securityUtil.getCurrentUser()).thenReturn(testUser);

        // When & Then
        mockMvc.perform(post("/profile").param("privacy", "public"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?username=testuser"))
                .andExpect(flash().attributeExists("flashSuccessMessage"));

        verify(userService).updateUserPrivacy(testUser.getId(), true);
    }
}
