package org.criticizer.servlets;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.criticizer.service.genre.GenreService;
import org.criticizer.service.tag.TagService;
import org.criticizer.servlets.admin.AdminManagementServlet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminManagementServletTest {

    @Mock
    private TagService tagServiceMock;
    @Mock
    private GenreService genreServiceMock;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private ServletConfig servletConfig;
    @Mock
    private ServletContext servletContext;
    @Mock
    private RequestDispatcher requestDispatcher;

    @InjectMocks
    private AdminManagementServlet adminManagementServlet;

    @BeforeEach
    void setUp() throws Exception {

        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getAttribute("tagService")).thenReturn(tagServiceMock);
        when(servletContext.getAttribute("genreService")).thenReturn(genreServiceMock);
        adminManagementServlet.init(servletConfig);


        lenient().when(request.getSession()).thenReturn(session);
        lenient().when(request.getContextPath()).thenReturn("/app");
        lenient().when(request.getRequestDispatcher(anyString())).thenReturn(requestDispatcher);
    }

    @Test
    @DisplayName("doGet should fetch tags and forward to management JSP")
    void doGet_withTypeTags_shouldFetchTagsAndForward() throws Exception {
        when(request.getParameter("type")).thenReturn("tags");

        adminManagementServlet.doGet(request, response);

        verify(tagServiceMock).getAllTags();
        verify(request).setAttribute(eq("items"), anyList());
        verify(request).setAttribute("itemType", "Tag");
        verify(requestDispatcher).forward(request, response);
    }

    @Test
    @DisplayName("doGet should fetch genres and forward to management JSP")
    void doGet_withTypeGenres_shouldFetchGenresAndForward() throws Exception {
        when(request.getParameter("type")).thenReturn("genres");

        adminManagementServlet.doGet(request, response);

        verify(genreServiceMock).getAllGenres();
        verify(request).setAttribute(eq("items"), anyList());
        verify(request).setAttribute("itemType", "Genre");
        verify(requestDispatcher).forward(request, response);
    }

    @Test
    @DisplayName("doPost should add a new tag")
    void doPost_withActionAddTag_shouldCallCreateTag() throws Exception {
        when(request.getParameter("type")).thenReturn("tags");
        when(request.getParameter("action")).thenReturn("add");
        when(request.getParameter("name")).thenReturn("New Tag");

        adminManagementServlet.doPost(request, response);

        verify(tagServiceMock).createTag("New Tag");
        verify(session).setAttribute("flashSuccessMessage", "Operation successful!");
        verify(response).sendRedirect("/app/admin/management?type=tags");
    }

    @Test
    @DisplayName("doPost should update an existing genre")
    void doPost_withActionUpdateGenre_shouldCallEditGenre() throws Exception {
        when(request.getParameter("type")).thenReturn("genres");
        when(request.getParameter("action")).thenReturn("update");
        when(request.getParameter("id")).thenReturn("123");
        when(request.getParameter("name")).thenReturn("Updated Genre");
        when(request.getParameterValues("mediaTypes")).thenReturn(new String[]{"movie", "book"});

        adminManagementServlet.doPost(request, response);

        verify(genreServiceMock).editGenre(123, "Updated Genre", List.of("movie", "book"));
        verify(session).setAttribute("flashSuccessMessage", "Operation successful!");
        verify(response).sendRedirect("/app/admin/management?type=genres");
    }

    @Test
    @DisplayName("doPost should delete a tag")
    void doPost_withActionDeleteTag_shouldCallRemoveTag() throws Exception {
        when(request.getParameter("type")).thenReturn("tags");
        when(request.getParameter("action")).thenReturn("delete");
        when(request.getParameter("id")).thenReturn("456");


        adminManagementServlet.doPost(request, response);

        verify(tagServiceMock).removeTag(456);
        verify(session).setAttribute("flashSuccessMessage", "Operation successful!");
        verify(response).sendRedirect("/app/admin/management?type=tags");
    }

    @Test
    @DisplayName("doPost should handle exceptions and set error message")
    void doPost_whenServiceThrowsException_shouldSetErrorMessage() throws Exception {
        when(request.getParameter("type")).thenReturn("tags");
        when(request.getParameter("action")).thenReturn("add");
        when(request.getParameter("name")).thenReturn("FailingTag");

        doThrow(new IllegalArgumentException("Tag name cannot be empty."))
                .when(tagServiceMock).createTag("FailingTag");

        adminManagementServlet.doPost(request, response);

        verify(session).setAttribute("flashErrorMessage", "Operation failed. Please try again.");


        verify(response).sendRedirect("/app/admin/management?type=tags");
    }
}