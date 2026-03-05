package org.criticizer.controller.category;

import org.criticizer.dto.show.ShowResponse;
import org.criticizer.entity.Show;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.show.ShowService;
import org.criticizer.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShowController Tests")
class ShowControllerTest {

    @Mock
    private ShowService showService;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private ShowController controller;

    @Test
    @DisplayName("Should return correct entity name")
    void shouldReturnCorrectEntityName() {
        assertEquals("Show", controller.getEntityName());
    }

    @Test
    @DisplayName("Should convert Show to ShowResponse")
    void shouldConvertShowToResponse() {
        // Given
        Show show = TestDataBuilder.createShow(1, "Test Show", 1, 75);

        // When
        ShowResponse response = controller.convertToResponse(show);

        // Then
        assertEquals(show.getId(), response.getId());
        assertEquals(show.getName(), response.getName());
    }
}
