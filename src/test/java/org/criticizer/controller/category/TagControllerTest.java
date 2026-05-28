package org.criticizer.controller.category;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.criticizer.dto.tag.CreateTagRequest;
import org.criticizer.dto.tag.TagResponse;
import org.criticizer.dto.tag.UpdateTagRequest;
import org.criticizer.service.tag.TagService;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("TagController Tests")
class TagControllerTest {

    @Mock private TagService tagService;

    @InjectMocks private TagController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GET /api/tags - Should return all tags")
    void shouldReturnAllTags() throws Exception {
        // Given
        when(tagService.getAllTags())
                .thenReturn(List.of(new TagResponse(1, "RPG"), new TagResponse(2, "Action")));

        // When & Then
        mockMvc.perform(get("/api/tags").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(tagService).getAllTags();
    }

    @Test
    @DisplayName("POST /api/tags - Should create tag")
    void shouldCreateTag() throws Exception {
        // Given
        CreateTagRequest request = new CreateTagRequest("RPG");
        TagResponse response = new TagResponse(1, "RPG");
        when(tagService.createTag(any(CreateTagRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(
                        post("/api/tags")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("RPG"));

        verify(tagService).createTag(any(CreateTagRequest.class));
    }

    @Test
    @DisplayName("PUT /api/tags/{id} - Should update tag")
    void shouldUpdateTag() throws Exception {
        // Given
        UpdateTagRequest request = new UpdateTagRequest(1, "Updated RPG");
        TagResponse response = new TagResponse(1, "Updated RPG");
        when(tagService.updateTag(any(UpdateTagRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(
                        put("/api/tags/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated RPG"));

        verify(tagService).updateTag(any(UpdateTagRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/tags/{id} - Should delete tag")
    void shouldDeleteTag() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/tags/1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Tag deleted successfully"));

        verify(tagService).deleteTag(1);
    }

    @Test
    @DisplayName("GET /api/tags/{id}/in-use - Should check if tag is in use")
    void shouldCheckIfTagIsInUse() throws Exception {
        // Given
        when(tagService.isTagInUse(1)).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/tags/1/in-use").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

        verify(tagService).isTagInUse(1);
    }
}
