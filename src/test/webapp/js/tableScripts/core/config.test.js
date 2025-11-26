import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';
import { ENTITY_CONFIGS, getCurrentConfig } from '../../../../../main/webapp/js/tableScripts/core/config.js';

describe('ENTITY_CONFIGS', () => {
    test('should have configs for all entity types', () => {
        expect(ENTITY_CONFIGS.games).toBeDefined();
        expect(ENTITY_CONFIGS.movies).toBeDefined();
        expect(ENTITY_CONFIGS.shows).toBeDefined();
        expect(ENTITY_CONFIGS.books).toBeDefined();
    });

    test('games config should have correct structure', () => {
        const config = ENTITY_CONFIGS.games;

        expect(config.entityType).toBe('games');
        expect(config.entityNameSingular).toBe('Game');
        expect(config.apiEndpoint).toBe('category');
        expect(config.selectors).toBeDefined();
        expect(config.paramNames).toBeDefined();
        expect(config.columns).toBeDefined();
    });

    test('should have correct selectors for games', () => {
        const selectors = ENTITY_CONFIGS.games.selectors;

        expect(selectors.tableBody).toBe('#gamesBody');
        expect(selectors.addForm).toBe('#add-game-form');
        expect(selectors.scoreInput).toBe('#gameScore');
        expect(selectors.rowsPerPageSelect).toBe('#rowsPerPage');
        expect(selectors.searchBox).toBe('#searchBox');
        expect(selectors.statusButtonClass).toBe('.status-button');
        expect(selectors.deleteButtonClass).toBe('.delete-button');
        expect(selectors.editIconButtonClass).toBe('.btn--edit');
        expect(selectors.deleteIconButtonClass).toBe('.btn--delete');
    });

    test('should have correct paramNames for games', () => {
        const params = ENTITY_CONFIGS.games.paramNames;

        expect(params.addItemName).toBe('gameName');
        expect(params.addItemScore).toBe('gameScore');
        expect(params.addItemTagIds).toBe('gameTagIds');
        expect(params.toggleItemStatus).toBe('toggleGameStatus');
        expect(params.removeItem).toBe('removeGame');
        expect(params.oldItemName).toBe('oldGameName');
        expect(params.updatedItemName).toBe('updatedGameName');
        expect(params.updatedItemScore).toBe('updatedGameScore');
        expect(params.updatedItemTagIds).toBe('updatedGameTagIds');
    });

    test('should have correct columns configuration', () => {
        const columns = ENTITY_CONFIGS.games.columns;

        expect(columns.name).toBeDefined();
        expect(columns.name.index).toBe(0);
        expect(columns.name.sortable).toBe(true);
        expect(columns.name.hideable).toBe(false);

        expect(columns.score).toBeDefined();
        expect(columns.score.index).toBe(1);
        expect(columns.score.sortable).toBe(true);

        expect(columns.tags).toBeDefined();
        expect(columns.tags.index).toBe(2);
        expect(columns.tags.hideable).toBe(true);

        expect(columns.completed).toBeDefined();
        expect(columns.actions).toBeDefined();
    });

    test('should validate score correctly for games', () => {
        global.alert = jest.fn();

        expect(ENTITY_CONFIGS.games.validateScore('50')).toBe(true);
        expect(ENTITY_CONFIGS.games.validateScore('1')).toBe(true);
        expect(ENTITY_CONFIGS.games.validateScore('100')).toBe(true);

        expect(ENTITY_CONFIGS.games.validateScore('0')).toBe(false);
        expect(ENTITY_CONFIGS.games.validateScore('101')).toBe(false);
        expect(ENTITY_CONFIGS.games.validateScore('abc')).toBe(false);
        expect(ENTITY_CONFIGS.games.validateScore('')).toBe(false);

        expect(global.alert).toHaveBeenCalled();
    });

    test('should apply score styling for games', () => {
        const tableBody = document.createElement('tbody');
        tableBody.innerHTML = `
            <tr><td><span class="score-cell">30</span></td></tr>
            <tr><td><span class="score-cell">60</span></td></tr>
            <tr><td><span class="score-cell">90</span></td></tr>
        `;

        ENTITY_CONFIGS.games.applyScoreStyling(tableBody);

        const cells = tableBody.querySelectorAll('.score-cell');
        expect(cells[0].classList.contains('score-low')).toBe(true);
        expect(cells[1].classList.contains('score-medium')).toBe(true);
        expect(cells[2].classList.contains('score-high')).toBe(true);
    });

    test('movies config should use genre_id for filtering', () => {
        const config = ENTITY_CONFIGS.movies;

        expect(config.entityType).toBe('movies');
        expect(config.paramNames.addItemTagIds).toBe('genreIds');
        expect(config.columns.tags.name).toBe('Genres');
    });

    test('shows config should have correct score thresholds', () => {
        const tableBody = document.createElement('tbody');
        tableBody.innerHTML = `
            <tr><td><span class="score-cell">35</span></td></tr>
            <tr><td><span class="score-cell">50</span></td></tr>
            <tr><td><span class="score-cell">70</span></td></tr>
        `;

        ENTITY_CONFIGS.shows.applyScoreStyling(tableBody);

        const cells = tableBody.querySelectorAll('.score-cell');
        expect(cells[0].classList.contains('score-low')).toBe(true);
        expect(cells[1].classList.contains('score-medium')).toBe(true);
        expect(cells[2].classList.contains('score-high')).toBe(true);
    });

    test('books config should have correct score thresholds', () => {
        const tableBody = document.createElement('tbody');
        tableBody.innerHTML = `
            <tr><td><span class="score-cell">45</span></td></tr>
            <tr><td><span class="score-cell">65</span></td></tr>
            <tr><td><span class="score-cell">85</span></td></tr>
        `;

        ENTITY_CONFIGS.books.applyScoreStyling(tableBody);

        const cells = tableBody.querySelectorAll('.score-cell');
        expect(cells[0].classList.contains('score-low')).toBe(true);
        expect(cells[1].classList.contains('score-medium')).toBe(true);
        expect(cells[2].classList.contains('score-high')).toBe(true);
    });

    test('should have CSRF config in all entities', () => {
        Object.values(ENTITY_CONFIGS).forEach(config => {
            expect(config.csrfTokenName).toBe('_csrf_token');
            expect(config.csrfParameterName).toBe('_csrf');
            expect(config.itemNameAttribute).toBe('data-item-name');
        });
    });
});

