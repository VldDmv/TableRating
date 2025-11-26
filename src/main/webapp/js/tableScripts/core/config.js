/**
 * Configuration for category pages, defining selectors, parameters, and validation.
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
    editIconButtonClass: '.btn--edit',
    deleteIconButtonClass: '.btn--delete'

};

const COMMON_CONFIG = {
    csrfTokenName: '_csrf_token',
    csrfParameterName: '_csrf',
    itemNameAttribute: 'data-item-name'
};

const COMMON_VALIDATORS = {
    validateScore: (scoreValue, entityName = 'Item') => {
        const score = parseInt(scoreValue, 10);
        if (isNaN(score) || score < 1 || score > 100) {
            alert(`${entityName} score must be a number between 1 and 100.`);
            return false;
        }
        return true;
    }
};

const createScoreStyling = (thresholds) => (tableBody) => {
    if (!tableBody) return;
    tableBody.querySelectorAll(".score-cell").forEach(cell => {
        cell.classList.remove("score-low", "score-medium", "score-high");
        const score = parseInt(cell.textContent.trim(), 10);
        if (!isNaN(score)) {
            if (score <= thresholds.low) cell.classList.add("score-low");
            else if (score <= thresholds.medium) cell.classList.add("score-medium");
            else cell.classList.add("score-high");
        }
    });
};

export const ENTITY_CONFIGS = {
    games: {
        entityType: 'games',
        entityNameSingular: 'Game',
        apiEndpoint: 'category',
        selectors: {
            tableBody: '#gamesBody',
            addForm: '#add-game-form',
            scoreInput: '#gameScore',
            ...COMMON_SELECTORS
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
            updatedItemTagIds: 'updatedGameTagIds'
        },
        validateScore: (val) => COMMON_VALIDATORS.validateScore(val, 'Game'),
        applyScoreStyling: createScoreStyling({ low: 49, medium: 74 }),
        columns: {
            name: { name: 'Name', key: 'name', index: 0, type: 'string', searchable: true, sortable: true, hideable: false },
            score: { name: 'Score', key: 'score', index: 1, type: 'number', cellSelector: '.score-cell', sortable: true, hideable: false },
            tags: { name: 'Tags', key: 'tags', index: 2, type: 'string', searchable: true, sortable: false, hideable: true },
            completed: { name: 'Completed', key: 'completed', index: 3, type: 'status', sortable: true, hideable: true },
            actions: { name: 'Delete', key: 'actions', hideable: true, sortable: false }
        },
        ...COMMON_CONFIG
    },
    movies: {
        entityType: 'movies',
        entityNameSingular: 'Movie',
        apiEndpoint: 'category',
        selectors: {
            tableBody: '#moviesBody',
            addForm: '#add-movie-form',
            scoreInput: '#movieScore',
            ...COMMON_SELECTORS
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
            updatedItemTagIds: 'genreIds'
        },
        validateScore: (val) => COMMON_VALIDATORS.validateScore(val, 'Movie'),
        applyScoreStyling: createScoreStyling({ low: 39, medium: 60 }),
        columns: {
            name: { name: 'Name', key: 'name', index: 0, type: 'string', searchable: true, sortable: true, hideable: false },
            score: { name: 'Score', key: 'score', index: 1, type: 'number', cellSelector: '.score-cell', sortable: true, hideable: false },
            tags: { name: 'Genres', key: 'tags', index: 2, type: 'string', searchable: true, sortable: false, hideable: true },
            completed: { name: 'Completed', key: 'completed', index: 3, type: 'status', sortable: true, hideable: true },
            actions: { name: 'Delete', key: 'actions', hideable: true, sortable: false }
        },
        ...COMMON_CONFIG
    },
    shows: {
        entityType: 'shows',
        entityNameSingular: 'Show',
        apiEndpoint: 'category',
        selectors: {
            tableBody: '#showsBody',
            addForm: '#add-show-form',
            scoreInput: '#showScore',
            ...COMMON_SELECTORS
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
            updatedItemTagIds: 'genreIds'
        },
        validateScore: (val) => COMMON_VALIDATORS.validateScore(val, 'Show'),
        applyScoreStyling: createScoreStyling({ low: 39, medium: 60 }),
        columns: {
            name: { name: 'Name', key: 'name', index: 0, type: 'string', searchable: true, sortable: true, hideable: false },
            score: { name: 'Score', key: 'score', index: 1, type: 'number', cellSelector: '.score-cell', sortable: true, hideable: false },
            tags: { name: 'Genres', key: 'tags', index: 2, type: 'string', searchable: true, sortable: false, hideable: true },
            completed: { name: 'Completed', key: 'completed', index: 3, type: 'status', sortable: true, hideable: true },
            actions: { name: 'Delete', key: 'actions', hideable: true, sortable: false }
        },
        ...COMMON_CONFIG
    },
    books: {
        entityType: 'books',
        entityNameSingular: 'Book',
        apiEndpoint: 'category',
        selectors: {
            tableBody: '#booksBody',
            addForm: '#add-book-form',
            scoreInput: '#bookScore',
            ...COMMON_SELECTORS
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
            updatedItemTagIds: 'genreIds'
        },
        validateScore: (val) => COMMON_VALIDATORS.validateScore(val, 'Book'),
        applyScoreStyling: createScoreStyling({ low: 49, medium: 79 }),
        columns: {
            name: { name: 'Name', key: 'name', index: 0, type: 'string', searchable: true, sortable: true, hideable: false },
            score: { name: 'Score', key: 'score', index: 1, type: 'number', cellSelector: '.score-cell', sortable: true, hideable: false },
            tags: { name: 'Genres', key: 'tags', index: 2, type: 'string', searchable: true, sortable: false, hideable: true },
            completed: { name: 'Completed', key: 'completed', index: 3, type: 'status', sortable: true, hideable: true },
            actions: { name: 'Delete', key: 'actions', hideable: true, sortable: false }
        },
        ...COMMON_CONFIG
    }
};

export function getCurrentConfig() {
    const entityTypeMeta = document.querySelector('meta[name="_entity_type"]');
    const entityType = entityTypeMeta ? entityTypeMeta.getAttribute('content') : null;
    return ENTITY_CONFIGS[entityType] || null;
}