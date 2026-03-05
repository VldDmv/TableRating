package org.criticizer.controller.admin;

import org.criticizer.dto.genre.CreateGenreRequest;
import org.criticizer.dto.genre.UpdateGenreRequest;
import org.criticizer.exceptions.data.ItemInUseException;
import org.criticizer.service.genre.GenreService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminGenreController Tests")
class AdminGenreControllerTest {

    @Mock
    private GenreService genreService;

    @InjectMocks
    private AdminGenreController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();
    }


    @Test
    @DisplayName("POST /admin/genres (action=add) - Should create genre successfully")
    void shouldCreateGenre() throws Exception {
        doReturn(null).when(genreService).createGenre(any(CreateGenreRequest.class));

        mockMvc.perform(post("/admin/genres")
                        .param("action", "add")
                        .param("name", "Action")
                        .param("mediaTypes", "movie", "show"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?type=genres"))
                .andExpect(flash().attributeExists("flashSuccessMessage"));

        verify(genreService).createGenre(any(CreateGenreRequest.class));
    }

    @Test
    @DisplayName("POST /admin/genres (action=update) - Should update genre successfully")
    void shouldUpdateGenre() throws Exception {
        doReturn(null).when(genreService).updateGenre(any(UpdateGenreRequest.class));

        mockMvc.perform(post("/admin/genres")
                        .param("action", "update")
                        .param("id", "1")
                        .param("name", "Updated Action")
                        .param("mediaTypes", "movie"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?type=genres"))
                .andExpect(flash().attributeExists("flashSuccessMessage"));

        verify(genreService).updateGenre(any(UpdateGenreRequest.class));
    }

    @Test
    @DisplayName("POST /admin/genres (action=delete) - Should delete genre successfully")
    void shouldDeleteGenre() throws Exception {
        doNothing().when(genreService).deleteGenre(1);

        mockMvc.perform(post("/admin/genres")
                        .param("action", "delete")
                        .param("id", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?type=genres"))
                .andExpect(flash().attributeExists("flashSuccessMessage"));

        verify(genreService).deleteGenre(1);
    }

    @Test
    @DisplayName("POST /admin/genres (action=delete) - Should handle genre in use")
    void shouldHandleGenreInUse() throws Exception {
        doThrow(new ItemInUseException("Genre in use"))
                .when(genreService).deleteGenre(1);

        mockMvc.perform(post("/admin/genres")
                        .param("action", "delete")
                        .param("id", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?type=genres"))
                .andExpect(flash().attributeExists("flashErrorMessage"));

        verify(genreService).deleteGenre(1);
    }

    @Test
    @DisplayName("POST /admin/genres (action=add) - Should handle add action")
    void shouldHandleAddAction() throws Exception {
        doReturn(null).when(genreService).createGenre(any(CreateGenreRequest.class));

        mockMvc.perform(post("/admin/genres")
                        .param("action", "add")
                        .param("name", "Comedy")
                        .param("mediaTypes", "movie", "show"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?type=genres"))
                .andExpect(flash().attributeExists("flashSuccessMessage"));

        verify(genreService).createGenre(any(CreateGenreRequest.class));
    }

    @Test
    @DisplayName("POST /admin/genres (action=update) - Should handle update action")
    void shouldHandleUpdateAction() throws Exception {
        doReturn(null).when(genreService).updateGenre(any(UpdateGenreRequest.class));

        mockMvc.perform(post("/admin/genres")
                        .param("action", "update")
                        .param("id", "1")
                        .param("name", "Updated Comedy")
                        .param("mediaTypes", "movie"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?type=genres"))
                .andExpect(flash().attributeExists("flashSuccessMessage"));

        verify(genreService).updateGenre(any(UpdateGenreRequest.class));
    }

    @Test
    @DisplayName("POST /admin/genres (action=delete) - Should handle delete action")
    void shouldHandleDeleteAction() throws Exception {
        doNothing().when(genreService).deleteGenre(1);

        mockMvc.perform(post("/admin/genres")
                        .param("action", "delete")
                        .param("id", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?type=genres"))
                .andExpect(flash().attributeExists("flashSuccessMessage"));

        verify(genreService).deleteGenre(1);
    }

    @Test
    @DisplayName("POST /admin/genres - Should handle invalid action")
    void shouldHandleInvalidAction() throws Exception {
        mockMvc.perform(post("/admin/genres")
                        .param("action", "invalid"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?type=genres"))
                .andExpect(flash().attributeExists("flashErrorMessage"));

        verifyNoInteractions(genreService);
    }
}