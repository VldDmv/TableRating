package org.criticizer.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.criticizer.entity.Role;
import org.criticizer.entity.User;
import org.criticizer.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class UserIntegrationTest {

    @Autowired private UserRepository userRepository;

    @AfterEach
    void cleanup() {
        userRepository.deleteAll();
    }

    @Test
    void createAndFindUser_Success() {
        // Arrange
        User user = new User("testuser", "hashedpassword");
        user.setRole(Role.USER);
        user.setProfileIsPublic(true);

        // Act
        User saved = userRepository.save(user);
        Optional<User> found = userRepository.findByNameIgnoreCase("testuser");

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("testuser");
    }

    @Test
    void searchUsers_FindsMatchingUsers() {
        // Arrange
        User user1 = new User("alice", "hash");
        user1.setProfileIsPublic(true);
        User user2 = new User("bob", "hash");
        user2.setProfileIsPublic(true);
        User user3 = new User("charlie", "hash");
        user3.setProfileIsPublic(false);

        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);

        // Act
        long publicCount = userRepository.countUsers(null, true);
        long allCount = userRepository.countUsers(null, false);

        // Assert
        assertThat(publicCount).isEqualTo(2); // Only alice and bob
        assertThat(allCount).isEqualTo(3); // All users
    }
}
