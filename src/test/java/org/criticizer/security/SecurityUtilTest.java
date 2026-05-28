package org.criticizer.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.List;
import org.criticizer.entity.Role;
import org.criticizer.entity.User;
import org.criticizer.exceptions.security.UnauthorizedException;
import org.criticizer.service.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SecurityUtilTest {

    @Mock private UserService userService;

    @Mock private SecurityContext securityContext;

    @InjectMocks private SecurityUtil securityUtil;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = new User(1, "testuser", "hash", Role.USER, true);
        adminUser = new User(2, "admin", "hash", Role.ADMIN, true);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_Authenticated_ReturnsUser() {
        Authentication auth = mock(Authentication.class);
        AuthenticatedUser principal = mock(AuthenticatedUser.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(principal);
        when(principal.getUser()).thenReturn(testUser);
        when(securityContext.getAuthentication()).thenReturn(auth);

        User result = securityUtil.getCurrentUser();

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("testuser");
    }

    @Test
    void getCurrentUser_NotAuthenticated_ThrowsException() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> securityUtil.getCurrentUser())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User not authenticated");
    }

    @Test
    void getCurrentUser_AnonymousUser_ThrowsException() {
        // Arrange
        Authentication auth =
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());
        when(securityContext.getAuthentication()).thenReturn(auth);

        // Act & Assert
        assertThatThrownBy(() -> securityUtil.getCurrentUser())
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getCurrentUser_UnauthenticatedAuth_ThrowsException() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // Act & Assert
        assertThatThrownBy(() -> securityUtil.getCurrentUser())
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getCurrentUsername_Authenticated_ReturnsUsername() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        AuthenticatedUser principal = mock(AuthenticatedUser.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(principal);
        when(principal.getUsername()).thenReturn("testuser");
        when(securityContext.getAuthentication()).thenReturn(auth);

        // Act
        String username = securityUtil.getCurrentUsername();

        // Assert
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void getCurrentUsername_NotAuthenticated_ReturnsNull() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act
        String username = securityUtil.getCurrentUsername();

        // Assert
        assertThat(username).isNull();
    }

    @Test
    void getCurrentUsername_AnonymousUser_ReturnsNull() {
        // Arrange
        Authentication auth =
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());
        when(securityContext.getAuthentication()).thenReturn(auth);

        // Act
        String username = securityUtil.getCurrentUsername();

        // Assert
        assertThat(username).isNull();
    }

    @Test
    void isAuthenticated_WithValidAuth_ReturnsTrue() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        AuthenticatedUser principal = mock(AuthenticatedUser.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(principal);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // Act
        boolean result = securityUtil.isAuthenticated();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isAuthenticated_NoAuth_ReturnsFalse() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act
        boolean result = securityUtil.isAuthenticated();

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isAuthenticated_AnonymousUser_ReturnsFalse() {
        // Arrange
        Authentication auth =
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());
        when(securityContext.getAuthentication()).thenReturn(auth);

        // Act
        boolean result = securityUtil.isAuthenticated();

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isAuthenticated_UnauthenticatedAuth_ReturnsFalse() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // Act
        boolean result = securityUtil.isAuthenticated();

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isAdmin_AdminUser_ReturnsTrue() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        AuthenticatedUser principal = mock(AuthenticatedUser.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(principal);
        when(principal.getUser()).thenReturn(adminUser);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // Act
        boolean result = securityUtil.isAdmin();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isAdmin_RegularUser_ReturnsFalse() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        AuthenticatedUser principal = mock(AuthenticatedUser.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(principal);
        when(principal.getUser()).thenReturn(testUser);
        when(securityContext.getAuthentication()).thenReturn(auth);

        // Act
        boolean result = securityUtil.isAdmin();

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isAdmin_NotAuthenticated_ReturnsFalse() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        // Act
        boolean result = securityUtil.isAdmin();

        // Assert
        assertThat(result).isFalse();
        verify(userService, never()).getUser(anyString());
    }
}
