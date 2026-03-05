import { jest } from '@jest/globals';

const { UsersPageManager, getAvatarColor } = await import('@/pages/usersPage.js');

// ─── Helpers ──────────────────────────────────────────────────────────────────

function makeUser(overrides = {}) {
    return {
        name: 'alice', totalItems: 42,
        gamesCount: 10, moviesCount: 15, booksCount: 7, showsCount: 10,
        createdAt: '2023-06-15T00:00:00Z',
        ...overrides
    };
}

function setupDOM() {
    document.body.innerHTML = `
        <div id="usersGrid"  style="display:grid"></div>
        <div id="usersList"  style="display:none"></div>
        <input id="userSearch" type="text">
        <select id="sortBy"><option value="totalItems">Total</option></select>
        <button id="sortOrderBtn"><span class="sort-icon">↓</span></button>
        <button id="cardsViewBtn" class="active"></button>
        <button id="listViewBtn"></button>
        <button id="prevPage"></button>
        <button id="nextPage"></button>
        <button id="pageDropdown">Page 1 of 1</button>
        <ul id="pageList"></ul>
        <div id="emptyState" style="display:none"></div>
        <span id="totalUsers">0</span>
        <div id="paginationContainer" style="display:none"></div>
    `;
}

function makeManager() {
    global.fetch = jest.fn().mockResolvedValue({
        ok: true,
        json: async () => ({ items: [], totalPages: 1, totalItems: 0 })
    });
    return new UsersPageManager();
}

afterEach(() => {
    document.body.innerHTML = '';
    localStorage.clear();
    jest.clearAllMocks();
});

// ─── _formatDate (via manager instance) ──────────────────────────────────────

describe('formatDate', () => {
    let manager;
    beforeEach(() => { setupDOM(); manager = makeManager(); });

    test('returns "Unknown" for null', () => {
        expect(manager._formatDate(null)).toBe('Unknown');
    });

    test('returns "Unknown" for undefined', () => {
        expect(manager._formatDate(undefined)).toBe('Unknown');
    });

    test('formats a valid ISO date string (contains year and month)', () => {
        const result = manager._formatDate('2023-06-15T00:00:00Z');
        expect(result).toContain('2023');
        expect(result).toContain('Jun');
    });

    test('returns non-empty string for any valid date', () => {
        expect(manager._formatDate('2020-01-01T00:00:00Z')).not.toBe('Unknown');
    });
});

// ─── getAvatarColor ───────────────────────────────────────────────────────────

describe('getAvatarColor', () => {
    test('returns a non-empty color string', () => {
        expect(getAvatarColor('alice').length).toBeGreaterThan(0);
    });

    test('returns the same color for the same name', () => {
        expect(getAvatarColor('alice')).toBe(getAvatarColor('alice'));
    });

    test('handles empty string without throwing', () => {
        expect(() => getAvatarColor('')).not.toThrow();
    });
});

// ─── _createUserCard (via manager instance) ───────────────────────────────────

