

import { jest } from '@jest/globals';

// ─── Mock dependencies ────────────────────────────────────────────────────────

jest.unstable_mockModule('@/tableScripts/core/utils.js', () => ({
    htmlUtils:     { escape: (s) => s, decode: (s) => s },
    entityUtils:   {
        getAvailableItems: jest.fn(() => [
            { id: 1, name: 'Action' },
            { id: 2, name: 'RPG' }
        ]),
        getItemTypeName: (t) => t === 'games' ? 'Tags' : 'Genres'
    },
    securityUtils: { getCsrfToken: jest.fn(() => 'mock-csrf') },
    ICONS:         { SAVE: '💾', EDIT: '✏️', DELETE: '🗑️', CANCEL: '❌', COMPLETED: '✅', NOT_COMPLETED: '❌' }
}));

jest.unstable_mockModule('@/tableScripts/core/errorHandler.js', () => ({
    ErrorHandler: {
        parseErrorResponse: jest.fn(async () => 'Error'),
        handle:             jest.fn()
    }
}));

const { CardInlineEditManager } = await import('@/tableScripts/features/cards/cardInlineEdit.js');
const { entityUtils }           = await import('@/tableScripts/core/utils.js');

// ─── Helpers ──────────────────────────────────────────────────────────────────

function makeConfig(overrides = {}) {
    return {
        entityType:        'games',
        entityNameSingular:'Game',
        hideActions:        false,
        applyScoreStyling:  jest.fn(),
        ...overrides
    };
}

function makeCard(options = {}) {
    const card = document.createElement('div');
    card.className = 'media-card';
    card.dataset.originalName     = options.name     ?? 'Test Game';
    card.dataset.originalScore    = options.score    ?? '75';
    card.dataset.originalTagIds   = options.tagIds   ?? '1,2';
    card.dataset.originalCoverUrl = options.coverUrl ?? '';
    card.dataset.completed        = options.completed ?? 'false';

    const coverDiv = document.createElement('div');
    coverDiv.className = 'card-cover';

    const contentDiv = document.createElement('div');
    contentDiv.className = 'card-content';
    contentDiv.innerHTML = '<h3 class="card-title">Test Game</h3>';

    card.appendChild(coverDiv);
    card.appendChild(contentDiv);
    return card;
}

function makeManager() {
    const container = document.createElement('div');
    container.id = 'cards-container';
    document.body.appendChild(container);

    // Setup modal
    document.body.innerHTML += `
        <div id="tags-edit-modal" style="display:none">
            <div class="modal-body"></div>
            <button id="modal-save-tags">Save</button>
            <button id="modal-cancel-tags">Cancel</button>
        </div>
    `;

    return new CardInlineEditManager(container, makeConfig());
}

// ─── toggleCardEdit ───────────────────────────────────────────────────────────

describe('CardInlineEditManager.toggleCardEdit', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
    });

    test('switches to edit mode when card is not editing', () => {
        const m    = makeManager();
        const card = makeCard();
        const spy  = jest.spyOn(m, 'switchToEditMode');

        m.toggleCardEdit(card);

        expect(spy).toHaveBeenCalledWith(card);
        expect(m.currentEditCard).toBe(card);
    });

    test('calls saveCard when card is already editing', () => {
        const m    = makeManager();
        const card = makeCard();
        card.classList.add('is-editing');

        const spy = jest.spyOn(m, 'saveCard').mockResolvedValue();
        m.toggleCardEdit(card);

        expect(spy).toHaveBeenCalledWith(card);
    });

    test('switches previous card to view mode before editing new card', () => {
        const m     = makeManager();
        const card1 = makeCard({ name: 'Card 1' });
        const card2 = makeCard({ name: 'Card 2' });

        m.currentEditCard = card1;

        const viewSpy = jest.spyOn(m, 'switchToViewMode');
        const editSpy = jest.spyOn(m, 'switchToEditMode');

        m.toggleCardEdit(card2);

        expect(viewSpy).toHaveBeenCalledWith(card1);
        expect(editSpy).toHaveBeenCalledWith(card2);
    });
});

