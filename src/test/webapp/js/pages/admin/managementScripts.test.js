import { jest } from '@jest/globals';

// Suppress the constructor's auto-fetch.

const { ManagementPage } = await import('@/pages/admin/managementScripts.js');

// ─── Helpers ──────────────────────────────────────────────────────────────────

function setupDOM(items = []) {
    const cardItems = items
        .map(
            ({ name, mediaTypes = [] }) => `
        <div class="management-item">
            <span class="item-name">${name}</span>
            <div class="item-media-types">
                ${mediaTypes.map((t) => `<span class="media-badge">${t}</span>`).join('')}
            </div>
        </div>
    `
        )
        .join('');

    const tableRows = items
        .map(
            ({ name, mediaTypes = [] }) => `
        <tr>
            <td>-</td>
            <td>${name}</td>
            <td><div class="table-media-types">
                ${mediaTypes.map((t) => `<span class="media-badge">${t}</span>`).join('')}
            </div></td>
        </tr>
    `
        )
        .join('');

    document.body.innerHTML = `
        <button id="cardsViewBtn" class="active"></button>
        <button id="tableViewBtn"></button>
        <div id="itemsCards" style="display:grid">${cardItems}</div>
        <div id="itemsTable" style="display:none">
            <table><tbody>${tableRows}</tbody></table>
        </div>
        <div id="emptyState" style="display:none"></div>
        <input id="managementSearch" type="text">
        <button class="filter-badge active" data-filter="all">All</button>
        <button class="filter-badge" data-filter="games">Games</button>
        <button class="filter-badge" data-filter="movies">Movies</button>
        <div id="editModal" style="display:none">
            <input id="edit-id">
            <input id="edit-name">
            <div id="edit-media-types">
                <input type="checkbox" value="games">
                <input type="checkbox" value="movies">
                <input type="checkbox" value="books">
            </div>
        </div>
        <div id="deleteModal" style="display:none">
            <input id="delete-id">
            <span id="delete-name"></span>
        </div>
    `;
}

function makeManager(items = []) {
    setupDOM(items);
    global.fetch = jest.fn().mockResolvedValue({
        ok: true,
        json: async () => ({ items: [], totalPages: 1 }),
    });
    return new ManagementPage();
}

afterEach(() => {
    document.body.innerHTML = '';
    localStorage.clear();
    jest.clearAllMocks();
});

// ─── _applyView ───────────────────────────────────────────────────────────────

describe('ManagementPage._applyView', () => {
    let manager;

    beforeEach(() => {
        manager = makeManager();
    });

    test('cards: itemsCards=grid, itemsTable=none', () => {
        manager._applyView('cards');
        expect(document.getElementById('itemsCards').style.display).toBe('grid');
        expect(document.getElementById('itemsTable').style.display).toBe('none');
    });

    test('table: itemsTable=block, itemsCards=none', () => {
        manager._applyView('table');
        expect(document.getElementById('itemsCards').style.display).toBe('none');
        expect(document.getElementById('itemsTable').style.display).toBe('block');
    });

    test('cards: cardsViewBtn active, tableViewBtn not', () => {
        manager._applyView('cards');
        expect(document.getElementById('cardsViewBtn').classList.contains('active')).toBe(true);
        expect(document.getElementById('tableViewBtn').classList.contains('active')).toBe(false);
    });

    test('table: tableViewBtn active, cardsViewBtn not', () => {
        manager._applyView('table');
        expect(document.getElementById('tableViewBtn').classList.contains('active')).toBe(true);
        expect(document.getElementById('cardsViewBtn').classList.contains('active')).toBe(false);
    });

    test('persists view to localStorage', () => {
        manager._applyView('table');
        expect(localStorage.getItem('managementView')).toBe('table');
        manager._applyView('cards');
        expect(localStorage.getItem('managementView')).toBe('cards');
    });
});

// ─── setFilter ────────────────────────────────────────────────────────────────

describe('ManagementPage.setFilter', () => {
    let manager;

    beforeEach(() => {
        manager = makeManager([{ name: 'Test', mediaTypes: ['games'] }]);
    });

    test('activates the clicked filter badge', () => {
        manager.setFilter('games');
        expect(document.querySelector('[data-filter="games"]').classList.contains('active')).toBe(
            true
        );
    });

    test('deactivates all other badges', () => {
        manager.setFilter('games');
        expect(document.querySelector('[data-filter="all"]').classList.contains('active')).toBe(
            false
        );
        expect(document.querySelector('[data-filter="movies"]').classList.contains('active')).toBe(
            false
        );
    });

    test('switching back to "all" activates only the all badge', () => {
        manager.setFilter('games');
        manager.setFilter('all');
        expect(document.querySelector('[data-filter="all"]').classList.contains('active')).toBe(
            true
        );
        expect(document.querySelector('[data-filter="games"]').classList.contains('active')).toBe(
            false
        );
    });
});

