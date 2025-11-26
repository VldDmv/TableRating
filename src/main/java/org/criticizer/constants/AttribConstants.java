package org.criticizer.constants;

/**
 * Storage of constants for servlets
 */
public final class AttribConstants {

    private AttribConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    // ============= Session Attributes =============
    public static final class SessionAttributes {
        public static final String USER = "user";
        public static final String CSRF_TOKEN = "_csrfToken";
        public static final String FLASH_SUCCESS = "flashSuccessMessage";
        public static final String FLASH_ERROR = "flashErrorMessage";

        private SessionAttributes() {}
    }

    // ============= Request Attributes =============
    public static final class RequestAttributes {
        // Common
        public static final String ERROR_MESSAGE = "errorMessage";
        public static final String ERROR_CODE = "errorCode";
        public static final String GSON = "gson";

        // User related
        public static final String PROFILE_OWNER = "profileOwner";
        public static final String IS_OWNER_VIEWING = "isOwnerViewing";
        public static final String CAN_VIEW = "canView";
        public static final String USER_LIST = "userList";
        public static final String CURRENT_PAGE = "currentPage";
        public static final String TOTAL_PAGES = "totalPages";
        public static final String SEARCH_TERM = "searchTerm";
        public static final String PAGE_SIZE = "pageSize";

        // Profile specific
        public static final String PAGE_RESULT = "pageResult";
        public static final String INITIAL_TAB = "initialTab";

        // Items related
        public static final String ENTITY_TYPE = "entityType";
        public static final String ENTITY_NAME_SINGULAR = "entityNameSingular";
        public static final String ENTITY_NAME_PLURAL = "entityNamePlural";
        public static final String PARAM_NAMES = "paramNames";
        public static final String ADD_FORM_ID = "addFormId";
        public static final String ALL_TAGS = "allTags";
        public static final String ALL_GENRES = "allGenres";

        // Admin specific
        public static final String STATS = "stats";
        public static final String ITEMS = "items";
        public static final String ITEM_TYPE = "itemType";
        public static final String TYPE = "type";

        private RequestAttributes() {}
    }

    // ============= Request Parameters =============
    public static final class RequestParams {
        // Common
        public static final String ACTION = "action";
        public static final String PAGE = "page";
        public static final String PAGE_SIZE = "pageSize";
        public static final String SEARCH = "search";
        public static final String USERNAME = "username";
        public static final String USER_ID = "userId";

        // Authentication
        public static final String NAME = "name";
        public static final String PASSWORD = "password";
        public static final String CONFIRM_PASSWORD = "confirmPassword";

        // Profile
        public static final String TAB = "tab";
        public static final String PRIVACY = "privacy";
        public static final String USER_PARAM = "user";
        public static final String CATEGORY = "category";

        // Admin
        public static final String NEW_ROLE = "newRole";
        public static final String TYPE = "type";
        public static final String ID = "id";
        public static final String MEDIA_TYPES = "mediaTypes";

        // Items filtering/sorting
        public static final String TAG_ID = "tag_id";
        public static final String GENRE_ID = "genre_id";
        public static final String ROWS = "rows";
        public static final String SORT_BY = "sortBy";
        public static final String SORT_ORDER = "sortOrder";

        private RequestParams() {}
    }

    // ============= Actions =============
    public static final class Actions {
        public static final String LOGIN = "login";
        public static final String LOGOUT = "logout";
        public static final String REGISTER = "register";
        public static final String ADD = "add";
        public static final String UPDATE = "update";
        public static final String DELETE = "delete";

        private Actions() {}
    }

    // ============= Headers =============
    public static final class Headers {
        public static final String X_REQUESTED_WITH = "X-Requested-With";
        public static final String X_REQUESTED_WITH_AJAX = "X-Requested-With-AJAX";
        public static final String XML_HTTP_REQUEST = "XMLHttpRequest";
        public static final String REFERER = "Referer";

