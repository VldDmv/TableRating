
import { jest } from '@jest/globals';

// ─── Mock dependencies ────────────────────────────────────────────────────────

jest.unstable_mockModule('@/tableScripts/core/utils.js', () => ({
    htmlUtils:     { escape: (s) => s, decode: (s) => s },
    entityUtils:   {
        getAvailableItems: () => [],
        getItemTypeName:   (t) => t === 'games' ? 'Tags' : 'Genres'
    },
    securityUtils: { getCsrfToken: () => 'mock-csrf' },
    ICONS:         { SAVE: '💾', EDIT: '✏️', COMPLETED: '✅', NOT_COMPLETED: '❌' },
    CONSTANTS:     {}
}));

jest.unstable_mockModule('@/tableScripts/core/errorHandler.js', () => ({
    ErrorHandler: {
        parseErrorResponse: jest.fn(),
        handle:             jest.fn()
    }
}));

const { InlineEditManager } = await import('@/tableScripts/features/table/tableInlineEdit.js');

// ─── Helpers ──────────────────────────────────────────────────────────────────

function makeConfig(overrides = {}) {
    return {
        entityType:        'games',
        entityNameSingular:'Game',
        columns: {
            cover:     { index: 0 },
            name:      { index: 1 },
            score:     { index: 2 },
            tags:      { index: 3 },
            completed: { index: 4 },
            actions:   { index: 5 }
        },
        selectors: {
            statusButtonClass:     '.status-button',
            deleteButtonClass:     '.delete-button',
            editIconButtonClass:   '.edit-button',
            deleteIconButtonClass: '.delete-button'
        },
        paramNames: {
            removeItem: 'removeGame'
        },
        validateScore: (v) => {
            const s = parseInt(v, 10);
            return !isNaN(s) && s >= 1 && s <= 100;
        },
        applyScoreStyling: jest.fn(),
        ...overrides
    };
}

function makeRow(dataset = {}) {
    const tr = document.createElement('tr');
    // 6 cells: cover, name, score, tags, completed, actions
    for (let i = 0; i < 6; i++) {
        tr.appendChild(document.createElement('td'));
    }
    // name cell text
    tr.children[1].textContent = dataset.name || 'Test Game';
    // score cell
    const scoreSpan = document.createElement('span');
    scoreSpan.className = 'score-cell';
    scoreSpan.textContent = dataset.score || '75';
    tr.children[2].appendChild(scoreSpan);

    Object.entries(dataset).forEach(([k, v]) => {
        tr.dataset[k] = v;
    });
    return tr;
}

// ─── detectChanges ────────────────────────────────────────────────────────────