// ─── _itemMatchesFilters ──────────────────────────────────────────────────────

describe('ManagementPage._itemMatchesFilters', () => {
    let manager;

    beforeEach(() => {
        manager = makeManager();
    });

    function makeBadges(types) {
        const div = document.createElement('div');
        types.forEach((t) => {
            const s = document.createElement('span');
            s.className = 'media-badge';
            s.textContent = t;
            div.appendChild(s);
        });
        return div;
    }

    test('no search + filter "all" → always matches', () => {
        expect(manager._itemMatchesFilters('anything', null, '', 'all')).toBe(true);
    });

    test('name does not include searchTerm → no match', () => {
        expect(manager._itemMatchesFilters('action', null, 'zzz', 'all')).toBe(false);
    });

    test('name includes searchTerm → match', () => {
        expect(manager._itemMatchesFilters('action rpg', null, 'rpg', 'all')).toBe(true);
    });

    test('filter "all" ignores badge content', () => {
        expect(manager._itemMatchesFilters('test', makeBadges(['movies']), '', 'all')).toBe(true);
    });

    test('specific filter matches badge content', () => {
        expect(
            manager._itemMatchesFilters('test', makeBadges(['games', 'movies']), '', 'games')
        ).toBe(true);
    });

    test('specific filter → no match when badge missing', () => {
        expect(manager._itemMatchesFilters('test', makeBadges(['movies']), '', 'games')).toBe(
            false
        );
    });

    test('filter match is case-insensitive', () => {
        expect(manager._itemMatchesFilters('test', makeBadges(['Games']), '', 'games')).toBe(true);
    });

    test('both search AND filter must match', () => {
        const el = makeBadges(['games']);
        expect(manager._itemMatchesFilters('my item', el, 'other', 'games')).toBe(false);
        expect(manager._itemMatchesFilters('my item', el, 'my', 'games')).toBe(true);
    });

    // Design decision: items without a badges element are treated as universally
    // matching any filter (so they're never hidden due to missing metadata).
    test('null badgesEl + specific filter → falls back to search-only match', () => {
        expect(manager._itemMatchesFilters('action', null, 'act', 'games')).toBe(true);
    });
});

// ─── applyFilters ─────────────────────────────────────────────────────────────

describe('ManagementPage.applyFilters', () => {
    let manager;

    beforeEach(() => {
        manager = makeManager([
            { name: 'Action', mediaTypes: ['games'] },
            { name: 'Drama', mediaTypes: ['movies'] },
            { name: 'Fantasy', mediaTypes: ['games', 'movies'] },
        ]);
    });

    test('empty search + "all" → all items visible', () => {
        manager.applyFilters('', 'all', 'cards');
        document
            .querySelectorAll('.management-item')
            .forEach((i) => expect(i.style.display).not.toBe('none'));
    });

    test('search filters by name (case-insensitive)', () => {
        manager.applyFilters('drama', 'all', 'cards');
        const items = document.querySelectorAll('.management-item');
        expect(items[0].style.display).toBe('none'); // Action
        expect(items[1].style.display).not.toBe('none'); // Drama
        expect(items[2].style.display).toBe('none'); // Fantasy
    });

    test('media type filter shows only matching items', () => {
        manager.applyFilters('', 'movies', 'cards');
        const items = document.querySelectorAll('.management-item');
        expect(items[0].style.display).toBe('none'); // Action (games only)
        expect(items[1].style.display).not.toBe('none'); // Drama
        expect(items[2].style.display).not.toBe('none'); // Fantasy (games+movies)
    });

    test('combined filter + search narrows results', () => {
        manager.applyFilters('fantasy', 'games', 'cards');
        const items = document.querySelectorAll('.management-item');
        expect(items[0].style.display).toBe('none'); // Action (name mismatch)
        expect(items[1].style.display).toBe('none'); // Drama (type mismatch)
        expect(items[2].style.display).not.toBe('none'); // Fantasy ✓
    });

    test('no matches → shows emptyState', () => {
        manager.applyFilters('zzznomatch', 'all', 'cards');
        expect(document.getElementById('emptyState').style.display).toBe('block');
    });

    test('no matches → hides both cards and table', () => {
        manager.applyFilters('zzznomatch', 'all', 'cards');
        expect(document.getElementById('itemsCards').style.display).toBe('none');
        expect(document.getElementById('itemsTable').style.display).toBe('none');
    });

    test('results found → emptyState hidden', () => {
        manager.applyFilters('action', 'all', 'cards');
        expect(document.getElementById('emptyState').style.display).toBe('none');
    });

    test('table rows are also filtered', () => {
        manager.applyFilters('drama', 'all', 'table');
        const rows = document.querySelectorAll('#itemsTable tbody tr');
        expect(rows[0].style.display).toBe('none'); // Action
        expect(rows[1].style.display).not.toBe('none'); // Drama
    });
});

