package org.criticizer.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.criticizer.entity.Role;
import org.criticizer.entity.User;
import org.criticizer.repository.GameRepository;
import org.criticizer.repository.TagRepository;
import org.criticizer.repository.UserRepository;
import org.criticizer.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class E2EIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private User adminUser;

    private static AuthenticatedUser auth(User u) {
        return new AuthenticatedUser(u);
    }

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", passwordEncoder.encode("password"));
        testUser.setRole(Role.USER);
        testUser.setProfileIsPublic(true);
        testUser = userRepository.save(testUser);

        adminUser = new User("admin", passwordEncoder.encode("adminpass"));
        adminUser.setRole(Role.ADMIN);
        adminUser.setProfileIsPublic(true);
        adminUser = userRepository.save(adminUser);
    }

    @AfterEach
    void cleanup() {
        gameRepository.deleteAll();
        tagRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void completeUserJourney_RegisterLoginCreateGameLogout() throws Exception {
        // 1. Register new user
        mockMvc.perform(post("/auth/register")
                        .param("username", "newuser")
                        .param("password", "newpass123")
                        .param("confirmPassword", "newpass123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        User newUser = userRepository.findByNameIgnoreCase("newuser").orElseThrow();

        // 2. Create a game
        String gameJson = """
                {"name":"Dark Souls","score":95,"tagIds":[]}
                """;

        mockMvc.perform(post("/api/games")
                        .with(user(auth(newUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gameJson)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Game created successfully"));

        // 3. Get games list
        mockMvc.perform(get("/api/games").with(user(auth(newUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].name").value("Dark Souls"))
                .andExpect(jsonPath("$.items[0].score").value(95));

        // 4. Update game
        String updateJson = """
                {"name":"Dark Souls Remastered","score":96,"tagIds":[]}
                """;
        mockMvc.perform(put("/api/games/Dark Souls")
                        .with(user(auth(newUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Game updated successfully"));

        // 5. Toggle completion
        mockMvc.perform(patch("/api/games/Dark Souls Remastered/toggle")
                        .with(user(auth(newUser))).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        // 6. Delete game
        mockMvc.perform(delete("/api/games/Dark Souls Remastered")
                        .with(user(auth(newUser))).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Game deleted successfully"));

        // 7. Verify game is deleted
        mockMvc.perform(get("/api/games").with(user(auth(newUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));

        // 8. Logout
        mockMvc.perform(post("/logout").with(user(auth(newUser))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index?logout=true"));
    }

    @Test
    void adminWorkflow_ManageUsersAndViewStats() throws Exception {
        // 1. Get dashboard stats
        mockMvc.perform(get("/api/admin/stats").with(user(auth(adminUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").exists())
                .andExpect(jsonPath("$.totalGames").exists());

        // 2. Get all users
        mockMvc.perform(get("/api/admin/users").with(user(auth(adminUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(2))));

        // 3. Change user role
        String roleJson = """
                {"role":"ADMIN"}
                """;
        mockMvc.perform(put("/api/admin/users/" + testUser.getId() + "/role")
                        .with(user(auth(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleJson)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User role updated successfully"));

        // 4. Verify role change
        mockMvc.perform(get("/api/admin/users/" + testUser.getId())
                        .with(user(auth(adminUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void profilePrivacyWorkflow() throws Exception {
        // 1. Get own profile
        mockMvc.perform(get("/api/profiles/me").with(user(auth(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.isOwner").value(true))
                .andExpect(jsonPath("$.profileIsPublic").value(true));

        // 2. Change privacy to private
        String privacyJson = """
                {"isPublic":false}
                """;
        mockMvc.perform(put("/api/profiles/me/privacy")
                        .with(user(auth(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(privacyJson)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublic").value(false));

        // 3. Verify profile is now private — reload from DB, testUser entity is stale
        testUser = userRepository.findById(testUser.getId()).orElseThrow();
        mockMvc.perform(get("/api/profiles/me").with(user(auth(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileIsPublic").value(false));
    }

    @Test
    void tagManagementWorkflow() throws Exception {
        //1. Creating Tags (ADMIN)
        String tagJson = """
                {"name":"RPG"}
                """;
        mockMvc.perform(post("/api/tags")
                        .with(user(auth(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tagJson)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("RPG"));

        // 2. Get all tags
        mockMvc.perform(get("/api/tags").with(user(auth(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("RPG"));

        String tagId = tagRepository.findByNameIgnoreCase("RPG").get().getId().toString();

        // 3. Check if tag is in use
        mockMvc.perform(get("/api/tags/" + tagId + "/in-use").with(user(auth(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));

        // 4. Create game with this tag
        String gameJson = String.format("""
                {"name":"Skyrim","score":95,"tagIds":[%s]}
                """, tagId);
        mockMvc.perform(post("/api/games")
                        .with(user(auth(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gameJson)
                        .with(csrf()))
                .andExpect(status().isCreated());

        // 5. Check if tag is in use now
        mockMvc.perform(get("/api/tags/" + tagId + "/in-use").with(user(auth(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

        // 6. Delete tag (ADMIN)
        mockMvc.perform(delete("/api/tags/" + tagId)
                        .with(user(auth(adminUser))).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Tag deleted successfully"));
    }

    @Test
    void genreManagementWorkflow() throws Exception {
        // 1. Creating Genres (ADMIN)
        String genreJson = """
                {"name":"Action","mediaTypes":["movie","show"]}
                """;
        mockMvc.perform(post("/api/genres")
                        .with(user(auth(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(genreJson)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Action"))
                .andExpect(jsonPath("$.mediaTypes", hasSize(2)));

        // 2. Get available genres for movies
        mockMvc.perform(get("/api/genres/available/movie").with(user(auth(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Action")));

        // 3. Get available genres for books (should not include Action)
        mockMvc.perform(get("/api/genres/available/book").with(user(auth(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", not(hasItem("Action"))));
    }

    @Test
    void paginationWorkflow() throws Exception {
        for (int i = 1; i <= 15; i++) {
            String gameJson = String.format(
                    "{\"name\":\"Game %d\",\"score\":%d,\"tagIds\":[]}", i, 50 + i);
            mockMvc.perform(post("/api/games")
                            .with(user(auth(testUser)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(gameJson)
                            .with(csrf()))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/games").with(user(auth(testUser)))
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(10)))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalItems").value(15));

        mockMvc.perform(get("/api/games").with(user(auth(testUser)))
                        .param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.currentPage").value(2));

        mockMvc.perform(get("/api/games").with(user(auth(testUser)))
                        .param("search", "Game 1").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(6))));
    }

    @Test
    void sortingWorkflow() throws Exception {
        String[] games = {
                "{\"name\":\"Low Score\",\"score\":60,\"tagIds\":[]}",
                "{\"name\":\"High Score\",\"score\":95,\"tagIds\":[]}",
                "{\"name\":\"Medium Score\",\"score\":75,\"tagIds\":[]}"
        };
        for (String game : games) {
            mockMvc.perform(post("/api/games")
                            .with(user(auth(testUser)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(game)
                            .with(csrf()))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/games").with(user(auth(testUser)))
                        .param("sortBy", "score").param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].score").value(60))
                .andExpect(jsonPath("$.items[2].score").value(95));

        mockMvc.perform(get("/api/games").with(user(auth(testUser)))
                        .param("sortBy", "score").param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].score").value(95))
                .andExpect(jsonPath("$.items[2].score").value(60));

        mockMvc.perform(get("/api/games").with(user(auth(testUser)))
                        .param("sortBy", "name").param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("High Score"));
    }
}