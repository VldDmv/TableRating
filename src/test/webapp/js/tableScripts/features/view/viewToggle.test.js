
import { jest } from '@jest/globals';

const { ViewToggleManager } = await import('@/tableScripts/features/view/viewToggle.js');

// ─── Helpers ──────────────────────────────────────────────────────────────────

function makeConfig(entityType = 'games') {
    return { entityType };
}

function setupDOM() {
    document.body.innerHTML = `
        <table id="mainTable" style="display:table;"></table>
        <div class="table-controls"></div>
    `;
}

function makeManager(config = makeConfig(), getItems = () => []) {
    const tableRenderer = { render: jest.fn(), renderLoading: jest.fn() };
    const cardRenderer  = { render: jest.fn(), renderLoading: jest.fn() };
    return new ViewToggleManager(config, tableRenderer, cardRenderer, getItems);
}

// ─── Preference storage ───────────────────────────────────────────────────────

describe('ViewToggleManager preferences', () => {
    beforeEach(() => {
        setupDOM();
        localStorage.clear();
    });

    test('defaults to "table" when no saved preference', () => {
        const m = makeManager();
        expect(m.getCurrentView()).toBe('table');
    });

    test('loads "cards" from localStorage', () => {
        localStorage.setItem('view-preference-games', 'cards');
        const m = makeManager();
        expect(m.getCurrentView()).toBe('cards');
    });

    test('ignores invalid saved preference, defaults to "table"', () => {
        localStorage.setItem('view-preference-games', 'invalid');
        const m = makeManager();
        expect(m.getCurrentView()).toBe('table');
    });

    test('saves preference to localStorage', () => {
        const m = makeManager();
        m.switchView('cards');
        expect(localStorage.getItem('view-preference-games')).toBe('cards');
    });

    test('uses entityType in storage key', () => {
        localStorage.setItem('view-preference-books', 'cards');
        const m = makeManager(makeConfig('books'));
        expect(m.getCurrentView()).toBe('cards');
    });
});

// ─── State helpers ────────────────────────────────────────────────────────────

describe('ViewToggleManager state helpers', () => {
    beforeEach(() => {
        setupDOM();
        localStorage.clear();
    });

    test('isTableView returns true for table view', () => {
        const m = makeManager();
        m.switchView('table');
        expect(m.isTableView()).toBe(true);
        expect(m.isCardsView()).toBe(false);
    });

    test('isCardsView returns true for cards view', () => {
        const m = makeManager();
        m.switchView('cards');
        expect(m.isCardsView()).toBe(true);
        expect(m.isTableView()).toBe(false);
    });
});

// ─── switchView ───────────────────────────────────────────────────────────────

describe('ViewToggleManager.switchView', () => {
    beforeEach(() => {
        setupDOM();
        localStorage.clear();
    });

    test('updates currentView to "cards"', () => {
        const m = makeManager();
        m.switchView('cards');
        expect(m.getCurrentView()).toBe('cards');
    });

    test('updates currentView to "table"', () => {
        localStorage.setItem('view-preference-games', 'cards');
        const m = makeManager();
        m.switchView('table');
        expect(m.getCurrentView()).toBe('table');
    });

    test('logs error and does not change view for unknown view name', () => {
        const m      = makeManager();
        const errSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        m.switchView('list'); // unknown
        expect(m.getCurrentView()).toBe('table'); // unchanged
        expect(errSpy).toHaveBeenCalled();
        errSpy.mockRestore();
    });

    test('calls onViewChange callback', () => {
        const m        = makeManager();
        const onChange = jest.fn();
        m.onViewChange = onChange;
        m.switchView('cards');
        expect(onChange).toHaveBeenCalledWith('cards');
    });
});

// ─── applyView / DOM ─────────────────────────────────────────────────────────

describe('ViewToggleManager.applyView DOM effects', () => {
    beforeEach(() => {
        setupDOM();
        localStorage.clear();
    });

    test('hides table and shows cards-container for cards view', () => {
        const m = makeManager();
        m.switchView('cards');

        const table = document.querySelector('table');
        expect(table.style.display).toBe('none');

        const cards = document.getElementById('cards-container');
        expect(cards).not.toBeNull();
        expect(cards.style.display).toBe('grid');
    });

    test('shows table and hides cards-container for table view', () => {
        const m = makeManager();
        m.switchView('cards');
        m.switchView('table');

        const table = document.querySelector('table');
        expect(table.style.display).toBe('table');

        const cards = document.getElementById('cards-container');
        if (cards) {
            expect(cards.style.display).toBe('none');
        }
    });

    test('renders items via cardRenderer when switching to cards', () => {
        const items       = [{ name: 'Game1' }, { name: 'Game2' }];
        const tableRend   = { render: jest.fn(), renderLoading: jest.fn() };
        const cardRend    = { render: jest.fn(), renderLoading: jest.fn() };
        const m = new ViewToggleManager(makeConfig(), tableRend, cardRend, () => items);
        m.switchView('cards');

        expect(cardRend.render).toHaveBeenCalledWith(items);
    });
});

// ─── Toggle button active state ───────────────────────────────────────────────

describe('ViewToggleManager toggle buttons', () => {
    beforeEach(() => {
        setupDOM();
        localStorage.clear();
    });

    test('table button is active initially', () => {
        const m = makeManager();
        const tableBtn = document.querySelector('[data-view="table"]');
        expect(tableBtn?.classList.contains('active')).toBe(true);
    });

    test('cards button becomes active after switch', () => {
        const m = makeManager();
        m.switchView('cards');

        const cardsBtn = document.querySelector('[data-view="cards"]');
        const tableBtn = document.querySelector('[data-view="table"]');
        expect(cardsBtn?.classList.contains('active')).toBe(true);
        expect(tableBtn?.classList.contains('active')).toBe(false);
    });
});