        private Headers() {}
    }

    // ============= Content Types =============
    public static final class ContentTypes {
        public static final String JSON = "application/json";
        public static final String HTML = "text/html";

        private ContentTypes() {}
    }

    // ============= Encodings =============
    public static final class Encodings {
        public static final String UTF_8 = "UTF-8";

        private Encodings() {}
    }

    // ============= Paths =============
    public static final class Paths {
        // JSP Pages
        public static final String INDEX_JSP = "/jsp/index.jsp";
        public static final String DASHBOARD_JSP = "/jsp/dashboard.jsp";
        public static final String USERS_JSP = "/jsp/users.jsp";

        // Templates
        public static final String PROFILE_TEMPLATE = "/WEB-INF/templates/profile.jsp";
        public static final String LIST_RATINGS_TEMPLATE = "/WEB-INF/templates/listRatingsTemplates.jsp";
        public static final String ERROR_PAGE = "/WEB-INF/error.jsp";

        // Admin Pages
        public static final String ADMIN_DASHBOARD = "/WEB-INF/admin/adminDashboard.jsp";
        public static final String ADMIN_USER_LIST = "/WEB-INF/admin/userList.jsp";
        public static final String ADMIN_MANAGEMENT = "/WEB-INF/admin/management.jsp";

        // Servlet Paths
        public static final String ADMIN_USERS_SERVLET = "/admin/users";
        public static final String PROFILE_SERVLET = "/profile";
        public static final String GAMES_SERVLET = "/games";
        public static final String MOVIES_SERVLET = "/movies";
        public static final String BOOKS_SERVLET = "/books";
        public static final String SHOWS_SERVLET = "/shows";

        private Paths() {}
    }

    // ============= Service Names =============
    public static final class ServiceNames {
        public static final String USER_SERVICE = "userService";
        public static final String GAME_SERVICE = "gameService";
        public static final String MOVIE_SERVICE = "movieService";
        public static final String BOOK_SERVICE = "bookService";
        public static final String SHOW_SERVICE = "showService";
        public static final String TAG_SERVICE = "tagService";
        public static final String GENRE_SERVICE = "genreService";
        public static final String DASHBOARD_SERVICE = "dashboardService";

        private ServiceNames() {}
    }

    // ============= Default Values =============
    public static final class Defaults {
        public static final int PAGE = 1;
        public static final int PAGE_SIZE_PROFILE = 15;
        public static final int PAGE_SIZE_ADMIN = 10;
        public static final int PAGE_SIZE_PUBLIC = 20;
        public static final int PAGE_SIZE_ITEMS = 10;
        public static final String DEFAULT_TAB = "games";
        public static final String DEFAULT_SORT_BY = "name";
        public static final String DEFAULT_SORT_ORDER = "asc";
        public static final String PRIVACY_PUBLIC = "public";

        private Defaults() {}
    }

    // ============= Categories =============
    public static final class Categories {
        public static final String GAMES = "games";
        public static final String MOVIES = "movies";
        public static final String BOOKS = "books";
        public static final String SHOWS = "shows";

        private Categories() {}
    }

    // ============= Entity Names =============
    public static final class EntityNames {
        public static final String GAME_SINGULAR = "Game";
        public static final String GAME_PLURAL = "Games";
        public static final String MOVIE_SINGULAR = "Movie";
        public static final String MOVIE_PLURAL = "Movies";
        public static final String BOOK_SINGULAR = "Book";
        public static final String BOOK_PLURAL = "Books";
        public static final String SHOW_SINGULAR = "Show";
        public static final String SHOW_PLURAL = "Shows";
        public static final String TAG = "Tag";
        public static final String GENRE = "Genre";

        private EntityNames() {}
    }

    // ============= Management Types =============
    public static final class ManagementTypes {
        public static final String TAGS = "tags";
        public static final String GENRES = "genres";

        private ManagementTypes() {}
    }
}