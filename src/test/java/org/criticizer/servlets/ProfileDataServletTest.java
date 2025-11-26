package org.criticizer.servlets;

import com.google.gson.Gson;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.criticizer.entity.Game;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ProfileDataServletTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private ServletConfig config;
    @Mock
    private ServletContext context;

    @Mock
    private UserService userService;
    @Mock
    private GameService gameService;
    @Mock
    private MovieService movieService;
    @Mock
    private BookService bookService;
    @Mock
    private ShowService showService;

    @InjectMocks
    private ProfileDataServlet profileDataServlet;

    private User profileOwner;
    private StringWriter stringWriter;
    private PrintWriter printWriter;
    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() throws ServletException, IOException {
        MockitoAnnotations.openMocks(this);

        when(config.getServletContext()).thenReturn(context);
        when(context.getAttribute("userService")).thenReturn(userService);
        when(context.getAttribute("gameService")).thenReturn(gameService);
        when(context.getAttribute("movieService")).thenReturn(movieService);
        when(context.getAttribute("bookService")).thenReturn(bookService);
        when(context.getAttribute("showService")).thenReturn(showService);

        profileDataServlet.init(config);

        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);


        when(request.getSession()).thenReturn(session);

        profileOwner = new User(1, "owner", "pass", Role.USER, true);
    }

    @Test
    @DisplayName("GET Games: Should retrieve game page and write JSON response")
    void doGet_GamesCategory_ReturnsJson() throws ServletException, IOException {
        String username = "owner";
        String category = "games";
        UserPageResult<Game> mockResult = new UserPageResult<>(
                List.of(new Game(1, "Elden Ring", 1, 95, true)),
                1, 1, 15
        );

        when(request.getParameter("user")).thenReturn(username);
        when(request.getParameter("category")).thenReturn(category);
        when(request.getParameter("page")).thenReturn("1");

        when(userService.getUser(username)).thenReturn(profileOwner);
        when(gameService.getUserGamesPage(anyInt(), anyInt(), anyInt(), any(), any(), any(), any()))
                .thenReturn(mockResult);

        profileDataServlet.doGet(request, response);

        verify(userService).getUser(username);
        verify(gameService).getUserGamesPage(eq(1), eq(1), eq(15), isNull(), isNull(), isNull(), isNull());
        verifyNoInteractions(movieService, bookService, showService);

        printWriter.flush();
        String jsonOutput = stringWriter.toString();
        assertTrue(jsonOutput.contains("Elden Ring"), "Response should contain game name");
        assertTrue(jsonOutput.contains("\"totalItems\":1"), "Response should contain pagination info");
    }

    @Test
    @DisplayName("GET Movies: Should retrieve movie page and write JSON response")
    void doGet_MoviesCategory_ReturnsJson() throws ServletException, IOException {
        String username = "owner";
        String category = "movies";

        when(request.getParameter("user")).thenReturn(username);
        when(request.getParameter("category")).thenReturn(category);
        when(userService.getUser(username)).thenReturn(profileOwner);

        profileDataServlet.doGet(request, response);

        verify(movieService).getUserMoviesPage(eq(1), anyInt(), anyInt(), isNull(), isNull(), isNull(), isNull());
        verifyNoInteractions(gameService);
    }

    @Test
    @DisplayName("invalid Category: Should catch InvalidInputException and delegate to error handler")
    void doGet_InvalidCategory_ShouldHandleError() throws ServletException, IOException {
        String username = "owner";
        String category = "music";

        when(request.getParameter("user")).thenReturn(username);
        when(request.getParameter("category")).thenReturn(category);
        when(userService.getUser(username)).thenReturn(profileOwner);

        profileDataServlet.doGet(request, response);

        verifyNoInteractions(gameService, movieService, bookService, showService);

        verify(request).getSession();
    }

    @Test
    @DisplayName("user Not Found: Should catch UserNotFoundException")
    void doGet_UserNotFound_ShouldHandleError() throws ServletException, IOException {
        String username = "ghost";
        String category = "games";

        when(request.getParameter("user")).thenReturn(username);
        when(request.getParameter("category")).thenReturn(category);

        when(userService.getUser(username)).thenThrow(new UserNotFoundException(username));

        profileDataServlet.doGet(request, response);

        verify(userService).getUser(username);
        verifyNoInteractions(gameService);

        verify(request).getSession();
    }

    @Test
    @DisplayName("missing Parameter: Should handle MissingParameterException")
    void doGet_MissingUserParam_ShouldHandleError() throws ServletException, IOException {
        when(request.getParameter("user")).thenReturn(null);

        profileDataServlet.doGet(request, response);

        verifyNoInteractions(userService, gameService);

        verify(request).getSession();
    }

    @Test
    @DisplayName("database Error: Should wrap unexpected exceptions")
    void doGet_DatabaseError_ShouldHandleException() throws ServletException, IOException {
        String username = "owner";
        String category = "games";

        when(request.getParameter("user")).thenReturn(username);
        when(request.getParameter("category")).thenReturn(category);
        when(userService.getUser(username)).thenReturn(profileOwner);

        when(gameService.getUserGamesPage(anyInt(), anyInt(), anyInt(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB Connection Failed"));

        profileDataServlet.doGet(request, response);

        verify(gameService).getUserGamesPage(anyInt(), anyInt(), anyInt(), any(), any(), any(), any());

        verify(request).getSession();
    }
}