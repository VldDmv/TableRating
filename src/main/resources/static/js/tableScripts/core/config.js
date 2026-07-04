/**
 * Configuration for category pages.
 */

const COMMON_SELECTORS = {
    rowsPerPageSelect: '#rowsPerPage',
    searchBox: '#searchBox',
    prevPageButton: '#prevPage',
    nextPageButton: '#nextPage',
    pageDropdown: '#pageDropdown',
    pageList: '#pageList',
    statusButtonClass: '.status-button',
    editButton: '#edit-button',
    deleteButtonClass: '.delete-button',
    editIconButtonClass: '.edit-button',
    deleteIconButtonClass: '.delete-button',
};

const COMMON_CONFIG = {
    csrfTokenName: '_csrf_token',
    csrfParameterName: '_csrf',
    itemNameAttribute: 'data-item-name',
};

const COMMON_VALIDATORS = {
    validateScore: (scoreValue, entityName = 'Item') => {
        const score = parseInt(scoreValue, 10);
        if (isNaN(score) || score < 1 || score > 100) {
            alert(`${entityName} score must be a number between 1 and 100.`);
            return false;
        }
        return true;
    },
};

const createScoreStyling = (thresholds) => (tableBody) => {
    if (!tableBody) return;
    tableBody.querySelectorAll('.score-cell').forEach((cell) => {
        cell.classList.remove('score-low', 'score-medium', 'score-high');
        const score = parseInt(cell.textContent.trim(), 10);
        if (!isNaN(score)) {
            if (score <= thresholds.low) cell.classList.add('score-low');
            else if (score <= thresholds.medium) cell.classList.add('score-medium');
            else cell.classList.add('score-high');
        }
    });
};

const COMMON_COLUMNS = {
    cover: {
        name: 'Cover',
        key: 'cover',
        index: 0,
        sortable: false,
        hideable: true,
    },
    name: {
        name: 'Name',
        key: 'name',
        index: 1,
        type: 'string',
        searchable: true,
        sortable: true,
        hideable: false,
    },
    score: {
        name: 'Score',
        key: 'score',
        index: 2,
        type: 'number',
        cellSelector: '.score-cell',
        sortable: true,
        hideable: false,
    },
    status: {
        name: 'Status',
        key: 'status',
        index: 4,
        type: 'status',
        sortable: true,
        hideable: true,
    },
    actions: {
        name: 'Delete',
        key: 'actions',
        index: 5,
        hideable: true,
        sortable: false,
    },
};

export const ENTITY_CONFIGS = {
    games: {
        entityType: 'games',
        entityNameSingular: 'Game',
        apiEndpoint: '/api/category/games',
        filterParamName: 'categoryId',
        selectors: {
            tableBody: '#gamesBody',
            addForm: '#add-game-form',
            scoreInput: '#gameScore',
            ...COMMON_SELECTORS,
        },
        paramNames: {
            addItemName: 'gameName',
            addItemScore: 'gameScore',
            addItemTagIds: 'gameTagIds',
            toggleItemStatus: 'toggleGameStatus',
            removeItem: 'removeGame',
            oldItemName: 'oldGameName',
            updatedItemName: 'updatedGameName',
            updatedItemScore: 'updatedGameScore',
            updatedItemTagIds: 'updatedGameTagIds',
        },
        validateScore: (val) => COMMON_VALIDATORS.validateScore(val, 'Game'),
        applyScoreStyling: createScoreStyling({ low: 49, medium: 74 }),
        columns: {
            ...COMMON_COLUMNS,
            tags: {
                name: 'Tags',
                key: 'tags',
                index: 3,
                type: 'string',
                searchable: true,
                sortable: false,
                hideable: true,
            },
        },
        ...COMMON_CONFIG,
    },

    movies: {
        entityType: 'movies',
        entityNameSingular: 'Movie',
        apiEndpoint: 'category',
        filterParamName: 'categoryId',
        selectors: {
            tableBody: '#moviesBody',
            addForm: '#add-movie-form',
            scoreInput: '#movieScore',
            ...COMMON_SELECTORS,
        },
        paramNames: {
            addItemName: 'movieName',
            addItemScore: 'movieScore',
            addItemTagIds: 'genreIds',
            toggleItemStatus: 'toggleMovieStatus',
            removeItem: 'removeMovie',
            oldItemName: 'oldMovieName',
            updatedItemName: 'updatedMovieName',
            updatedItemScore: 'updatedMovieScore',
            updatedItemTagIds: 'genreIds',
        },
        validateScore: (val) => COMMON_VALIDATORS.validateScore(val, 'Movie'),
        applyScoreStyling: createScoreStyling({ low: 39, medium: 60 }),
        columns: {
            ...COMMON_COLUMNS,
            tags: {
                name: 'Genres',
                key: 'tags',
                index: 3,
                type: 'string',
                searchable: true,
                sortable: false,
                hideable: true,
            },
        },
        ...COMMON_CONFIG,
    },

    shows: {
        entityType: 'shows',
        entityNameSingular: 'Show',
        apiEndpoint: 'category',
        filterParamName: 'categoryId',
        selectors: {
            tableBody: '#showsBody',
            addForm: '#add-show-form',
            scoreInput: '#showScore',
            ...COMMON_SELECTORS,
        },
        paramNames: {
            addItemName: 'showName',
            addItemScore: 'showScore',
            addItemTagIds: 'genreIds',
            toggleItemStatus: 'toggleShowStatus',
            removeItem: 'removeShow',
            oldItemName: 'oldShowName',
            updatedItemName: 'updatedShowName',
            updatedItemScore: 'updatedShowScore',
            updatedItemTagIds: 'genreIds',
        },
        validateScore: (val) => COMMON_VALIDATORS.validateScore(val, 'Show'),
        applyScoreStyling: createScoreStyling({ low: 39, medium: 60 }),
        columns: {
            ...COMMON_COLUMNS,
            tags: {
                name: 'Genres',
                key: 'tags',
                index: 3,
                type: 'string',
                searchable: true,
                sortable: false,
                hideable: true,
            },
        },
        ...COMMON_CONFIG,
    },

    books: {
        entityType: 'books',
        entityNameSingular: 'Book',
        apiEndpoint: 'category',
        filterParamName: 'categoryId',
        selectors: {
            tableBody: '#booksBody',
            addForm: '#add-book-form',
            scoreInput: '#bookScore',
            ...COMMON_SELECTORS,
        },
        paramNames: {
            addItemName: 'bookName',
            addItemScore: 'bookScore',
            addItemTagIds: 'genreIds',
            toggleItemStatus: 'toggleBookStatus',
            removeItem: 'removeBook',
            oldItemName: 'oldBookName',
            updatedItemName: 'updatedBookName',
            updatedItemScore: 'updatedBookScore',
            updatedItemTagIds: 'genreIds',
        },
        validateScore: (val) => COMMON_VALIDATORS.validateScore(val, 'Book'),
        applyScoreStyling: createScoreStyling({ low: 49, medium: 79 }),
        columns: {
            ...COMMON_COLUMNS,
            tags: {
                name: 'Genres',
                key: 'tags',
                index: 3,
                type: 'string',
                searchable: true,
                sortable: false,
                hideable: true,
            },
        },
        ...COMMON_CONFIG,
    },
};

export function getCurrentConfig() {
    return window.categoryConfig || null;
}