describe('getCurrentConfig', () => {
    beforeEach(() => {
        document.head.innerHTML = '';
    });

    test('should return games config when meta tag is set to games', () => {
        const meta = document.createElement('meta');
        meta.name = '_entity_type';
        meta.content = 'games';
        document.head.appendChild(meta);

        const config = getCurrentConfig();

        expect(config).toBe(ENTITY_CONFIGS.games);
        expect(config.entityType).toBe('games');
    });

    test('should return movies config when meta tag is set to movies', () => {
        const meta = document.createElement('meta');
        meta.name = '_entity_type';
        meta.content = 'movies';
        document.head.appendChild(meta);

        const config = getCurrentConfig();

        expect(config).toBe(ENTITY_CONFIGS.movies);
        expect(config.entityType).toBe('movies');
    });

    test('should return shows config when meta tag is set to shows', () => {
        const meta = document.createElement('meta');
        meta.name = '_entity_type';
        meta.content = 'shows';
        document.head.appendChild(meta);

        const config = getCurrentConfig();

        expect(config).toBe(ENTITY_CONFIGS.shows);
    });

    test('should return books config when meta tag is set to books', () => {
        const meta = document.createElement('meta');
        meta.name = '_entity_type';
        meta.content = 'books';
        document.head.appendChild(meta);

        const config = getCurrentConfig();

        expect(config).toBe(ENTITY_CONFIGS.books);
    });

    test('should return null when meta tag is not present', () => {
        const config = getCurrentConfig();

        expect(config).toBeNull();
    });

    test('should return null for unknown entity type', () => {
        const meta = document.createElement('meta');
        meta.name = '_entity_type';
        meta.content = 'unknown';
        document.head.appendChild(meta);

        const config = getCurrentConfig();

        expect(config).toBeNull();
    });

    test('should return null when meta content is empty', () => {
        const meta = document.createElement('meta');
        meta.name = '_entity_type';
        meta.content = '';
        document.head.appendChild(meta);

        const config = getCurrentConfig();

        expect(config).toBeNull();
    });
});

describe('Score Styling Edge Cases', () => {
    test('should handle non-numeric scores gracefully', () => {
        const tableBody = document.createElement('tbody');
        tableBody.innerHTML = `
            <tr><td><span class="score-cell">abc</span></td></tr>
            <tr><td><span class="score-cell"></span></td></tr>
            <tr><td><span class="score-cell">50</span></td></tr>
        `;

        expect(() => {
            ENTITY_CONFIGS.games.applyScoreStyling(tableBody);
        }).not.toThrow();

        const cells = tableBody.querySelectorAll('.score-cell');
        expect(cells[0].classList.length).toBe(1);
        expect(cells[1].classList.length).toBe(1);
        expect(cells[2].classList.contains('score-medium')).toBe(true);
    });

    test('should handle missing tableBody gracefully', () => {
        expect(() => {
            ENTITY_CONFIGS.games.applyScoreStyling(null);
        }).not.toThrow();

        expect(() => {
            ENTITY_CONFIGS.games.applyScoreStyling(undefined);
        }).not.toThrow();
    });

    test('should remove previous styling classes before applying new ones', () => {
        const tableBody = document.createElement('tbody');
        tableBody.innerHTML = `
            <tr><td><span class="score-cell score-high">30</span></td></tr>
        `;

        ENTITY_CONFIGS.games.applyScoreStyling(tableBody);

        const cell = tableBody.querySelector('.score-cell');
        expect(cell.classList.contains('score-high')).toBe(false);
        expect(cell.classList.contains('score-low')).toBe(true);
    });

    test('should handle boundary values correctly', () => {
        const tableBody = document.createElement('tbody');
        tableBody.innerHTML = `
            <tr><td><span class="score-cell">49</span></td></tr>
            <tr><td><span class="score-cell">50</span></td></tr>
            <tr><td><span class="score-cell">74</span></td></tr>
            <tr><td><span class="score-cell">75</span></td></tr>
        `;

        ENTITY_CONFIGS.games.applyScoreStyling(tableBody);

        const cells = tableBody.querySelectorAll('.score-cell');
        expect(cells[0].classList.contains('score-low')).toBe(true);
        expect(cells[1].classList.contains('score-medium')).toBe(true);
        expect(cells[2].classList.contains('score-medium')).toBe(true);
        expect(cells[3].classList.contains('score-high')).toBe(true);
    });
});