describe('InlineEditManager.detectChanges', () => {
    let manager;

    beforeEach(() => {
        const tb = document.createElement('tbody');
        manager = new InlineEditManager(tb, makeConfig());
    });

    test('returns false when nothing changed', () => {
        const row = makeRow();
        row.dataset.originalName     = 'Witcher';
        row.dataset.originalScore    = '85';
        row.dataset.initialTagIds    = '1,2';
        row.dataset.originalCoverUrl = 'https://img.com/cover.jpg';

        expect(manager.detectChanges(row, 'Witcher', '85', ['1', '2'], 'https://img.com/cover.jpg')).toBe(false);
    });

    test('returns true when name changed', () => {
        const row = makeRow();
        row.dataset.originalName     = 'Witcher';
        row.dataset.originalScore    = '85';
        row.dataset.initialTagIds    = '1,2';
        row.dataset.originalCoverUrl = '';

        expect(manager.detectChanges(row, 'Witcher 2', '85', ['1', '2'], '')).toBe(true);
    });

    test('returns true when score changed', () => {
        const row = makeRow();
        row.dataset.originalName     = 'Witcher';
        row.dataset.originalScore    = '85';
        row.dataset.initialTagIds    = '';
        row.dataset.originalCoverUrl = '';

        expect(manager.detectChanges(row, 'Witcher', '90', [], '')).toBe(true);
    });

    test('returns true when tags changed', () => {
        const row = makeRow();
        row.dataset.originalName     = 'Witcher';
        row.dataset.originalScore    = '85';
        row.dataset.initialTagIds    = '1,2';
        row.dataset.originalCoverUrl = '';

        expect(manager.detectChanges(row, 'Witcher', '85', ['1'], '')).toBe(true);
    });

    test('returns true when coverUrl changed', () => {
        const row = makeRow();
        row.dataset.originalName     = 'Witcher';
        row.dataset.originalScore    = '85';
        row.dataset.initialTagIds    = '';
        row.dataset.originalCoverUrl = '';

        expect(manager.detectChanges(row, 'Witcher', '85', [], 'https://new.jpg')).toBe(true);
    });

    test('returns false when tags order matches (initialTagIds used, not originalTagIds)', () => {
        const row = makeRow();
        row.dataset.originalName     = 'Game';
        row.dataset.originalScore    = '50';
        row.dataset.initialTagIds    = '3,4';
        row.dataset.originalTagIds   = '3,4';
        row.dataset.originalCoverUrl = '';

        expect(manager.detectChanges(row, 'Game', '50', ['3', '4'], '')).toBe(false);
    });
});

// ─── updateRowData ────────────────────────────────────────────────────────────

describe('InlineEditManager.updateRowData', () => {
    let manager;

    beforeEach(() => {
        const tb = document.createElement('tbody');
        manager = new InlineEditManager(tb, makeConfig());
    });

    test('updates all dataset fields', () => {
        const row = makeRow();
        manager.updateRowData(row, 'New Name', '95', ['5', '6'], 'https://cover.jpg');

        expect(row.dataset.originalName).toBe('New Name');
        expect(row.dataset.originalScore).toBe('95');
        expect(row.dataset.initialTagIds).toBe('5,6');
        expect(row.dataset.originalTagIds).toBe('5,6');
        expect(row.dataset.originalCoverUrl).toBe('https://cover.jpg');
    });

    test('handles empty tagIds array', () => {
        const row = makeRow();
        manager.updateRowData(row, 'Game', '50', [], '');

        expect(row.dataset.initialTagIds).toBe('');
        expect(row.dataset.originalTagIds).toBe('');
    });
});

// ─── storeOriginalValues ──────────────────────────────────────────────────────

describe('InlineEditManager.storeOriginalValues', () => {
    let manager;

    beforeEach(() => {
        const tb = document.createElement('tbody');
        manager = new InlineEditManager(tb, makeConfig());
    });

    test('stores name and score from cells', () => {
        const row = makeRow({ name: 'Elden Ring' });
        // row doesn't have dataset.originalName yet
        const nameCell  = row.children[1];
        const scoreCell = row.children[2];

        manager.storeOriginalValues(row, nameCell, scoreCell);

        expect(row.dataset.originalName).toBe('Elden Ring');
        expect(row.dataset.originalScore).toBe('75');
    });

    test('does not overwrite existing originalName', () => {
        const row = makeRow();
        row.dataset.originalName = 'Already Set';
        const nameCell  = row.children[1];
        const scoreCell = row.children[2];

        manager.storeOriginalValues(row, nameCell, scoreCell);

        expect(row.dataset.originalName).toBe('Already Set');
    });

    test('collects tag IDs from .tag-badge elements', () => {
        const row = makeRow();
        const tagsCell = row.children[3];

        const b1 = document.createElement('span');
        b1.className = 'tag-badge';
        b1.dataset.tagId = '10';

        const b2 = document.createElement('span');
        b2.className = 'tag-badge';
        b2.dataset.tagId = '20';

        tagsCell.appendChild(b1);
        tagsCell.appendChild(b2);

        manager.storeOriginalValues(row, row.children[1], row.children[2]);

        expect(row.dataset.originalTagIds).toBe('10,20');
        expect(row.dataset.initialTagIds).toBe('10,20');
    });
});