// ─── switchToEditMode ────────────────────────────────────────────────────────

describe('CardInlineEditManager.switchToEditMode', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
    });

    test('adds is-editing class to card', () => {
        const m    = makeManager();
        const card = makeCard();

        m.switchToEditMode(card);

        expect(card.classList.contains('is-editing')).toBe(true);
    });

    test('renders name input with original value', () => {
        const m    = makeManager();
        const card = makeCard({ name: 'Dark Souls' });

        m.switchToEditMode(card);

        const input = card.querySelector('.edit-name-input');
        expect(input).not.toBeNull();
        expect(input.value).toBe('Dark Souls');
    });

    test('renders score input with original value', () => {
        const m    = makeManager();
        const card = makeCard({ score: '88' });

        m.switchToEditMode(card);

        const input = card.querySelector('.edit-score-input');
        expect(input).not.toBeNull();
        expect(input.value).toBe('88');
    });

    test('renders cover URL input', () => {
        const m    = makeManager();
        const card = makeCard({ coverUrl: 'https://example.com/img.jpg' });

        m.switchToEditMode(card);

        const input = card.querySelector('.edit-cover-url-input');
        expect(input).not.toBeNull();
        expect(input.value).toBe('https://example.com/img.jpg');
    });

    test('renders save and cancel buttons', () => {
        const m    = makeManager();
        const card = makeCard();

        m.switchToEditMode(card);

        expect(card.querySelector('.save-edit-btn')).not.toBeNull();
        expect(card.querySelector('.cancel-edit-btn')).not.toBeNull();
    });

    test('labels tags field correctly for games', () => {
        const m    = makeManager();
        const card = makeCard();

        m.switchToEditMode(card);

        const form = card.querySelector('.card-edit-form');
        expect(form.innerHTML).toContain('Tags:');
    });

    test('labels tags field correctly for movies', () => {
        const container = document.createElement('div');
        document.body.appendChild(container);
        const m = new CardInlineEditManager(container, makeConfig({ entityType: 'movies' }));
        const card = makeCard();

        m.switchToEditMode(card);

        const form = card.querySelector('.card-edit-form');
        expect(form.innerHTML).toContain('Genres:');
    });
});

// ─── switchToViewMode ────────────────────────────────────────────────────────

describe('CardInlineEditManager.switchToViewMode', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
    });

    test('removes is-editing class', () => {
        const m    = makeManager();
        const card = makeCard();
        card.classList.add('is-editing');

        m.switchToViewMode(card);

        expect(card.classList.contains('is-editing')).toBe(false);
    });

    test('renders card title with original name', () => {
        const m    = makeManager();
        const card = makeCard({ name: 'Elden Ring' });
        card.classList.add('is-editing');

        m.switchToViewMode(card);

        const title = card.querySelector('.card-title');
        expect(title?.textContent).toBe('Elden Ring');
    });

    test('renders score cell', () => {
        const m    = makeManager();
        const card = makeCard({ score: '92' });

        m.switchToViewMode(card);

        const score = card.querySelector('.score-cell');
        expect(score?.textContent).toBe('92');
    });

    test('clears currentEditCard when it matches', () => {
        const m    = makeManager();
        const card = makeCard();
        m.currentEditCard = card;

        m.switchToViewMode(card);

        expect(m.currentEditCard).toBeNull();
    });

    test('calls applyScoreStyling when defined', () => {
        const m    = makeManager();
        const card = makeCard();

        m.switchToViewMode(card);

        expect(m.config.applyScoreStyling).toHaveBeenCalled();
    });

    test('renders cover image when coverUrl is set', () => {
        const m    = makeManager();
        const card = makeCard({ coverUrl: 'https://cover.jpg' });

        m.switchToViewMode(card);

        const img = card.querySelector('.card-cover-image');
        expect(img).not.toBeNull();
        expect(img.src).toContain('cover.jpg');
    });

    test('renders placeholder when no coverUrl', () => {
        const m    = makeManager();
        const card = makeCard({ coverUrl: '' });

        m.switchToViewMode(card);

        const placeholder = card.querySelector('.card-cover-placeholder');
        expect(placeholder).not.toBeNull();
    });
});

