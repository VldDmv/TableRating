package org.criticizer.service;

import org.criticizer.dao.tag.TagDao;
import org.criticizer.entity.Tag;
import org.criticizer.exceptions.data.ItemInUseException;
import org.criticizer.exceptions.validation.EmptyNameException;
import org.criticizer.service.tag.TagServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    private TagDao tagDaoMock;

    @InjectMocks
    private TagServiceImpl tagService;

    @Test
    @DisplayName("createTag should call dao.addTag with trimmed name")
    void createTag_withValidName_shouldSucceed() {
        String tagName = "  Action RPG  ";
        String expectedTagName = "Action RPG";

        tagService.createTag(tagName);

        verify(tagDaoMock, times(1)).addTag(expectedTagName);
    }

    @Test
    @DisplayName("createTag should throw exception for null or empty name")
    void createTag_withInvalidName_shouldThrowException() {
        assertThrows(EmptyNameException.class, () -> tagService.createTag(null));
        assertThrows(EmptyNameException.class, () -> tagService.createTag("   "));

        verify(tagDaoMock, never()).addTag(anyString());
    }

    @Test
    @DisplayName("editTag should call dao.updateTag with trimmed name")
    void editTag_withValidName_shouldSucceed() {
        int tagId = 1;
        String newTagName = "  Updated Tag  ";
        String expectedNewTagName = "Updated Tag";

        tagService.editTag(tagId, newTagName);

        verify(tagDaoMock, times(1)).updateTag(tagId, expectedNewTagName);
    }

    @Test
    @DisplayName("editTag should throw exception for null or empty new name")
    void editTag_withInvalidName_shouldThrowException() {
        int tagId = 1;
        assertThrows(EmptyNameException.class, () -> tagService.editTag(tagId, null));
        assertThrows(EmptyNameException.class, () -> tagService.editTag(tagId, "   "));
        verify(tagDaoMock, never()).updateTag(anyInt(), anyString());
    }

    @Test
    @DisplayName("removeTag should call dao.deleteTag when tag is not in use")
    void removeTag_whenNotInUse_shouldSucceed() {
        int tagId = 1;
        when(tagDaoMock.isTagInUse(tagId)).thenReturn(false);

        tagService.removeTag(tagId);

        verify(tagDaoMock, times(1)).isTagInUse(tagId);
        verify(tagDaoMock, times(1)).deleteTag(tagId);
    }

    @Test
    @DisplayName("removeTag should throw exception when tag is in use")
    void removeTag_whenInUse_shouldThrowException() {
        int tagId = 1;
        when(tagDaoMock.isTagInUse(tagId)).thenReturn(true);

        Exception exception = assertThrows(ItemInUseException.class, () -> tagService.removeTag(tagId));
        assertEquals("Operation not permitted: deletetag - Cannot delete tag: it is currently assigned to one or more games", exception.getMessage());

        verify(tagDaoMock, times(1)).isTagInUse(tagId);
        verify(tagDaoMock, never()).deleteTag(tagId);
    }

    @Test
    @DisplayName("getAllTags should return the list from dao")
    void getAllTags_shouldReturnListFromDao() {
        List<Tag> expectedTags = List.of(new Tag(1, "RPG"), new Tag(2, "Action"));
        when(tagDaoMock.getAllTags()).thenReturn(expectedTags);

        List<Tag> actualTags = tagService.getAllTags();

        assertSame(expectedTags, actualTags, "Should return the exact list instance from DAO.");
        verify(tagDaoMock, times(1)).getAllTags();
    }

    @Test
    @DisplayName("getTagsForGame should return the list from dao")
    void getTagsForGame_shouldReturnListFromDao() {
        int gameId = 1;
        List<Tag> expectedTags = List.of(new Tag(1, "RPG"));
        when(tagDaoMock.getTagsForGame(gameId)).thenReturn(expectedTags);

        List<Tag> actualTags = tagService.getTagsForGame(gameId);

        assertSame(expectedTags, actualTags, "Should return the exact list instance from DAO for the given gameId.");
        verify(tagDaoMock, times(1)).getTagsForGame(gameId);
    }
}