// ─── createNameInput ──────────────────────────────────────────────────────────

describe('InlineEditManager.createNameInput', () => {
    let manager;

    beforeEach(() => {
        const tb = document.createElement('tbody');
        manager = new InlineEditManager(tb, makeConfig());
    });

    test('creates input with original name value', () => {
        const row = makeRow();
        row.dataset.originalName = 'Dark Souls';
        const nameCell = row.children[1];

        manager.createNameInput(row, nameCell);

        const input = nameCell.querySelector('input');
        expect(input).not.toBeNull();
        expect(input.type).toBe('text');
        expect(input.value).toBe('Dark Souls');
        expect(input.className).toBe('edit-name-input');
    });

    test('does not create duplicate input if one already exists', () => {
        const row = makeRow();
        row.dataset.originalName = 'Game';
        const nameCell = row.children[1];

        manager.createNameInput(row, nameCell);
        manager.createNameInput(row, nameCell);

        expect(nameCell.querySelectorAll('input').length).toBe(1);
    });
});

// ─── createScoreInput ─────────────────────────────────────────────────────────

describe('InlineEditManager.createScoreInput', () => {
    let manager;

    beforeEach(() => {
        const tb = document.createElement('tbody');
        manager = new InlineEditManager(tb, makeConfig());
    });

    test('creates number input with original score', () => {
        const row = makeRow();
        row.dataset.originalScore = '82';
        const scoreCell = row.children[2];

        manager.createScoreInput(row, scoreCell);

        const input = scoreCell.querySelector('input');
        expect(input).not.toBeNull();
        expect(input.type).toBe('number');
        expect(input.value).toBe('82');
        expect(input.min).toBe('1');
        expect(input.max).toBe('100');
        expect(input.className).toBe('edit-score-input');
    });

    test('does not create duplicate score input', () => {
        const row = makeRow();
        row.dataset.originalScore = '50';
        const scoreCell = row.children[2];

        manager.createScoreInput(row, scoreCell);
        manager.createScoreInput(row, scoreCell);

        expect(scoreCell.querySelectorAll('input').length).toBe(1);
    });
});

// ─── toggleRowEdit ────────────────────────────────────────────────────────────

describe('InlineEditManager.toggleRowEdit', () => {
    let manager;

    beforeEach(() => {
        document.body.innerHTML = `
            <div id="tags-edit-modal" style="display:none">
                <div class="modal-body"></div>
            </div>
        `;
        const tb = document.createElement('tbody');
        manager = new InlineEditManager(tb, makeConfig());
    });

    test('switches row to edit mode when not editing', () => {
        const row = makeRow();
        row.dataset.originalName  = 'Witcher';
        row.dataset.originalScore = '85';

        // We need to spy on switchToEditRow
        const spy = jest.spyOn(manager, 'switchToEditRow');
        manager.toggleRowEdit(row);

        expect(spy).toHaveBeenCalledWith(row);
        expect(manager.currentEditRow).toBe(row);
    });

    test('calls saveRow when row is already editing', () => {
        const row = makeRow();
        row.classList.add('is-editing');

        const spy = jest.spyOn(manager, 'saveRow');
        manager.toggleRowEdit(row);

        expect(spy).toHaveBeenCalledWith(row);
    });

    test('switches previous edit row to view mode before editing new row', () => {
        const row1 = makeRow();
        const row2 = makeRow();
        manager.currentEditRow = row1;

        const viewSpy = jest.spyOn(manager, 'switchToViewRow');
        const editSpy = jest.spyOn(manager, 'switchToEditRow');

        manager.toggleRowEdit(row2);

        expect(viewSpy).toHaveBeenCalledWith(row1);
        expect(editSpy).toHaveBeenCalledWith(row2);
    });
});

// ─── switchToViewRow ──────────────────────────────────────────────────────────