// ─── renderTagsEditDisplay ────────────────────────────────────────────────────

describe('CardInlineEditManager.renderTagsEditDisplay', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
        entityUtils.getAvailableItems.mockReturnValue([
            { id: 1, name: 'Action' },
            { id: 2, name: 'RPG' },
            { id: 3, name: 'Strategy' }
        ]);
    });

    test('returns "No tags selected" when no tagIds', () => {
        const m    = makeManager();
        const card = makeCard({ tagIds: '' });

        const html = m.renderTagsEditDisplay(card);
        expect(html).toContain('No tags selected');
    });

    test('renders tag badges for matching ids', () => {
        const m    = makeManager();
        const card = makeCard({ tagIds: '1,2' });

        const html = m.renderTagsEditDisplay(card);
        expect(html).toContain('Action');
        expect(html).toContain('RPG');
        expect(html).not.toContain('Strategy');
    });

    test('renders empty when tagIds do not match available items', () => {
        const m    = makeManager();
        const card = makeCard({ tagIds: '99,100' });

        const html = m.renderTagsEditDisplay(card);
        // No matching items, so result should not contain tag-badge
        expect(html).not.toContain('tag-badge');
    });
});

// ─── saveTagsFromModal ────────────────────────────────────────────────────────

describe('CardInlineEditManager.saveTagsFromModal', () => {
    beforeEach(() => {
        document.body.innerHTML = `
            <div id="tags-edit-modal">
                <div class="modal-body">
                    <input type="checkbox" value="1" checked>
                    <input type="checkbox" value="2">
                    <input type="checkbox" value="3" checked>
                </div>
            </div>
        `;
    });

    test('updates currentModalCard dataset with checked ids', () => {
        const container = document.createElement('div');
        const m  = new CardInlineEditManager(container, makeConfig());
        const card = makeCard({ tagIds: '' });
        m.currentModalCard = card;

        jest.spyOn(m, 'renderTagsEditDisplay').mockReturnValue('<span>tags</span>');
        m.saveTagsFromModal();

        expect(card.dataset.originalTagIds).toBe('1,3');
    });

    test('closes modal after saving', () => {
        const container = document.createElement('div');
        const m   = new CardInlineEditManager(container, makeConfig());
        const card = makeCard();
        m.currentModalCard = card;

        jest.spyOn(m, 'renderTagsEditDisplay').mockReturnValue('');
        m.saveTagsFromModal();

        expect(document.getElementById('tags-edit-modal').style.display).toBe('none');
    });

    test('does nothing when currentModalCard is null', () => {
        const container = document.createElement('div');
        const m   = new CardInlineEditManager(container, makeConfig());
        m.currentModalCard = null;

        expect(() => m.saveTagsFromModal()).not.toThrow();
    });
});

// ─── closeTagsModal ──────────────────────────────────────────────────────────

describe('CardInlineEditManager.closeTagsModal', () => {
    test('hides the modal', () => {
        document.body.innerHTML = `
            <div id="tags-edit-modal" style="display:block"></div>
        `;
        const container = document.createElement('div');
        const m = new CardInlineEditManager(container, makeConfig());

        m.closeTagsModal();

        expect(document.getElementById('tags-edit-modal').style.display).toBe('none');
    });

    test('clears currentModalCard', () => {
        document.body.innerHTML = `<div id="tags-edit-modal"></div>`;
        const container = document.createElement('div');
        const m = new CardInlineEditManager(container, makeConfig());
        m.currentModalCard = makeCard();

        m.closeTagsModal();

        expect(m.currentModalCard).toBeNull();
    });

    test('does not throw when modal does not exist', () => {
        document.body.innerHTML = '';
        const container = document.createElement('div');
        const m = new CardInlineEditManager(container, makeConfig());

        expect(() => m.closeTagsModal()).not.toThrow();
    });
});