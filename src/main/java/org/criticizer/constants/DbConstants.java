package org.criticizer.constants;


public final class DbConstants {

    private DbConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    // ============= Table Names =============
    public static final class Tables {
        // Main entity tables
        public static final String USERS = "users";
        public static final String GAMES = "games";
        public static final String MOVIES = "movies";
        public static final String BOOKS = "books";
        public static final String SHOWS = "shows";

        // Taxonomy tables
        public static final String TAGS = "tags";
        public static final String GENRES = "genres";
        public static final String GENRE_APPLICABILITY = "genre_applicability";

        // Junction tables
        public static final String GAME_TAGS = "game_tags";
        public static final String MOVIE_GENRES = "movie_genres";
        public static final String BOOK_GENRES = "book_genres";
        public static final String SHOW_GENRES = "show_genres";

        private Tables() {}
    }

    // ============= Column Names =============
    public static final class Columns {
        // Common columns
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String USER_ID = "user_id";
        public static final String SCORE = "score";
        public static final String COMPLETED = "completed";

        // User specific
        public static final String PASSWORD = "password";
        public static final String ROLE = "role";
        public static final String PROFILE_IS_PUBLIC = "profile_is_public";

        // Junction table columns
        public static final String GAME_ID = "game_id";
        public static final String MOVIE_ID = "movie_id";
        public static final String BOOK_ID = "book_id";
        public static final String SHOW_ID = "show_id";
        public static final String TAG_ID = "tag_id";
        public static final String GENRE_ID = "genre_id";

        // Genre applicability
        public static final String MEDIA_TYPE = "media_type";

        private Columns() {}
    }

    // ============= Media Types =============
    public static final class MediaTypes {
        public static final String MOVIE = "movie";
        public static final String BOOK = "book";
        public static final String SHOW = "show";
        public static final String SHARED = "shared";

        private MediaTypes() {}
    }

    // ============= Query Defaults =============
    public static final class QueryDefaults {
        public static final String DEFAULT_SORT_COLUMN = "name";
        public static final String DEFAULT_SORT_ORDER = "ASC";

        private QueryDefaults() {}
    }
}