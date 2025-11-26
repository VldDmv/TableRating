package org.criticizer.servlets;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.criticizer.entity.Role;
import org.criticizer.entity.User;
import org.criticizer.exceptions.data.UserNotFoundException;
import org.criticizer.service.book.BookService;
import org.criticizer.service.game.GameService;
import org.criticizer.service.movie.MovieService;
import org.criticizer.service.show.ShowService;
import org.criticizer.service.user.UserPageResult;
import org.criticizer.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class ProfileServletTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private ServletConfig config;
    @Mock private ServletContext context;
    @Mock private RequestDispatcher dispatcher;
    @Mock private UserService userService;
    @Mock private GameService gameService;
    @Mock private MovieService movieService;
    @Mock private BookService bookService;
    @Mock private ShowService showService;



    @InjectMocks private ProfileServlet profileServlet;

    private User profileOwner;
    private User viewer;

    @BeforeEach
    void setUp() throws ServletException {
        MockitoAnnotations.openMocks(this);
        when(config.getServletContext()).thenReturn(context);
        when(context.getAttribute("userService")).thenReturn(userService);
        when(context.getAttribute("gameService")).thenReturn(gameService);


        when(context.getAttribute("movieService")).thenReturn(movieService);
        when(context.getAttribute("bookService")).thenReturn(bookService);
        when(context.getAttribute("showService")).thenReturn(showService);


        profileServlet.init(config);

        profileOwner = new User(1, "owner", "pass", Role.USER, true);
        viewer = new User(2, "viewer", "pass", Role.USER, true);
    }

    @Test
    void testDoGet_UserNotFound_Sends404Error() throws ServletException, IOException {
        when(request.getParameter("username")).thenReturn("nonexistent");
        when(userService.getUser("nonexistent")).thenThrow(new UserNotFoundException("nonexistent"));

        profileServlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "User not found");

    }

    @Test
    void testDoGet_ViewingOwnProfile_SetsAttributesCorrectly() throws ServletException, IOException {
        when(request.getParameter("username")).thenReturn("owner");
        when(userService.getUser("owner")).thenReturn(profileOwner);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(profileOwner);
        when(request.getRequestDispatcher("/WEB-INF/templates/profile.jsp")).thenReturn(dispatcher);

        when(gameService.getUserGamesPage(profileOwner.getId(), 1, 15, null, null,null,null)).thenReturn(new UserPageResult<>(null,0,1,15));

        profileServlet.doGet(request, response);

        verify(request).setAttribute("isOwnerViewing", true);
        verify(request).setAttribute("canView", true);
        verify(request).setAttribute(eq("pageResult"), any());
        verify(dispatcher).forward(request, response);
    }

    @Test
    void testDoPost_UpdatePrivacy_CallsServiceAndRedirects() throws ServletException, IOException {
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(viewer);
        when(request.getParameter("privacy")).thenReturn("public");
        when(request.getHeader("Referer")).thenReturn("somePage");

        profileServlet.doPost(request, response);

        verify(userService).updateUserPrivacy(viewer.getId(), true);
        verify(session).setAttribute(eq("user"), any(User.class));
        verify(response).sendRedirect("somePage");
    }
}