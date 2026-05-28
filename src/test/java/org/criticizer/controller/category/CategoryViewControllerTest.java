package org.criticizer.controller.category;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.criticizer.constants.ContentCategory;
import org.criticizer.dto.helper.PageResponse;
import org.criticizer.entity.Game;
import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.game.GameService;
import org.criticizer.service.genre.GenreService;
import org.criticizer.service.helper.MediaTypeResolver;
import org.criticizer.service.tag.TagService;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryViewController Tests")
class CategoryViewControllerTest {

    @Mock private MediaTypeResolver mediaTypeResolver;

    @Mock private TagService tagService;

    @Mock private GenreService genreService;

    @Mock private SecurityUtil securityUtil;

    @Mock private GameService gameService;

    @InjectMocks private CategoryViewController controller;

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
    @DisplayName("GET /games - Should load games page")
    void shouldLoadGamesPage() throws Exception {
        // Given
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        doReturn(gameService).when(mediaTypeResolver).resolve(ContentCategory.GAMES);
        when(tagService.getAllTags()).thenReturn(List.of());

        List<Game> games = TestDataBuilder.createGames(5, testUser.getId());
        PageResponse<Game> pageResponse = TestDataBuilder.createPageResponse(games, 1, 10);
        when(gameService.getUserItemsPage(
                        anyInt(), eq(1), eq(10), isNull(), isNull(), eq("name"), eq("asc")))
                .thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/games"))
                .andExpect(status().isOk())
                .andExpect(view().name("category/listRatingsTemplate"))
                .andExpect(model().attribute("entityType", "games"))
                .andExpect(model().attributeExists("initialData"))
                .andExpect(model().attributeExists("allTags"));

        verify(tagService).getAllTags();
        verify(gameService)
                .getUserItemsPage(
                        anyInt(), eq(1), eq(10), isNull(), isNull(), eq("name"), eq("asc"));
    }

    @Test
    @DisplayName("GET /movies - Should load movies page")
    void shouldLoadMoviesPage() throws Exception {
        // Given
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        doReturn(gameService).when(mediaTypeResolver).resolve(ContentCategory.MOVIES);
        when(genreService.getAvailableGenresFor("movie")).thenReturn(List.of());

        PageResponse<Game> pageResponse = TestDataBuilder.createEmptyPageResponse(1, 10);
        when(gameService.getUserItemsPage(
                        anyInt(), eq(1), eq(10), isNull(), isNull(), eq("name"), eq("asc")))
                .thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/movies"))
                .andExpect(status().isOk())
                .andExpect(view().name("category/listRatingsTemplate"))
                .andExpect(model().attribute("entityType", "movies"))
                .andExpect(model().attributeExists("allGenres"));

        verify(genreService).getAvailableGenresFor("movie");
    }

    @Test
    @DisplayName("GET /books - Should load books page")
    void shouldLoadBooksPage() throws Exception {
        // Given
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        doReturn(gameService).when(mediaTypeResolver).resolve(ContentCategory.BOOKS);
        when(genreService.getAvailableGenresFor("book")).thenReturn(List.of());

        PageResponse<Game> pageResponse = TestDataBuilder.createEmptyPageResponse(1, 10);
        when(gameService.getUserItemsPage(
                        anyInt(), eq(1), eq(10), isNull(), isNull(), eq("name"), eq("asc")))
                .thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(view().name("category/listRatingsTemplate"));

        verify(genreService).getAvailableGenresFor("book");
    }

    @Test
    @DisplayName("GET /shows - Should load shows page")
    void shouldLoadShowsPage() throws Exception {
        // Given
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        doReturn(gameService).when(mediaTypeResolver).resolve(ContentCategory.SHOWS);
        when(genreService.getAvailableGenresFor("show")).thenReturn(List.of());

        PageResponse<Game> pageResponse = TestDataBuilder.createEmptyPageResponse(1, 10);
        when(gameService.getUserItemsPage(
                        anyInt(), eq(1), eq(10), isNull(), isNull(), eq("name"), eq("asc")))
                .thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/shows"))
                .andExpect(status().isOk())
                .andExpect(view().name("category/listRatingsTemplate"));

        verify(genreService).getAvailableGenresFor("show");
    }
}
