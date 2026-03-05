

import { jest } from '@jest/globals';

const { TagChipsManager } = await import('@/tableScripts/features/forms/tagchipsManager.js');

// ─── DOM setup ────────────────────────────────────────────────────────────────

function setupDOM(items = []) {
    const dropdownItems = items.map(({ id, name }) => `
        <div class="tag-dropdown-item" data-tag-id="${id}" data-tag-name="${name}">
            <input type="checkbox" value="${id}">
            ${name}
        </div>
    `).join('');

    document.body.innerHTML = `
        <div id="chipsDisplay"></div>
        <div id="tagDropdown" style="display:none;">
            <input id="tagSearchInput" type="text">
            ${dropdownItems}
        </div>
        <button id="addTagBtn">+</button>
    `;
}

function makeManager(items = []) {
    setupDOM(items);
    return new TagChipsManager('chipsDisplay', 'tagDropdown', 'addTagBtn', 'tagSearchInput');
}

// ─── addChip ─────────────────────────────────────────────────────────────────

describe('TagChipsManager.addChip', () => {
    test('adds a chip to the display', () => {
        const m = makeManager();
        m.addChip('1', 'Action');

        const chip = document.querySelector('[data-chip-id="1"]');
        expect(chip).not.toBeNull();
    });

    test('chip contains tag name text', () => {
        const m = makeManager();
        m.addChip('2', 'RPG');

        const chip = document.querySelector('[data-chip-id="2"]');
        expect(chip.textContent).toContain('RPG');
    });

    test('does not add duplicate chip', () => {
        const m = makeManager();
        m.addChip('1', 'Action');
        m.addChip('1', 'Action'); // duplicate

        const chips = document.querySelectorAll('[data-chip-id="1"]');
        expect(chips.length).toBe(1);
    });

    test('adds multiple different chips', () => {
        const m = makeManager();
        m.addChip('1', 'Action');
        m.addChip('2', 'RPG');
        m.addChip('3', 'Strategy');

        expect(document.querySelectorAll('.tag-chip').length).toBe(3);
    });

    test('chip has a remove button', () => {
        const m = makeManager();
        m.addChip('1', 'Action');

        const removeBtn = document.querySelector('[data-chip-id="1"] .tag-chip-remove');
        expect(removeBtn).not.toBeNull();
    });
});

// ─── removeChip ──────────────────────────────────────────────────────────────

