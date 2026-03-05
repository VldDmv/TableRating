package org.criticizer.controller.auth;

import org.criticizer.entity.User;
import org.criticizer.repository.*;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.dashboard.DashboardService;
import org.criticizer.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HomeController Tests")
class HomeControllerTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ShowRepository showRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private SecurityUtil securityUtil;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DashboardService dashboardService;

    @InjectMocks
    private HomeController controller;

    private MockMvc mockMvc;
    private User testUser;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();

        testUser = TestDataBuilder.createRegularUser();
    }

    @Test
    @DisplayName("GET / - Should return index page")
    void shouldReturnIndexPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("GET /index - Should return index page")
    void shouldReturnIndexPageAlternate() throws Exception {
        mockMvc.perform(get("/index"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("GET /dashboard - Should load dashboard with statistics")
    void shouldLoadDashboardWithStatistics() throws Exception {
        when(securityUtil.getCurrentUser()).thenReturn(testUser);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("username", testUser.getName()))
                .andExpect(model().attributeExists("gamesStats"))
                .andExpect(model().attributeExists("moviesStats"))
                .andExpect(model().attributeExists("booksStats"))
                .andExpect(model().attributeExists("showsStats"));

        verify(securityUtil).getCurrentUser();
        verify(dashboardService).getUserDashboardStats(testUser.getId());
    }
}