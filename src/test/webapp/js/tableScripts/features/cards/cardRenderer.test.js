import { jest } from '@jest/globals';

jest.unstable_mockModule('@/tableScripts/core/utils.js', () => ({
    htmlUtils: { escape: (s) => s, decode: (s) => s },
    ICONS: {
        COMPLETED: '✅',
        NOT_COMPLETED: '❌',
        EDIT: '✏️',
        DELETE: '🗑️',
    },
}));

const { CardRenderer } = await import('@/tableScripts/features/cards/cardRenderer.js');

// ─── Helpers ──────────────────────────────────────────────────────────────────

function makeConfig(overrides = {}) {
    return {
        entityType: 'games',
        hideActions: false,
        applyScoreStyling: jest.fn(),
        ...overrides,
    };
}

function makeItem(overrides = {}) {
    return {
        name: 'Witcher 3',
        score: 95,
        completed: false,
        coverUrl: '',
        tags: [],
        ...overrides,
    };
}

function setup(configOverrides = {}) {
    document.body.innerHTML = '<div id="cards-container"></div>';
    return new CardRenderer(makeConfig(configOverrides));
}

afterEach(() => {
    document.body.innerHTML = '';
});

// ─── render ───────────────────────────────────────────────────────────────────

describe('CardRenderer.render()', () => {
    test('renders one card per item', () => {
        const r = setup();
        r.render([makeItem(), makeItem({ name: 'Dark Souls' })]);
        expect(document.querySelectorAll('.media-card').length).toBe(2);
    });

    test('renders empty state when items array is empty', () => {
        const r = setup();
        r.render([]);
        expect(document.querySelector('.cards-empty-state')).not.toBeNull();
    });

    test('renders empty state when items is null', () => {
        const r = setup();
        r.render(null);
        expect(document.querySelector('.cards-empty-state')).not.toBeNull();
    });

    test('calls applyScoreStyling after rendering', () => {
        const r = setup();
        r.render([makeItem()]);
        expect(r.config.applyScoreStyling).toHaveBeenCalled();
    });

    test('calls onRender callback when registered', () => {
        const r = setup();
        const cb = jest.fn();
        r.onRender(cb);
        r.render([makeItem()]);
        expect(cb).toHaveBeenCalled();
    });

    test('calls onRender callback even on empty state', () => {
        const r = setup();
        const cb = jest.fn();
        r.onRender(cb);
        r.render([]);
        expect(cb).toHaveBeenCalled();
    });
});

// ─── renderLoading ────────────────────────────────────────────────────────────

describe('CardRenderer.renderLoading()', () => {
    test('renders loading state into the container', () => {
        const r = setup();
        r.render([]); // ensures container reference is set
        r.renderLoading();
        expect(document.getElementById('cards-container').innerHTML).toContain('Loading');
    });
});

// ─── createCard ───────────────────────────────────────────────────────────────

describe('CardRenderer.createCard()', () => {
    let renderer;
    beforeEach(() => {
        renderer = setup();
    });

    test('returns a DIV with class media-card', () => {
        const card = renderer.createCard(makeItem());
        expect(card.tagName).toBe('DIV');
        expect(card.classList.contains('media-card')).toBe(true);
    });

    test('stores originalName in dataset', () => {
        expect(renderer.createCard(makeItem({ name: 'Bloodborne' })).dataset.originalName).toBe(
            'Bloodborne'
        );
    });

    test('stores originalScore in dataset', () => {
        expect(renderer.createCard(makeItem({ score: 99 })).dataset.originalScore).toBe('99');
    });

    test('stores completed status as string in dataset', () => {
        expect(renderer.createCard(makeItem({ completed: true })).dataset.completed).toBe('true');
    });

    test('renders .card-title with item name', () => {
        const card = renderer.createCard(makeItem({ name: 'Sekiro' }));
        expect(card.querySelector('.card-title').textContent).toBe('Sekiro');
    });

    test('renders .score-cell with correct score', () => {
        const card = renderer.createCard(makeItem({ score: 91 }));
        expect(card.querySelector('.score-cell').textContent).toBe('91');
    });

    test('renders cover image when coverUrl is set', () => {
        const img = renderer
            .createCard(makeItem({ coverUrl: 'https://cover.jpg' }))
            .querySelector('.card-cover-image');
        expect(img).not.toBeNull();
        expect(img.src).toContain('cover.jpg');
    });

    test('renders cover placeholder when no coverUrl', () => {
        expect(
            renderer.createCard(makeItem({ coverUrl: '' })).querySelector('.card-cover-placeholder')
        ).not.toBeNull();
    });

    test('renders tag names from item.tags', () => {
        const card = renderer.createCard(
            makeItem({
                tags: [
                    { id: 1, name: 'Action' },
                    { id: 2, name: 'RPG' },
                ],
            })
        );
        expect(card.innerHTML).toContain('Action');
        expect(card.innerHTML).toContain('RPG');
    });

    test('renders genre names from item.genres when tags absent', () => {
        const card = renderer.createCard(
            makeItem({ tags: undefined, genres: [{ id: 5, name: 'Drama' }] })
        );
        expect(card.innerHTML).toContain('Drama');
    });

    test('stores originalTagIds as comma-separated IDs', () => {
        const card = renderer.createCard(
            makeItem({
                tags: [
                    { id: 10, name: 'X' },
                    { id: 20, name: 'Y' },
                ],
            })
        );
        expect(card.dataset.originalTagIds).toBe('10,20');
    });

    test('stores originalCoverUrl in dataset', () => {
        const card = renderer.createCard(makeItem({ coverUrl: 'https://img.jpg' }));
        expect(card.dataset.originalCoverUrl).toBe('https://img.jpg');
    });

    test('shows action buttons when hideActions is false', () => {
        expect(renderer.createCard(makeItem()).querySelector('.status-button')).not.toBeNull();
    });

    test('hides action buttons when hideActions is true', () => {
        const r = new CardRenderer(makeConfig({ hideActions: true }));
        const card = r.createCard(makeItem());
        expect(card.querySelector('.card-actions')).toBeNull();
    });

    test('shows .card-no-tags span when tags array is empty', () => {
        expect(
            renderer.createCard(makeItem({ tags: [] })).querySelector('.card-no-tags')
        ).not.toBeNull();
    });

    test('renders a .tag-badge for each tag', () => {
        const card = renderer.createCard(
            makeItem({
                tags: [
                    { id: 1, name: 'A' },
                    { id: 2, name: 'B' },
                    { id: 3, name: 'C' },
                ],
            })
        );
        expect(card.querySelectorAll('.tag-badge').length).toBe(3);
    });
});