describe('createUserCard', () => {
    let manager;
    beforeEach(() => { setupDOM(); manager = makeManager(); });

    function parse(user) {
        const div = document.createElement('div');
        div.innerHTML = manager._createUserCard(user);
        return div.querySelector('.user-card');
    }

    test('root element has class user-card', () => {
        expect(parse(makeUser()).classList.contains('user-card')).toBe(true);
    });

    test('data-username attribute matches user name', () => {
        expect(parse(makeUser({ name: 'bob' })).dataset.username).toBe('bob');
    });

    test('avatar shows first letter of name in uppercase', () => {
        expect(parse(makeUser({ name: 'charlie' })).querySelector('.user-avatar')
            .textContent.trim()).toBe('C');
    });

    test('totalItems badge shows correct count', () => {
        expect(parse(makeUser({ totalItems: 99 }))
            .querySelector('.total-items-badge').textContent).toContain('99');
    });

    test('user name appears in .user-name', () => {
        expect(parse(makeUser({ name: 'diana' })).querySelector('.user-name').textContent)
            .toBe('diana');
    });

    test('renders 4 stat boxes', () => {
        expect(parse(makeUser()).querySelectorAll('.stat-box').length).toBe(4);
    });

    test('stat values are in order: Games, Movies, Books, Shows', () => {
        const values = Array.from(
            parse(makeUser({ gamesCount: 5, moviesCount: 8, booksCount: 3, showsCount: 12 }))
                .querySelectorAll('.stat-box-value')
        ).map(el => el.textContent);
        expect(values).toEqual(['5', '8', '3', '12']);
    });

    test('stat labels are Games, Movies, Books, Shows', () => {
        const labels = Array.from(parse(makeUser()).querySelectorAll('.stat-box-label'))
            .map(el => el.textContent);
        expect(labels).toEqual(['Games', 'Movies', 'Books', 'Shows']);
    });

    test('join date contains year', () => {
        expect(parse(makeUser({ createdAt: '2023-06-15T00:00:00Z' }))
            .querySelector('.user-joined').textContent).toContain('2023');
    });

    test('shows "Unknown" when createdAt is null', () => {
        expect(parse(makeUser({ createdAt: null }))
            .querySelector('.user-joined').textContent).toContain('Unknown');
    });

   test('avatar has a background color set', () => {
       const avatar = parse(makeUser({ name: 'alice' })).querySelector('.user-avatar');
       expect(avatar.getAttribute('style')).toContain('background');
   });

    test('escapes XSS in username', () => {
        expect(parse(makeUser({ name: '<script>xss</script>' })).innerHTML)
            .not.toContain('<script>');
    });
});

// ─── _createUserListItem (via manager instance) ───────────────────────────────

describe('createUserListItem', () => {
    let manager;
    beforeEach(() => { setupDOM(); manager = makeManager(); });

    function parse(user) {
        const div = document.createElement('div');
        div.innerHTML = manager._createUserListItem(user);
        return div.querySelector('.user-list-item');
    }

    test('root element has class user-list-item', () => {
        expect(parse(makeUser()).classList.contains('user-list-item')).toBe(true);
    });

    test('data-username attribute matches user name', () => {
        expect(parse(makeUser({ name: 'eve' })).dataset.username).toBe('eve');
    });

    test('avatar shows first letter of name in uppercase', () => {
        expect(parse(makeUser({ name: 'frank' })).querySelector('.user-avatar')
            .textContent.trim()).toBe('F');
    });

    test('.user-list-total shows correct count', () => {
        expect(parse(makeUser({ totalItems: 77 }))
            .querySelector('.user-list-total').textContent).toContain('77');
    });

    test('user name appears in .user-list-name', () => {
        expect(parse(makeUser({ name: 'grace' })).querySelector('.user-list-name').textContent)
            .toBe('grace');
    });

    test('renders 4 stat spans', () => {
        expect(parse(makeUser()).querySelectorAll('.user-list-stat').length).toBe(4);
    });

    test('.user-list-joined contains "Joined" prefix', () => {
        expect(parse(makeUser({ createdAt: '2022-01-01T00:00:00Z' }))
            .querySelector('.user-list-joined').textContent).toContain('Joined');
    });

    test('shows "Unknown" when createdAt is null', () => {
        expect(parse(makeUser({ createdAt: null }))
            .querySelector('.user-list-joined').textContent).toContain('Unknown');
    });

    test('escapes XSS in username', () => {
        expect(parse(makeUser({ name: '<img onerror=alert(1)>' })).innerHTML)
            .not.toContain('<img onerror');
    });
});

// ─── UsersPageManager._applyView ─────────────────────────────────────────────

