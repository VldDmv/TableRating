package org.criticizer.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.criticizer.entity.*;
import org.criticizer.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private GameRepository gameRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private ShowRepository showRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private GenreRepository genreRepository;
    @Autowired private GenreApplicabilityRepository applicabilityRepository;

    @AfterEach
    void cleanup() {
        gameRepository.deleteAll();
        movieRepository.deleteAll();
        bookRepository.deleteAll();
        showRepository.deleteAll();
        tagRepository.deleteAll();
        genreRepository.deleteAll();
        applicabilityRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ========== User Repository Tests ==========

    @Test
    void userRepository_SaveAndFind_Success() {
        User user = new User("testuser", "hashedpass");
        user.setRole(Role.USER);
        user.setProfileIsPublic(true);

        User saved = userRepository.save(user);
        Optional<User> found = userRepository.findByNameIgnoreCase("TESTUSER");

        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualToIgnoringCase("testuser");
    }

    @Test
    void userRepository_SearchUsers_FiltersCorrectly() {
        User alice = new User("alice", "hash");
        alice.setProfileIsPublic(true);
        User bob = new User("bob", "hash");
        bob.setProfileIsPublic(true);
        User charlie = new User("charlie", "hash");
        charlie.setProfileIsPublic(false);

        userRepository.saveAll(List.of(alice, bob, charlie));

        Page<User> publicUsers = userRepository.findUsers(true, PageRequest.of(0, 10));
        Page<User> allUsers = userRepository.findUsers(false, PageRequest.of(0, 10));
        Page<User> searchResults = userRepository.searchUsers("ali", true, PageRequest.of(0, 10));

        assertThat(publicUsers.getContent()).hasSize(2);
        assertThat(allUsers.getContent()).hasSize(3);
        assertThat(searchResults.getContent()).hasSize(1);
        assertThat(searchResults.getContent().get(0).getName()).isEqualTo("alice");
    }

    // ========== Game Repository Tests ==========

    @Test
    void gameRepository_SaveWithTags_Success() {
        User user = userRepository.save(new User("gamer", "hash"));
        Tag rpg = tagRepository.save(new Tag(null, "RPG"));
        Tag action = tagRepository.save(new Tag(null, "Action"));

        Game game = new Game(null, "Skyrim", user.getId(), 95, false);
        game.setTags(new HashSet<>(List.of(rpg, action)));

        Game saved = gameRepository.save(game);
        Game found = gameRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTags()).hasSize(2);
        assertThat(found.getTags()).extracting("name").containsExactlyInAnyOrder("RPG", "Action");
    }

    @Test
    void gameRepository_FindItemsWithTags_EagerLoading() {
        User user = userRepository.save(new User("gamer", "hash"));
        Tag rpg = tagRepository.save(new Tag(null, "RPG"));

        Game game1 = new Game(null, "Dark Souls", user.getId(), 90, true);
        game1.setTags(new HashSet<>(List.of(rpg)));
        Game game2 = new Game(null, "Elden Ring", user.getId(), 95, false);
        game2.setTags(new HashSet<>(List.of(rpg)));

        gameRepository.saveAll(List.of(game1, game2));

        // Two-step query update
        Page<Integer> ids =
                gameRepository.findItemIds(
                        user.getId(), null, null, null, null, PageRequest.of(0, 10));
        List<Game> results = gameRepository.findByIdsWithCategories(ids.getContent());

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTags()).isNotEmpty();
    }

    @Test
    void gameRepository_FilterByTag_Success() {
        User user = userRepository.save(new User("gamer", "hash"));
        Tag rpg = tagRepository.save(new Tag(null, "RPG"));
        Tag fps = tagRepository.save(new Tag(null, "FPS"));

        Game game1 = new Game(null, "Skyrim", user.getId(), 95, false);
        game1.setTags(new HashSet<>(List.of(rpg)));
        Game game2 = new Game(null, "Doom", user.getId(), 85, false);
        game2.setTags(new HashSet<>(List.of(fps)));

        gameRepository.saveAll(List.of(game1, game2));

        // Two-step query update
        Page<Integer> ids =
                gameRepository.findItemIds(
                        user.getId(), rpg.getId(), null, null, null, PageRequest.of(0, 10));
        List<Game> rpgGames = gameRepository.findByIdsWithCategories(ids.getContent());

        assertThat(rpgGames).hasSize(1);
        assertThat(rpgGames.get(0).getName()).isEqualTo("Skyrim");
    }

    @Test
    void gameRepository_SearchByName_Success() {
        User user = userRepository.save(new User("gamer", "hash"));
        gameRepository.saveAll(
                List.of(
                        new Game(null, "Dark Souls", user.getId(), 90, false),
                        new Game(null, "Dark Souls 2", user.getId(), 85, false),
                        new Game(null, "Elden Ring", user.getId(), 95, false)));

        // Two-step query update
        Page<Integer> ids =
                gameRepository.findItemIds(
                        user.getId(), null, "dark", null, null, PageRequest.of(0, 10));
        List<Game> results = gameRepository.findByIdsWithCategories(ids.getContent());

        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting("name")
                .allMatch(name -> ((String) name).toLowerCase().contains("dark"));
    }

    // ========== Movie Repository Tests ==========

    @Test
    void movieRepository_SaveWithGenres_Success() {
        User user = userRepository.save(new User("viewer", "hash"));
        Genre action = genreRepository.save(new Genre(null, "Action"));
        Genre scifi = genreRepository.save(new Genre(null, "Sci-Fi"));

        Movie movie = new Movie(null, "The Matrix", user.getId(), 95, false);
        movie.setGenres(new HashSet<>(List.of(action, scifi)));

        Movie saved = movieRepository.save(movie);
        Movie found = movieRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getGenres()).hasSize(2);
        assertThat(found.getGenres())
                .extracting("name")
                .containsExactlyInAnyOrder("Action", "Sci-Fi");
    }

    @Test
    void movieRepository_FilterByGenre_Success() {
        User user = userRepository.save(new User("viewer", "hash"));
        Genre action = genreRepository.save(new Genre(null, "Action"));
        Genre drama = genreRepository.save(new Genre(null, "Drama"));

        Movie movie1 = new Movie(null, "Die Hard", user.getId(), 90, false);
        movie1.setGenres(new HashSet<>(List.of(action)));
        Movie movie2 = new Movie(null, "The Godfather", user.getId(), 100, false);
        movie2.setGenres(new HashSet<>(List.of(drama)));

        movieRepository.saveAll(List.of(movie1, movie2));

        // Two-step query update
        Page<Integer> ids =
                movieRepository.findItemIds(
                        user.getId(), action.getId(), null, null, null, PageRequest.of(0, 10));
        List<Movie> actionMovies = movieRepository.findByIdsWithCategories(ids.getContent());

        assertThat(actionMovies).hasSize(1);
        assertThat(actionMovies.get(0).getName()).isEqualTo("Die Hard");
    }

    // ========== Genre Repository Tests ==========

    @Test
    void genreRepository_FindAvailableGenresFor_Success() {
        Genre action = genreRepository.save(new Genre(null, "Action"));
        Genre romance = genreRepository.save(new Genre(null, "Romance"));
        Genre drama = genreRepository.save(new Genre(null, "Drama"));

        applicabilityRepository.saveAll(
                List.of(
                        new GenreApplicability(action.getId(), "movie"),
                        new GenreApplicability(romance.getId(), "book"),
                        new GenreApplicability(drama.getId(), "shared")));

        List<Genre> movieGenres = genreRepository.findAvailableGenresFor("movie");
        List<Genre> bookGenres = genreRepository.findAvailableGenresFor("book");

        assertThat(movieGenres).hasSize(2); // Action + Drama (shared)
        assertThat(bookGenres).hasSize(2); // Romance + Drama (shared)
        assertThat(movieGenres).extracting("name").containsExactlyInAnyOrder("Action", "Drama");
    }

    @Test
    void genreRepository_CheckGenreInUse_Success() {
        User user = userRepository.save(new User("user", "hash"));
        Genre action = genreRepository.save(new Genre(null, "Action"));
        Genre unused = genreRepository.save(new Genre(null, "Unused"));

        Movie movie = new Movie(null, "Test Movie", user.getId(), 80, false);
        movie.setGenres(new HashSet<>(List.of(action)));
        movieRepository.save(movie);

        boolean actionInUse = genreRepository.countMoviesWithGenre(action.getId()) > 0;
        boolean unusedInUse = genreRepository.countMoviesWithGenre(unused.getId()) > 0;

        assertThat(actionInUse).isTrue();
        assertThat(unusedInUse).isFalse();
    }

    // ========== Tag Repository Tests ==========

    @Test
    void tagRepository_FindByGameId_Success() {
        User user = userRepository.save(new User("gamer", "hash"));
        Tag rpg = tagRepository.save(new Tag(null, "RPG"));
        Tag openWorld = tagRepository.save(new Tag(null, "Open World"));

        Game game = new Game(null, "Skyrim", user.getId(), 95, false);
        game.setTags(new HashSet<>(List.of(rpg, openWorld)));
        Game saved = gameRepository.save(game);

        List<Tag> tags = tagRepository.findByGameId(saved.getId());

        assertThat(tags).hasSize(2);
        assertThat(tags).extracting("name").containsExactlyInAnyOrder("RPG", "Open World");
    }

    @Test
    void tagRepository_IsTagInUse_Success() {
        User user = userRepository.save(new User("gamer", "hash"));
        Tag usedTag = tagRepository.save(new Tag(null, "Used"));
        Tag unusedTag = tagRepository.save(new Tag(null, "Unused"));

        Game game = new Game(null, "Test Game", user.getId(), 80, false);
        game.setTags(new HashSet<>(List.of(usedTag)));
        gameRepository.save(game);

        boolean used = tagRepository.isTagInUse(usedTag.getId());
        boolean unused = tagRepository.isTagInUse(unusedTag.getId());

        assertThat(used).isTrue();
        assertThat(unused).isFalse();
    }

    // ========== Cascade Delete Tests ==========

    @Test
    void userRepository_DeleteUser_CascadesMediaDeletion() {
        User user = userRepository.save(new User("testuser", "hash"));
        gameRepository.save(new Game(null, "Game1", user.getId(), 80, false));
        movieRepository.save(new Movie(null, "Movie1", user.getId(), 80, false));
        bookRepository.save(new Book(null, "Book1", user.getId(), 80, false));
        showRepository.save(new Show(null, "Show1", user.getId(), 80, false));

        gameRepository.deleteByUserId(user.getId());
        movieRepository.deleteByUserId(user.getId());
        bookRepository.deleteByUserId(user.getId());
        showRepository.deleteByUserId(user.getId());
        userRepository.delete(user);

        assertThat(userRepository.count()).isZero();
        assertThat(gameRepository.count()).isZero();
        assertThat(movieRepository.count()).isZero();
        assertThat(bookRepository.count()).isZero();
        assertThat(showRepository.count()).isZero();
    }

    // ========== Sorting Tests ==========

    @Test
    void gameRepository_SortByScore_Success() {
        User user = userRepository.save(new User("gamer", "hash"));
        gameRepository.saveAll(
                List.of(
                        new Game(null, "Low", user.getId(), 60, false),
                        new Game(null, "High", user.getId(), 95, false),
                        new Game(null, "Medium", user.getId(), 75, false)));

        // Two-step query update
        Page<Integer> ascIds =
                gameRepository.findItemIds(
                        user.getId(),
                        null,
                        null,
                        null,
                        null,
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "score")));
        Page<Integer> descIds =
                gameRepository.findItemIds(
                        user.getId(),
                        null,
                        null,
                        null,
                        null,
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "score")));

        List<Game> ascending = gameRepository.findByIdsWithCategories(ascIds.getContent());
        List<Game> descending = gameRepository.findByIdsWithCategories(descIds.getContent());

        ascending.sort(Comparator.comparing(Game::getScore));
        descending.sort(Comparator.comparing(Game::getScore).reversed());

        assertThat(ascending.get(0).getScore()).isEqualTo(60);
        assertThat(ascending.get(2).getScore()).isEqualTo(95);
        assertThat(descending.get(0).getScore()).isEqualTo(95);
        assertThat(descending.get(2).getScore()).isEqualTo(60);
    }
}