describe('InlineEditManager.switchToViewRow', () => {
    let manager;

    beforeEach(() => {
        const tb = document.createElement('tbody');
        manager = new InlineEditManager(tb, makeConfig());
    });

    test('removes is-editing class', () => {
        const row = makeRow();
        row.classList.add('is-editing');
        row.dataset.originalName     = 'Test';
        row.dataset.originalScore    = '70';
        row.dataset.originalTagIds   = '';
        row.dataset.originalCoverUrl = '';

        manager.switchToViewRow(row);

        expect(row.classList.contains('is-editing')).toBe(false);
    });

    test('restores name cell text content', () => {
        const row = makeRow();
        row.classList.add('is-editing');
        row.dataset.originalName     = 'Restored Name';
        row.dataset.originalScore    = '70';
        row.dataset.originalTagIds   = '';
        row.dataset.originalCoverUrl = '';

        manager.switchToViewRow(row);

        expect(row.children[1].textContent).toBe('Restored Name');
    });

    test('restores score cell with score-cell span', () => {
        const row = makeRow();
        row.classList.add('is-editing');
        row.dataset.originalName     = 'Test';
        row.dataset.originalScore    = '88';
        row.dataset.originalTagIds   = '';
        row.dataset.originalCoverUrl = '';

        manager.switchToViewRow(row);

        const span = row.children[2].querySelector('.score-cell');
        expect(span).not.toBeNull();
        expect(span.textContent).toBe('88');
    });

    test('clears currentEditRow when it matches', () => {
        const row = makeRow();
        row.dataset.originalName     = 'Test';
        row.dataset.originalScore    = '70';
        row.dataset.originalTagIds   = '';
        row.dataset.originalCoverUrl = '';
        manager.currentEditRow = row;

        manager.switchToViewRow(row);

        expect(manager.currentEditRow).toBeNull();
    });

    test('calls applyScoreStyling if defined', () => {
        const tb = document.createElement('tbody');
        const applyMock = jest.fn();
        const config = makeConfig({ applyScoreStyling: applyMock });
        const m = new InlineEditManager(tb, config);

        const row = makeRow();
        row.dataset.originalName     = 'Test';
        row.dataset.originalScore    = '70';
        row.dataset.originalTagIds   = '';
        row.dataset.originalCoverUrl = '';

        m.switchToViewRow(row);

        expect(applyMock).toHaveBeenCalledWith(tb);
    });
});

// ─── saveTagsFromModal ────────────────────────────────────────────────────────

describe('InlineEditManager.saveTagsFromModal', () => {
    let manager;

    beforeEach(() => {
        document.body.innerHTML = `
            <div id="tags-edit-modal" style="display:block">
                <div class="modal-body">
                    <input type="checkbox" value="1" checked>
                    <input type="checkbox" value="2">
                    <input type="checkbox" value="3" checked>
                </div>
            </div>
        `;
        const tb = document.createElement('tbody');
        manager = new InlineEditManager(tb, makeConfig());
    });

    test('updates row dataset with checked tag IDs', () => {
        const row = makeRow();
        row.dataset.originalTagIds = '';
        manager.currentEditRow = row;

        jest.spyOn(manager, 'updateTagsInRow').mockImplementation(() => {});
        manager.saveTagsFromModal();

        expect(row.dataset.originalTagIds).toBe('1,3');
    });

    test('closes the modal after saving', () => {
        const row = makeRow();
        row.dataset.originalTagIds = '';
        manager.currentEditRow = row;

        jest.spyOn(manager, 'updateTagsInRow').mockImplementation(() => {});
        manager.saveTagsFromModal();

        expect(document.getElementById('tags-edit-modal').style.display).toBe('none');
    });

    test('does nothing if currentEditRow is null', () => {
        manager.currentEditRow = null;
        expect(() => manager.saveTagsFromModal()).not.toThrow();
    });
});