describe('UsersPageManager._applyView', () => {
    let manager;

    beforeEach(() => { setupDOM(); manager = makeManager(); });

    test('cards: usersGrid=grid, usersList=none', () => {
        manager._applyView('cards');
        expect(document.getElementById('usersGrid').style.display).toBe('grid');
        expect(document.getElementById('usersList').style.display).toBe('none');
    });

    test('list: usersList=flex, usersGrid=none', () => {
        manager._applyView('list');
        expect(document.getElementById('usersList').style.display).toBe('flex');
        expect(document.getElementById('usersGrid').style.display).toBe('none');
    });

    test('cards: cardsViewBtn active, listViewBtn not', () => {
        manager._applyView('cards');
        expect(document.getElementById('cardsViewBtn').classList.contains('active')).toBe(true);
        expect(document.getElementById('listViewBtn').classList.contains('active')).toBe(false);
    });

    test('list: listViewBtn active, cardsViewBtn not', () => {
        manager._applyView('list');
        expect(document.getElementById('listViewBtn').classList.contains('active')).toBe(true);
        expect(document.getElementById('cardsViewBtn').classList.contains('active')).toBe(false);
    });

    test('saves preference to localStorage', () => {
        manager._applyView('list');
        expect(localStorage.getItem('usersView')).toBe('list');
        manager._applyView('cards');
        expect(localStorage.getItem('usersView')).toBe('cards');
    });
});

// ─── UsersPageManager.renderUsers ────────────────────────────────────────────

describe('UsersPageManager.renderUsers', () => {
    let manager;

    beforeEach(() => { setupDOM(); manager = makeManager(); });

    test('empty array: shows emptyState', () => {
        manager.renderUsers([]);
        expect(document.getElementById('emptyState').style.display).toBe('block');
    });

    test('null: shows emptyState', () => {
        manager.renderUsers(null);
        expect(document.getElementById('emptyState').style.display).toBe('block');
    });

    test('non-empty: hides emptyState, shows paginationContainer', () => {
        manager.renderUsers([makeUser()]);
        expect(document.getElementById('emptyState').style.display).toBe('none');
        expect(document.getElementById('paginationContainer').style.display).toBe('flex');
    });

    test('cards view: renders .user-card elements into usersGrid', () => {
        manager.currentView = 'cards';
        manager.renderUsers([makeUser(), makeUser({ name: 'bob' })]);
        expect(document.querySelectorAll('#usersGrid .user-card').length).toBe(2);
    });

    test('list view: renders .user-list-item elements into usersList', () => {
        manager.currentView = 'list';
        manager.renderUsers([makeUser(), makeUser({ name: 'bob' })]);
        expect(document.querySelectorAll('#usersList .user-list-item').length).toBe(2);
    });

    test('data-username on each card is correct', () => {
        manager.currentView = 'cards';
        manager.renderUsers([makeUser({ name: 'henry' })]);
        expect(document.querySelector('.user-card').dataset.username).toBe('henry');
    });
});

// ─── UsersPageManager.updatePagination ───────────────────────────────────────

describe('UsersPageManager.updatePagination', () => {
    let manager;

    beforeEach(() => { setupDOM(); manager = makeManager(); });

    test('prevPage disabled on page 1', () => {
        manager.currentPage = 1; manager.totalPages = 5;
        manager.updatePagination();
        expect(document.getElementById('prevPage').disabled).toBe(true);
    });

    test('nextPage disabled on last page', () => {
        manager.currentPage = 5; manager.totalPages = 5;
        manager.updatePagination();
        expect(document.getElementById('nextPage').disabled).toBe(true);
    });

    test('both buttons enabled on a middle page', () => {
        manager.currentPage = 3; manager.totalPages = 5;
        manager.updatePagination();
        expect(document.getElementById('prevPage').disabled).toBe(false);
        expect(document.getElementById('nextPage').disabled).toBe(false);
    });

    test('dropdown shows "Page X of Y"', () => {
        manager.currentPage = 2; manager.totalPages = 7;
        manager.updatePagination();
        expect(document.getElementById('pageDropdown').textContent).toBe('Page 2 of 7');
    });

    test('pageList has correct number of items', () => {
        manager.currentPage = 1; manager.totalPages = 4;
        manager.updatePagination();
        expect(document.getElementById('pageList').querySelectorAll('li').length).toBe(4);
    });

    test('active page link has active-page class', () => {
        manager.currentPage = 3; manager.totalPages = 5;
        manager.updatePagination();
        const links = document.querySelectorAll('#pageList a');
        expect(links[2].classList.contains('active-page')).toBe(true);
    });

    test('single page: both buttons disabled', () => {
        manager.currentPage = 1; manager.totalPages = 1;
        manager.updatePagination();
        expect(document.getElementById('prevPage').disabled).toBe(true);
        expect(document.getElementById('nextPage').disabled).toBe(true);
    });
});

