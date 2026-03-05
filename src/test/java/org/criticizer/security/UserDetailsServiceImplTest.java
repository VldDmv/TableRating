package org.criticizer.security;

import org.criticizer.entity.Role;
import org.criticizer.entity.User;
import org.criticizer.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = new User(1, "testuser", "hashedpass", Role.USER, true);
        adminUser = new User(2, "admin", "adminpass", Role.ADMIN, true);
    }

    @Test
    void loadUserByUsername_ExistingUser_ReturnsUserDetails() {
        // Arrange
        when(userService.getUser("testuser")).thenReturn(testUser);

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("hashedpass");
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");

        verify(userService).getUser("testuser");
    }

    @Test
    void loadUserByUsername_AdminUser_HasAdminRole() {
        // Arrange
        when(userService.getUser("admin")).thenReturn(adminUser);

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

        // Assert
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_NonExistentUser_ThrowsException() {
        // Arrange
        when(userService.getUser("nonexistent"))
                .thenThrow(new org.criticizer.exceptions.data.UserNotFoundException("nonexistent"));

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);

        verify(userService).getUser("nonexistent");
    }

    @Test
    void loadUserByUsername_NullUser_ThrowsException() {
        // Arrange
        when(userService.getUser("testuser")).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("testuser"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: testuser");
    }

    @Test
    void loadUserByUsername_CaseInsensitive_Works() {
        // Arrange
        when(userService.getUser("TESTUSER")).thenReturn(testUser);

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("TESTUSER");

        // Assert
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        verify(userService).getUser("TESTUSER");
    }
}