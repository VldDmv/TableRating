package org.criticizer.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.criticizer.dto.helper.PageResponse;
import org.criticizer.dto.user.UserPublicResponse;
import org.criticizer.entity.Role;
import org.criticizer.entity.User;
import org.criticizer.exceptions.data.UserAlreadyExistsException;
import org.criticizer.exceptions.data.UserNotFoundException;
import org.criticizer.exceptions.security.InsufficientPermissionsException;
import org.criticizer.exceptions.security.OperationNotPermittedException;
import org.criticizer.exceptions.validation.InvalidInputException;
import org.criticizer.repository.*;
import org.criticizer.service.helper.ServiceValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Comprehensive unit tests for UserService. Uses Mockito for dependency mocking and AssertJ for
 * fluent assertions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;

    @Mock private GameRepository gameRepository;

    @Mock private MovieRepository movieRepository;

    @Mock private BookRepository bookRepository;

    @Mock private ShowRepository showRepository;

    @Mock private ServiceValidator validator;

    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "hashedPassword");
        testUser.setId(1);
        testUser.setRole(Role.USER);
        testUser.setProfileIsPublic(true);
        testUser.setCreatedAt(LocalDateTime.now());

        adminUser = new User("admin", "hashedAdminPassword");
        adminUser.setId(2);
        adminUser.setRole(Role.ADMIN);
        adminUser.setProfileIsPublic(true);
    }

    // ==================== GET USER TESTS ====================

    @Nested
    @DisplayName("getUser() Tests")
    class GetUserTests {

        @Test
        @DisplayName("Should return user when exists")
        void shouldReturnUserWhenExists() {
            // Given
            when(userRepository.findByNameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));

            // When
            User result = userService.getUser("testuser");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("testuser");
            assertThat(result.getId()).isEqualTo(1);

            verify(userRepository).findByNameIgnoreCase("testuser");
        }

        @Test
        @DisplayName("Should be case-insensitive")
        void shouldBeCaseInsensitive() {
            // Given
            when(userRepository.findByNameIgnoreCase("TESTUSER")).thenReturn(Optional.of(testUser));

            // When
            User result = userService.getUser("TESTUSER");

            // Then
            assertThat(result).isEqualTo(testUser);
            verify(userRepository).findByNameIgnoreCase("TESTUSER");
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user does not exist")
        void shouldThrowWhenUserNotFound() {
            // Given
            when(userRepository.findByNameIgnoreCase("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.getUser("nonexistent"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("nonexistent");

            verify(userRepository).findByNameIgnoreCase("nonexistent");
        }
    }

    // ==================== REGISTER USER TESTS ====================

    @Nested
    @DisplayName("registerUser() Tests")
    class RegisterUserTests {

        @Test
        @DisplayName("Should successfully register new user")
        void shouldRegisterNewUser() {
            // Given
            String username = "newuser";
            String password = "password123";
            String trimmed = "newuser";
            String hashedPassword = "$2a$10$hashedPassword";

            when(validator.validateUsername(username)).thenReturn(trimmed);
            doNothing().when(validator).validatePassword(password);
            when(userRepository.existsByNameIgnoreCase(trimmed)).thenReturn(false);
            when(passwordEncoder.encode(password)).thenReturn(hashedPassword);
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When
            userService.registerUser(username, password);

            // Then
            verify(validator).validateUsername(username);
            verify(validator).validatePassword(password);
            verify(userRepository).existsByNameIgnoreCase(trimmed);
            verify(passwordEncoder).encode(password);
            verify(userRepository)
                    .save(
                            argThat(
                                    user ->
                                            user.getName().equals(trimmed)
                                                    && user.getPassword().equals(hashedPassword)
                                                    && user.getRole() == Role.USER
                                                    && !user.isProfileIsPublic()));
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException when username is taken")
        void shouldThrowWhenUsernameExists() {
            // Given
            String username = "existinguser";
            String trimmed = "existinguser";

            when(validator.validateUsername(username)).thenReturn(trimmed);
            doNothing().when(validator).validatePassword(anyString());
            when(userRepository.existsByNameIgnoreCase(trimmed)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.registerUser(username, "password"))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining(trimmed);

            verify(userRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Should set default values for new user")
        void shouldSetDefaultValues() {
            // Given
            when(validator.validateUsername(anyString())).thenReturn("newuser");
            when(userRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");

            // When
            userService.registerUser("newuser", "password");

            // Then
            verify(userRepository)
                    .save(
                            argThat(
                                    user ->
                                            user.getRole() == Role.USER
                                                    && !user.isProfileIsPublic()));
        }
    }

    // ==================== EXISTS BY USERNAME TESTS ====================

    @Nested
    @DisplayName("existsByUsername() Tests")
    class ExistsByUsernameTests {

        @Test
        @DisplayName("Should return true when username exists")
        void shouldReturnTrueWhenExists() {
            // Given
            when(userRepository.existsByNameIgnoreCase("testuser")).thenReturn(true);

            // When
            boolean result = userService.existsByUsername("testuser");

            // Then
            assertThat(result).isTrue();
            verify(userRepository).existsByNameIgnoreCase("testuser");
        }

        @Test
        @DisplayName("Should return false when username does not exist")
        void shouldReturnFalseWhenNotExists() {
            // Given
            when(userRepository.existsByNameIgnoreCase("newuser")).thenReturn(false);

            // When
            boolean result = userService.existsByUsername("newuser");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for null username")
        void shouldReturnFalseForNull() {
            // When
            boolean result = userService.existsByUsername(null);

            // Then
            assertThat(result).isFalse();
            verify(userRepository, never()).existsByNameIgnoreCase(any());
        }

        @Test
        @DisplayName("Should return false for empty username")
        void shouldReturnFalseForEmpty() {
            // When
            boolean result = userService.existsByUsername("   ");

            // Then
            assertThat(result).isFalse();
            verify(userRepository, never()).existsByNameIgnoreCase(any());
        }

        @Test
        @DisplayName("Should trim username before checking")
        void shouldTrimUsername() {
            // Given
            when(userRepository.existsByNameIgnoreCase("testuser")).thenReturn(true);

            // When
            boolean result = userService.existsByUsername("  testuser  ");

            // Then
            assertThat(result).isTrue();
            verify(userRepository).existsByNameIgnoreCase("testuser");
        }
    }

    // ==================== GET USERS PAGE WITH STATS TESTS ====================

    @Nested
    @DisplayName("getUsersPageWithStats() Tests")
    class GetUsersPageWithStatsTests {

        @Test
        @DisplayName("Should return paginated users with statistics")
        void shouldReturnPaginatedUsersWithStats() {
            // Given
            ServiceValidator.PaginationParams params =
                    new ServiceValidator.PaginationParams(1, 10, 0);

            List<User> users = Arrays.asList(testUser);
            Page<User> userPage = new PageImpl<>(users);

            when(validator.validatePagination(1, 10)).thenReturn(params);
            when(validator.sanitizeSearchTerm(null)).thenReturn(null);
            when(userRepository.findUsers(eq(true), any(Pageable.class))).thenReturn(userPage);

            // Mock statistics
            when(gameRepository.countByUserIds(anyList())).thenReturn(Map.of(1, 5L));
            when(movieRepository.countByUserIds(anyList())).thenReturn(Map.of(1, 10L));
            when(bookRepository.countByUserIds(anyList())).thenReturn(Map.of(1, 3L));
            when(showRepository.countByUserIds(anyList())).thenReturn(Map.of(1, 2L));

            // When
            PageResponse<UserPublicResponse> result =
                    userService.getUsersPageWithStats(null, 1, 10, "name", "asc");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);

            UserPublicResponse response = result.getItems().get(0);
            assertThat(response.getName()).isEqualTo("testuser");
            assertThat(response.getGamesCount()).isEqualTo(5);
            assertThat(response.getMoviesCount()).isEqualTo(10);
            assertThat(response.getBooksCount()).isEqualTo(3);
            assertThat(response.getShowsCount()).isEqualTo(2);
            assertThat(response.getTotalItems()).isEqualTo(20);
        }

        @Test
        @DisplayName("Should filter by search term")
        void shouldFilterBySearchTerm() {
            // Given
            ServiceValidator.PaginationParams params =
                    new ServiceValidator.PaginationParams(1, 10, 0);

            when(validator.validatePagination(1, 10)).thenReturn(params);
            when(validator.sanitizeSearchTerm("test")).thenReturn("test");

            Page<User> searchResults = new PageImpl<>(Arrays.asList(testUser));
            when(userRepository.searchUsers(eq("test"), eq(true), any(Pageable.class)))
                    .thenReturn(searchResults);

            // Mock statistics
            when(gameRepository.countByUserIds(anyList())).thenReturn(Map.of(1, 0L));
            when(movieRepository.countByUserIds(anyList())).thenReturn(Map.of(1, 0L));
            when(bookRepository.countByUserIds(anyList())).thenReturn(Map.of(1, 0L));
            when(showRepository.countByUserIds(anyList())).thenReturn(Map.of(1, 0L));

            // When
            PageResponse<UserPublicResponse> result =
                    userService.getUsersPageWithStats("test", 1, 10, "name", "asc");

            // Then
            assertThat(result.getItems()).isNotEmpty();
            verify(userRepository).searchUsers(eq("test"), eq(true), any(Pageable.class));
        }

        @Test
        @DisplayName("Should sort by total items descending")
        void shouldSortByTotalItems() {
            // Given
            User user1 = new User("user1", "pass");
            user1.setId(1);
            user1.setProfileIsPublic(true);

            User user2 = new User("user2", "pass");
            user2.setId(2);
            user2.setProfileIsPublic(true);

            ServiceValidator.PaginationParams params =
                    new ServiceValidator.PaginationParams(1, 10, 0);

            when(validator.validatePagination(1, 10)).thenReturn(params);
            when(validator.sanitizeSearchTerm(null)).thenReturn(null);

            Page<User> userPage = new PageImpl<>(Arrays.asList(user1, user2));
            when(userRepository.findUsers(eq(true), any(Pageable.class))).thenReturn(userPage);

            // user1 has 10 total items
            when(gameRepository.countByUserIds(anyList())).thenReturn(Map.of(1, 5L));
            when(movieRepository.countByUserIds(anyList())).thenReturn(Map.of(1, 5L));
            when(bookRepository.countByUserIds(anyList())).thenReturn(Map.of(1, 0L));
            when(showRepository.countByUserIds(anyList())).thenReturn(Map.of(1, 0L));

            // user2 has 20 total items
            when(gameRepository.countByUserIds(anyList())).thenReturn(Map.of(2, 10L));
            when(movieRepository.countByUserIds(anyList())).thenReturn(Map.of(2, 10L));
            when(bookRepository.countByUserIds(anyList())).thenReturn(Map.of(2, 0L));
            when(showRepository.countByUserIds(anyList())).thenReturn(Map.of(2, 0L));
            // When
            PageResponse<UserPublicResponse> result =
                    userService.getUsersPageWithStats(null, 1, 10, "totalItems", "desc");

            // Then
            assertThat(result.getItems()).hasSize(2);
            assertThat(result.getItems().get(0).getName()).isEqualTo("user2"); // 20 items first
            assertThat(result.getItems().get(1).getName()).isEqualTo("user1"); // 10 items second
        }
    }

    // ==================== CHANGE USER ROLE TESTS ====================

    @Nested
    @DisplayName("changeUserRole() Tests")
    class ChangeUserRoleTests {

        @Test
        @DisplayName("Should successfully change user role as admin")
        void shouldChangeUserRole() {
            // Given
            when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When
            userService.changeUserRole(1, Role.ADMIN, adminUser);

            // Then
            verify(userRepository)
                    .save(argThat(user -> user.getId() == 1 && user.getRole() == Role.ADMIN));
        }

        @Test
        @DisplayName("Should throw when non-admin tries to change role")
        void shouldThrowWhenNonAdminTriesChange() {
            // When & Then
            assertThatThrownBy(() -> userService.changeUserRole(1, Role.ADMIN, testUser))
                    .isInstanceOf(InsufficientPermissionsException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when admin tries to remove own admin role")
        void shouldThrowWhenAdminRemovesOwnRole() {
            // When & Then
            assertThatThrownBy(() -> userService.changeUserRole(2, Role.USER, adminUser))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("cannot remove their own admin role");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when target user not found")
        void shouldThrowWhenTargetUserNotFound() {
            // Given
            when(userRepository.findById(999)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.changeUserRole(999, Role.ADMIN, adminUser))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw when new role is null")
        void shouldThrowWhenRoleIsNull() {
            // When & Then
            assertThatThrownBy(() -> userService.changeUserRole(1, null, adminUser))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("newRole");
        }
    }

    // ==================== DELETE USER TESTS ====================

    @Nested
    @DisplayName("deleteUser() Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should successfully delete user and all media as admin")
        void shouldDeleteUser() {
            // Given
            when(userRepository.existsById(1)).thenReturn(true);
            doNothing().when(gameRepository).deleteByUserId(1);
            doNothing().when(movieRepository).deleteByUserId(1);
            doNothing().when(bookRepository).deleteByUserId(1);
            doNothing().when(showRepository).deleteByUserId(1);
            doNothing().when(userRepository).deleteById(1);

            // When
            userService.deleteUser(1, adminUser);

            // Then
            verify(gameRepository).deleteByUserId(1);
            verify(movieRepository).deleteByUserId(1);
            verify(bookRepository).deleteByUserId(1);
            verify(showRepository).deleteByUserId(1);
            verify(userRepository).deleteById(1);
        }

        @Test
        @DisplayName("Should throw when non-admin tries to delete user")
        void shouldThrowWhenNonAdminTriesDelete() {
            // When & Then
            assertThatThrownBy(() -> userService.deleteUser(2, testUser))
                    .isInstanceOf(InsufficientPermissionsException.class);

            verify(userRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw when admin tries to delete themselves")
        void shouldThrowWhenAdminDeletesSelf() {
            // When & Then
            assertThatThrownBy(() -> userService.deleteUser(2, adminUser))
                    .isInstanceOf(OperationNotPermittedException.class)
                    .hasMessageContaining("cannot delete your own account");

            verify(userRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw when target user does not exist")
        void shouldThrowWhenUserNotFound() {
            // Given
            when(userRepository.existsById(999)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> userService.deleteUser(999, adminUser))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ==================== UPDATE USER PRIVACY TESTS ====================

    @Nested
    @DisplayName("updateUserPrivacy() Tests")
    class UpdateUserPrivacyTests {

        @Test
        @DisplayName("Should successfully update privacy to public")
        void shouldUpdatePrivacyToPublic() {
            // Given
            testUser.setProfileIsPublic(false);
            when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When
            userService.updateUserPrivacy(1, true);

            // Then
            verify(userRepository)
                    .save(argThat(user -> user.getId() == 1 && user.isProfileIsPublic()));
        }

        @Test
        @DisplayName("Should successfully update privacy to private")
        void shouldUpdatePrivacyToPrivate() {
            // Given
            testUser.setProfileIsPublic(true);
            when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When
            userService.updateUserPrivacy(1, false);

            // Then
            verify(userRepository)
                    .save(argThat(user -> user.getId() == 1 && !user.isProfileIsPublic()));
        }

        @Test
        @DisplayName("Should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            // Given
            when(userRepository.findById(999)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.updateUserPrivacy(999, true))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ==================== LIST ALL USERS TESTS ====================

    @Nested
    @DisplayName("listAllUsers() Tests")
    class ListAllUsersTests {

        @Test
        @DisplayName("Should return all users sorted by name")
        void shouldReturnAllUsers() {
            // Given
            List<User> users = Arrays.asList(testUser, adminUser);
            Sort sort = Sort.by(Sort.Direction.ASC, "name");
            when(userRepository.findAll(sort)).thenReturn(users);

            // When
            List<User> result = userService.listAllUsers();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(testUser, adminUser);
            verify(userRepository).findAll(sort);
        }

        @Test
        @DisplayName("Should return empty list when no users exist")
        void shouldReturnEmptyListWhenNoUsers() {
            // Given
            Sort sort = Sort.by(Sort.Direction.ASC, "name");
            when(userRepository.findAll(sort)).thenReturn(List.of());

            // When
            List<User> result = userService.listAllUsers();

            // Then
            assertThat(result).isEmpty();
        }
    }
}