// ─── UsersPageManager.updateStats ────────────────────────────────────────────

describe('UsersPageManager.updateStats', () => {
    beforeEach(() => { setupDOM(); });

    test('sets totalUsers element to the provided count', () => {
        const manager = makeManager();
        manager.totalUsers = 150;
        manager.updateStats();
        expect(document.getElementById('totalUsers').textContent).toBe('150');
    });

    test('shows 0 correctly', () => {
        const manager = makeManager();
        manager.totalUsers = 0;
        manager.updateStats();
        expect(document.getElementById('totalUsers').textContent).toBe('0');
    });
});

// ─── UsersPageManager sort order toggle ──────────────────────────────────────

describe('UsersPageManager.toggleSortOrder', () => {
    let manager;

    beforeEach(() => { setupDOM(); manager = makeManager(); });

    test('toggles from asc to desc', () => {
        manager.sortOrder = 'asc';
        manager.toggleSortOrder();
        expect(manager.sortOrder).toBe('desc');
    });

    test('toggles from desc to asc', () => {
        manager.sortOrder = 'desc';
        manager.toggleSortOrder();
        expect(manager.sortOrder).toBe('asc');
    });

    test('sort icon is updated after toggle', () => {
        manager.sortOrder = 'asc';
        manager.toggleSortOrder();
        const icon = document.querySelector('.sort-icon').textContent;
        expect(['↑', '↓']).toContain(icon);
    });
});

// ─── UsersPageManager.loadUsers fetch integration ────────────────────────────

describe('UsersPageManager.loadUsers', () => {
    beforeEach(() => { setupDOM(); });

    test('calls /api/users endpoint', async () => {
        global.fetch = jest.fn().mockResolvedValue({
            ok: true, json: async () => ({ items: [], totalPages: 1, totalItems: 0 })
        });
        const manager = new UsersPageManager();
        await manager.loadUsers();
        expect(global.fetch).toHaveBeenCalledWith(expect.stringContaining('/api/users'));
    });

    test('URL includes search param when searchTerm is set', async () => {
        global.fetch = jest.fn().mockResolvedValue({
            ok: true, json: async () => ({ items: [], totalPages: 1, totalItems: 0 })
        });
        const manager = new UsersPageManager();
        manager.searchTerm = 'alice';
        await manager.loadUsers();
        expect(global.fetch.mock.calls.at(-1)[0]).toContain('search=alice');
    });

    test('shows emptyState when API returns no items', async () => {
        global.fetch = jest.fn().mockResolvedValue({
            ok: true, json: async () => ({ items: [], totalPages: 1, totalItems: 0 })
        });
        const manager = new UsersPageManager();
        await manager.loadUsers();
        expect(document.getElementById('emptyState').style.display).toBe('block');
    });

    test('renders cards when API returns items', async () => {
        const users = [makeUser({ name: 'alice' }), makeUser({ name: 'bob' })];
        global.fetch = jest.fn().mockResolvedValue({
            ok: true, json: async () => ({ items: users, totalPages: 1, totalItems: 2 })
        });
        const manager = new UsersPageManager();
        await manager.loadUsers();
        expect(document.querySelectorAll('#usersGrid .user-card').length).toBe(2);
    });

    test('shows error state when server responds with ok: false', async () => {
        global.fetch = jest.fn().mockResolvedValue({ ok: false, status: 500 });
        const manager = new UsersPageManager();
        await manager.loadUsers();
        expect(document.getElementById('usersGrid').innerHTML).toContain('Failed');
    });

    test('shows error state when fetch throws (network timeout)', async () => {
        global.fetch = jest.fn().mockRejectedValue(new Error('Network timeout'));
        const manager = new UsersPageManager();
        await manager.loadUsers();
        expect(document.getElementById('usersGrid').innerHTML).toContain('Failed');
    });
});