describe('TagChipsManager.removeChip', () => {
    beforeEach(() => {
        jest.useFakeTimers();
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    test('removes chip after animation delay', () => {
        const m = makeManager();
        m.addChip('1', 'Action');

        m.removeChip('1');
        jest.advanceTimersByTime(300);

        expect(document.querySelector('[data-chip-id="1"]')).toBeNull();
    });

    test('does not throw when chip does not exist', () => {
        const m = makeManager();
        expect(() => m.removeChip('999')).not.toThrow();
    });
});

// ─── getSelectedIds ───────────────────────────────────────────────────────────

describe('TagChipsManager.getSelectedIds', () => {
    test('returns empty array when no chips', () => {
        const m = makeManager();
        expect(m.getSelectedIds()).toEqual([]);
    });

    test('returns ids of all chips', () => {
        const m = makeManager();
        m.addChip('1', 'Action');
        m.addChip('2', 'RPG');

        const ids = m.getSelectedIds();
        expect(ids).toContain('1');
        expect(ids).toContain('2');
        expect(ids.length).toBe(2);
    });

    test('returns empty after clearAll', () => {
        const m = makeManager([{ id: '1', name: 'Action' }]);
        m.addChip('1', 'Action');
        m.clearAll();

        expect(m.getSelectedIds()).toEqual([]);
    });
});

// ─── clearAll ────────────────────────────────────────────────────────────────

describe('TagChipsManager.clearAll', () => {
    test('removes all chips', () => {
        const m = makeManager();
        m.addChip('1', 'Action');
        m.addChip('2', 'RPG');

        m.clearAll();

        expect(document.querySelectorAll('.tag-chip').length).toBe(0);
    });

    test('unchecks all checkboxes in dropdown', () => {
        const m = makeManager([
            { id: '1', name: 'Action' },
            { id: '2', name: 'RPG' }
        ]);
        m.addChip('1', 'Action');

        // Manually check the checkbox
        const cb = document.querySelector('[data-tag-id="1"] input[type="checkbox"]');
        cb.checked = true;

        m.clearAll();

        expect(cb.checked).toBe(false);
    });

    test('removes selected class from dropdown items', () => {
        const m = makeManager([{ id: '1', name: 'Action' }]);
        const item = document.querySelector('[data-tag-id="1"]');
        item.classList.add('selected');

        m.clearAll();

        expect(item.classList.contains('selected')).toBe(false);
    });
});

// ─── filterDropdown ──────────────────────────────────────────────────────────

describe('TagChipsManager.filterDropdown', () => {
    const items = [
        { id: '1', name: 'Action' },
        { id: '2', name: 'RPG' },
        { id: '3', name: 'Action RPG' }
    ];

    test('shows all items when query is empty', () => {
        const m = makeManager(items);
        m.filterDropdown('');

        const visible = Array.from(document.querySelectorAll('.tag-dropdown-item'))
            .filter(el => el.style.display !== 'none');
        expect(visible.length).toBe(3);
    });

    test('filters items by search query', () => {
        const m = makeManager(items);
        m.filterDropdown('rpg');

        const visible = Array.from(document.querySelectorAll('.tag-dropdown-item'))
            .filter(el => el.style.display !== 'none');
        expect(visible.length).toBe(2); // RPG and Action RPG
    });

    test('hides non-matching items', () => {
        const m = makeManager(items);
        m.filterDropdown('action');

        const actionItem    = document.querySelector('[data-tag-id="1"]');
        const rpgItem       = document.querySelector('[data-tag-id="2"]');
        const actionRpgItem = document.querySelector('[data-tag-id="3"]');

        expect(actionItem.style.display).not.toBe('none');
        expect(rpgItem.style.display).toBe('none');
        expect(actionRpgItem.style.display).not.toBe('none');
    });

    test('is case-insensitive', () => {
        const m = makeManager(items);
        m.filterDropdown('ACTION');

        const visible = Array.from(document.querySelectorAll('.tag-dropdown-item'))
            .filter(el => el.style.display !== 'none');
        expect(visible.length).toBe(2);
    });
});

// ─── toggleDropdown ──────────────────────────────────────────────────────────

describe('TagChipsManager.toggleDropdown', () => {
    test('shows dropdown when hidden', () => {
        const m = makeManager();
        const dropdown = document.getElementById('tagDropdown');
        dropdown.style.display = 'none';

        m.toggleDropdown();

        expect(dropdown.style.display).toBe('block');
    });

    test('hides dropdown when visible', () => {
        const m = makeManager();
        const dropdown = document.getElementById('tagDropdown');
        dropdown.style.display = 'block';

        m.toggleDropdown();

        expect(dropdown.style.display).toBe('none');
    });
});

// ─── setSelectedTags ─────────────────────────────────────────────────────────

describe('TagChipsManager.setSelectedTags', () => {
    const items = [
        { id: '1', name: 'Action' },
        { id: '2', name: 'RPG' },
        { id: '3', name: 'Strategy' }
    ];

    test('sets chips for given tags', () => {
        const m = makeManager(items);
        m.setSelectedTags([
            { id: '1', name: 'Action' },
            { id: '3', name: 'Strategy' }
        ]);

        const ids = m.getSelectedIds();
        expect(ids).toContain('1');
        expect(ids).toContain('3');
        expect(ids).not.toContain('2');
    });

    test('clears previous chips before setting new ones', () => {
        const m = makeManager(items);
        m.addChip('2', 'RPG');

        m.setSelectedTags([{ id: '1', name: 'Action' }]);

        expect(document.querySelector('[data-chip-id="2"]')).toBeNull();
        expect(document.querySelector('[data-chip-id="1"]')).not.toBeNull();
    });

    test('checks corresponding dropdown checkboxes', () => {
        const m = makeManager(items);
        m.setSelectedTags([{ id: '1', name: 'Action' }]);

        const cb = document.querySelector('[data-tag-id="1"] input[type="checkbox"]');
        expect(cb.checked).toBe(true);
    });
});

// ─── toggleTag ────────────────────────────────────────────────────────────────

describe('TagChipsManager.toggleTag', () => {
    test('selecting unchecked item adds chip', () => {
        const m = makeManager([{ id: '1', name: 'Action' }]);
        const item = document.querySelector('.tag-dropdown-item');
        const cb   = item.querySelector('input[type="checkbox"]');
        cb.checked = false;

        m.toggleTag(item);

        expect(document.querySelector('[data-chip-id="1"]')).not.toBeNull();
        expect(cb.checked).toBe(true);
    });

    test('deselecting checked item removes chip', () => {
        jest.useFakeTimers();
        const m = makeManager([{ id: '1', name: 'Action' }]);
        m.addChip('1', 'Action');

        const item = document.querySelector('.tag-dropdown-item');
        const cb   = item.querySelector('input[type="checkbox"]');
        cb.checked = true;
        item.classList.add('selected');

        m.toggleTag(item);
        jest.advanceTimersByTime(300);

        expect(cb.checked).toBe(false);
        expect(item.classList.contains('selected')).toBe(false);
        jest.useRealTimers();
    });
});