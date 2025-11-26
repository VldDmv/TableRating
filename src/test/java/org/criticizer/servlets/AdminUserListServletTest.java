    package org.criticizer.servlets;

    import jakarta.servlet.RequestDispatcher;
    import jakarta.servlet.ServletConfig;
    import jakarta.servlet.ServletContext;
    import jakarta.servlet.ServletException;
    import jakarta.servlet.http.HttpServletRequest;
    import jakarta.servlet.http.HttpServletResponse;
    import org.criticizer.entity.Role;
    import org.criticizer.entity.User;
    import org.criticizer.service.user.UserService;
    import org.criticizer.servlets.admin.AdminUserListServlet;
    import org.junit.jupiter.api.BeforeEach;
    import org.mockito.Mock;
    import org.mockito.MockitoAnnotations;

    import java.util.Arrays;
    import java.util.List;

    import static org.mockito.Mockito.when;

    class AdminUserListServletTest {

        @Mock
        private HttpServletRequest request;

        @Mock
        private HttpServletResponse response;

        @Mock
        private ServletContext servletContext;

        @Mock
        private ServletConfig servletConfig;

        @Mock
        private UserService userService;

        @Mock
        private RequestDispatcher requestDispatcher;

        private AdminUserListServlet servlet;

        private List<User> testUsers;

        @BeforeEach
        void setUp() throws ServletException {
            MockitoAnnotations.openMocks(this);
            servlet = new AdminUserListServlet();

            when(servletConfig.getServletContext()).thenReturn(servletContext);
            when(servletContext.getAttribute("userService")).thenReturn(userService);
            servlet.init(servletConfig);

            testUsers = Arrays.asList(
                    new User(1, "user1", "pass1", Role.USER, false),
                    new User(2, "user2", "pass2", Role.ADMIN, false)
            );


            when(request.getRequestDispatcher("/WEB-INF/admin/userList.jsp")).thenReturn(requestDispatcher);
        }
    }

