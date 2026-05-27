package org.criticizer.controller.category;

import org.criticizer.constants.ContentCategory;
import org.criticizer.dto.helper.PageResponse;
import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.helper.AbstractMediaService;
import org.criticizer.service.helper.MediaTypeResolver;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryDataController Tests")
class CategoryDataControllerTest {

    @Mock
    private MediaTypeResolver mediaTypeResolver;

    @Mock
    private AbstractMediaService mediaServiceMock;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private CategoryDataController controller;

    private MockMvc mockMvc;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        testUser = TestDataBuilder.createRegularUser();
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("GET /api/category/games - Should return games data")
    void shouldReturnGamesData() throws Exception {
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        when(mediaTypeResolver.resolve(ContentCategory.GAMES)).thenReturn(mediaServiceMock);

        PageResponse pageResponse = TestDataBuilder.createPageResponse(Collections.emptyList(), 1, 10);

        when(mediaServiceMock.getUserItemsPageAsDto(
                eq(testUser.getId()), eq(1), eq(10), isNull(), isNull(), eq("name"), eq("asc"), isNull(), isNull(), isNull()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/category/games")
                        .param("page", "1")
                        .param("rows", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(mediaServiceMock).getUserItemsPageAsDto(
                eq(testUser.getId()), eq(1), eq(10), isNull(), isNull(), eq("name"), eq("asc"), isNull(), isNull(), isNull());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("GET /api/category/games - Should support tag filtering")
    void shouldSupportTagFiltering() throws Exception {
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        when(mediaTypeResolver.resolve(ContentCategory.GAMES)).thenReturn(mediaServiceMock);

        PageResponse pageResponse = TestDataBuilder.createEmptyPageResponse(1, 10);
        when(mediaServiceMock.getUserItemsPageAsDto(
                eq(testUser.getId()), eq(1), eq(10), eq(1), isNull(), eq("name"), eq("asc"), isNull(), isNull(), isNull()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/category/games")
                        .param("page", "1")
                        .param("rows", "10")
                        .param("categoryId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(mediaServiceMock).getUserItemsPageAsDto(
                eq(testUser.getId()), eq(1), eq(10), eq(1), isNull(), eq("name"), eq("asc"), isNull(), isNull(), isNull());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("GET /api/category/movies - Should return movies data")
    void shouldReturnMoviesData() throws Exception {
        when(securityUtil.getCurrentUser()).thenReturn(testUser);
        when(mediaTypeResolver.resolve(ContentCategory.MOVIES)).thenReturn(mediaServiceMock);

        PageResponse pageResponse = TestDataBuilder.createEmptyPageResponse(1, 10);
        when(mediaServiceMock.getUserItemsPageAsDto(anyInt(), eq(1), eq(10), isNull(), isNull(), eq("name"), eq("asc"), isNull(), isNull(), isNull()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/category/movies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(mediaServiceMock).getUserItemsPageAsDto(anyInt(), eq(1), eq(10), isNull(), isNull(), eq("name"), eq("asc"), isNull(), isNull(), isNull());
    }
}