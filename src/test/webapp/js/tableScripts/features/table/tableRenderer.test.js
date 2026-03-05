

import { jest } from '@jest/globals';

jest.unstable_mockModule('@/tableScripts/core/utils.js', () => ({
    htmlUtils: { escape: (s) => s, decode: (s) => s },
    ICONS: {
        COMPLETED:     '✅',
        NOT_COMPLETED: '❌',
        EDIT:          '✏️',
        DELETE:        '🗑️'
    }
}));

const { TableRenderer } = await import('@/tableScripts/features/table/tableRenderer.js');

// ─── Helpers ──────────────────────────────────────────────────────────────────

function makeConfig(overrides = {}) {
    return {
        entityType:        'games',
        hideActions:        false,
        applyScoreStyling:  jest.fn(),
        ...overrides
    };
}

function makeItem(overrides = {}) {
    return {
        name:      'Witcher 3',
        score:     95,
        completed: false,
        coverUrl:  '',
        tags:      [],
        ...overrides
    };
}

// ─── render ───────────────────────────────────────────────────────────────────

describe('TableRenderer.render()', () => {
    let tableBody;
    let renderer;

    beforeEach(() => {
        tableBody = document.createElement('tbody');
        renderer  = new TableRenderer(makeConfig(), tableBody);
    });

    test('renders one row per item', () => {
        renderer.render([makeItem(), makeItem({ name: 'Dark Souls' })]);
        expect(tableBody.querySelectorAll('tr').length).toBe(2);
    });

    test('calls renderEmpty when items array is empty', () => {
        const spy = jest.spyOn(renderer, 'renderEmpty');
        renderer.render([]);
        expect(spy).toHaveBeenCalled();
    });

    test('calls renderEmpty when items is null', () => {
        const spy = jest.spyOn(renderer, 'renderEmpty');
        renderer.render(null);
        expect(spy).toHaveBeenCalled();
    });

    test('calls applyScoreStyling after rendering items', () => {
        renderer.render([makeItem()]);
        expect(renderer.config.applyScoreStyling).toHaveBeenCalledWith(tableBody);
    });
});

// ─── renderEmpty ──────────────────────────────────────────────────────────────

describe('TableRenderer.renderEmpty()', () => {
    test('renders a single row with a no-items message', () => {
        const tableBody = document.createElement('tbody');
        const renderer  = new TableRenderer(makeConfig(), tableBody);
        renderer.renderEmpty();
        const rows = tableBody.querySelectorAll('tr');
        expect(rows.length).toBe(1);
        expect(rows[0].textContent).toContain('No items');
    });
});

// ─── renderLoading ────────────────────────────────────────────────────────────

describe('TableRenderer.renderLoading()', () => {
    test('renders a single loading row', () => {
        const tableBody = document.createElement('tbody');
        const renderer  = new TableRenderer(makeConfig(), tableBody);
        renderer.renderLoading();
        const rows = tableBody.querySelectorAll('tr');
        expect(rows.length).toBe(1);
        expect(rows[0].textContent).toContain('Loading');
    });
});

// ─── createRow ────────────────────────────────────────────────────────────────

describe('TableRenderer.createRow()', () => {
    let tableBody;
    let renderer;

    beforeEach(() => {
        tableBody = document.createElement('tbody');
        renderer  = new TableRenderer(makeConfig(), tableBody);
    });

    test('returns a TR element', () => {
        expect(renderer.createRow(makeItem()).tagName).toBe('TR');
    });

    test('stores originalName in dataset', () => {
        const row = renderer.createRow(makeItem({ name: 'Elden Ring' }));
        expect(row.dataset.originalName).toBe('Elden Ring');
    });

    test('stores originalScore in dataset', () => {
        const row = renderer.createRow(makeItem({ score: 88 }));
        expect(row.dataset.originalScore).toBe('88');
    });

    test('stores originalTagIds as comma-separated IDs', () => {
        const row = renderer.createRow(
            makeItem({ tags: [{ id: 1, name: 'Action' }, { id: 2, name: 'RPG' }] })
        );
        expect(row.dataset.originalTagIds).toBe('1,2');
    });

    test('has 6 cells: cover, name, score, tags, completed, actions', () => {
        expect(renderer.createRow(makeItem()).children.length).toBe(6);
    });

    test('score cell contains a .score-cell span with the score', () => {
        const row       = renderer.createRow(makeItem({ score: 77 }));
        const scoreCell = row.querySelector('.score-cell');
        expect(scoreCell).not.toBeNull();
        expect(scoreCell.textContent).toBe('77');
    });

    test('renders cover placeholder when no coverUrl', () => {
        expect(renderer.createRow(makeItem({ coverUrl: '' })).querySelector('.cover-placeholder'))
            .not.toBeNull();
    });

    test('renders cover img when coverUrl is set', () => {
        const img = renderer.createRow(makeItem({ coverUrl: 'https://img.jpg' }))
            .querySelector('.cover-thumbnail');
        expect(img).not.toBeNull();
        expect(img.src).toContain('img.jpg');
    });

    test('uses genres array when tags is undefined', () => {
        const row = renderer.createRow(
            makeItem({ genres: [{ id: 5, name: 'Drama' }], tags: undefined })
        );
        expect(row.dataset.originalTagIds).toBe('5');
    });

    test('status button shows COMPLETED icon for completed item', () => {
        const btn = renderer.createRow(makeItem({ completed: true })).querySelector('.status-button');
        expect(btn.innerHTML.trim()).toBe('✅');
    });

    test('status button shows NOT_COMPLETED icon for incomplete item', () => {
        const btn = renderer.createRow(makeItem({ completed: false })).querySelector('.status-button');
        expect(btn.innerHTML.trim()).toBe('❌');
    });
});

// ─── _createTagsHtml ──────────────────────────────────────────────────────────

describe('TableRenderer._createTagsHtml()', () => {
    let renderer;

    beforeEach(() => {
        renderer = new TableRenderer(makeConfig(), document.createElement('tbody'));
    });

    test('returns empty string for empty array', () => {
        expect(renderer._createTagsHtml([])).toBe('');
    });

    test('returns empty string for null', () => {
        expect(renderer._createTagsHtml(null)).toBe('');
    });

    test('skips items without a name', () => {
        const html = renderer._createTagsHtml([{ id: 1, name: null }, { id: 2, name: 'RPG' }]);
        expect(html).not.toContain('null');
        expect(html).toContain('RPG');
    });

    test('renders tag-badge spans', () => {
        const html = renderer._createTagsHtml([{ id: 1, name: 'Action' }]);
        expect(html).toContain('class="tag-badge"');
        expect(html).toContain('Action');
    });

    test('includes data-tag-id attribute on each badge', () => {
        const html = renderer._createTagsHtml([{ id: 42, name: 'Horror' }]);
        expect(html).toContain('data-tag-id="42"');
    });
});