// ─── modal helpers ────────────────────────────────────────────────────────────

describe('ManagementPage modal helpers', () => {
    let manager;

    beforeEach(() => {
        manager = makeManager();
    });

    test('closeEditModal removes show class and hides modal', () => {
        const modal = document.getElementById('editModal');
        modal.classList.add('show');
        modal.style.display = 'flex';
        manager.closeEditModal();
        expect(modal.classList.contains('show')).toBe(false);
        expect(modal.style.display).toBe('none');
    });

    test('closeDeleteModal removes show class and hides modal', () => {
        const modal = document.getElementById('deleteModal');
        modal.classList.add('show');
        modal.style.display = 'flex';
        manager.closeDeleteModal();
        expect(modal.classList.contains('show')).toBe(false);
        expect(modal.style.display).toBe('none');
    });
});

// ─── editItem ─────────────────────────────────────────────────────────────────

describe('ManagementPage.editItem', () => {
    let manager;

    beforeEach(() => {
        manager = makeManager();
    });

    function makeBtn(id, name, mediaTypes) {
        const btn = document.createElement('button');
        btn.dataset.id = id;
        btn.dataset.name = name;
        if (mediaTypes) btn.dataset.mediaTypes = mediaTypes;
        return btn;
    }

    test('populates id and name fields', () => {
        manager.editItem(makeBtn('7', 'Horror', 'movies'));
        expect(document.getElementById('edit-id').value).toBe('7');
        expect(document.getElementById('edit-name').value).toBe('Horror');
    });

    test('checks correct media type checkboxes', () => {
        manager.editItem(makeBtn('1', 'Test', 'games,movies'));
        const cbs = document.querySelectorAll('#edit-media-types input');
        expect(cbs[0].checked).toBe(true); // games
        expect(cbs[1].checked).toBe(true); // movies
        expect(cbs[2].checked).toBe(false); // books
    });

    test('unchecks non-matching checkboxes that were previously checked', () => {
        document.querySelectorAll('#edit-media-types input').forEach((cb) => (cb.checked = true));
        manager.editItem(makeBtn('1', 'Test', 'games'));
        const cbs = document.querySelectorAll('#edit-media-types input');
        expect(cbs[0].checked).toBe(true); // games
        expect(cbs[1].checked).toBe(false); // movies
        expect(cbs[2].checked).toBe(false); // books
    });

    test('opens editModal', () => {
        manager.editItem(makeBtn('1', 'Test'));
        expect(document.getElementById('editModal').style.display).toBe('flex');
    });

    test('works when mediaTypes dataset is absent', () => {
        expect(() => manager.editItem(makeBtn('1', 'Test'))).not.toThrow();
    });
});

// ─── deleteItem ───────────────────────────────────────────────────────────────

describe('ManagementPage.deleteItem', () => {
    let manager;

    beforeEach(() => {
        manager = makeManager();
    });

    function makeBtn(id, name) {
        const btn = document.createElement('button');
        btn.dataset.id = id;
        btn.dataset.name = name;
        return btn;
    }

    test('populates delete-id and delete-name', () => {
        manager.deleteItem(makeBtn('3', 'Sci-Fi'));
        expect(document.getElementById('delete-id').value).toBe('3');
        expect(document.getElementById('delete-name').textContent).toBe('Sci-Fi');
    });

    test('opens deleteModal', () => {
        manager.deleteItem(makeBtn('3', 'Sci-Fi'));
        expect(document.getElementById('deleteModal').style.display).toBe('flex');
    });
});
