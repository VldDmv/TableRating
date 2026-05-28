package org.criticizer.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.criticizer.dto.tag.CreateTagRequest;
import org.criticizer.dto.tag.UpdateTagRequest;
import org.criticizer.exceptions.data.ItemInUseException;
import org.criticizer.service.tag.TagService;
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
@DisplayName("AdminTagController Tests")
class AdminTagControllerTest {

    @Mock private TagService tagService;

    @InjectMocks private AdminTagController controller;

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
    @DisplayName("POST /admin/tags?action=add - Should create tag successfully")
    void shouldCreateTag() throws Exception {
        mockMvc.perform(post("/admin/tags").param("action", "add").param("name", "RPG"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?type=tags"))
                .andExpect(flash().attributeExists("flashSuccessMessage"));

        verify(tagService).createTag(any(CreateTagRequest.class));
    }

    @Test
    @DisplayName("POST /admin/tags?action=update - Should update tag successfully")
    void shouldUpdateTag() throws Exception {
        mockMvc.perform(
                        post("/admin/tags")
                                .param("action", "update")
                                .param("id", "1")
                                .param("name", "Updated RPG"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?type=tags"))
                .andExpect(flash().attributeExists("flashSuccessMessage"));

        verify(tagService).updateTag(any(UpdateTagRequest.class));
    }

    @Test
    @DisplayName("POST /admin/tags?action=delete - Should handle tag in use")
    void shouldHandleTagInUse() throws Exception {
        // Given
        doThrow(new ItemInUseException("Tag in use")).when(tagService).deleteTag(1);

        // When & Then
        mockMvc.perform(post("/admin/tags").param("action", "delete").param("id", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?type=tags"))
                .andExpect(flash().attributeExists("flashErrorMessage"));

        verify(tagService).deleteTag(1);
    }

    @Test
    @DisplayName("POST /admin/tags - Should handle add action")
    void shouldHandleAddAction() throws Exception {
        // Given
        doReturn(null).when(tagService).createTag(any(CreateTagRequest.class));

        // When & Then
        mockMvc.perform(post("/admin/tags").param("action", "add").param("name", "Strategy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?type=tags"))
                .andExpect(flash().attributeExists("flashSuccessMessage"));

        verify(tagService).createTag(any(CreateTagRequest.class));
    }

    @Test
    @DisplayName("POST /admin/tags - Should handle update action")
    void shouldHandleUpdateAction() throws Exception {
        // Given
        doReturn(null).when(tagService).updateTag(any(UpdateTagRequest.class));

        // When & Then
        mockMvc.perform(
                        post("/admin/tags")
                                .param("action", "update")
                                .param("id", "1")
                                .param("name", "Updated Strategy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?type=tags"))
                .andExpect(flash().attributeExists("flashSuccessMessage"));

        verify(tagService).updateTag(any(UpdateTagRequest.class));
    }

    @Test
    @DisplayName("POST /admin/tags - Should handle delete action")
    void shouldHandleDeleteAction() throws Exception {
        // Given
        doNothing().when(tagService).deleteTag(1);

        // When & Then
        mockMvc.perform(post("/admin/tags").param("action", "delete").param("id", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?type=tags"))
                .andExpect(flash().attributeExists("flashSuccessMessage"));

        verify(tagService).deleteTag(1);
    }

    @Test
    @DisplayName("POST /admin/tags - Should handle invalid action")
    void shouldHandleInvalidAction() throws Exception {
        // When & Then
        mockMvc.perform(post("/admin/tags").param("action", "invalid"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?type=tags"))
                .andExpect(flash().attributeExists("flashErrorMessage"));

        verifyNoInteractions(tagService);
    }

    @Test
    @DisplayName("POST /admin/tags?action=add - Should handle service error")
    void shouldHandleServiceError() throws Exception {
        // Given
        doThrow(new RuntimeException("Database error"))
                .when(tagService)
                .createTag(any(CreateTagRequest.class));

        // When & Then
        mockMvc.perform(post("/admin/tags").param("action", "add").param("name", "RPG"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/management?type=tags"))
                .andExpect(flash().attributeExists("flashErrorMessage"));

        verify(tagService).createTag(any(CreateTagRequest.class));
    }
}
