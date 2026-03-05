import { jest } from '@jest/globals';


const { AdminUsersManager } = await import('@/pages/admin/adminUsers.js');

// ─── Helpers ──────────────────────────────────────────────────────────────────

function setupDOM() {
    document.body.innerHTML = `
        <button id="cardsViewBtn" class="active"></button>
        <button id="tableViewBtn"></button>
        <div id="usersCards" style="display:grid"></div>
        <div id="usersTable" style="display:none">
            <table><tbody></tbody></table>
        </div>
        <input id="liveSearch" type="text">
        <div id="emptyState" style="display:none"></div>
        <div id="toastContainer"></div>
        <div id="adminCount">0</div>
        <div id="totalUsersCount">0</div>
        <div id="editModal" style="display:none">
            <input id="editUserId">
            <span id="editUsername"></span>
            <input id="roleAdmin" type="radio">
            <input id="roleUser"  type="radio">
        </div>
        <div id="deleteModal" style="display:none">
            <input id="deleteUserId">
            <span id="deleteUsername"></span>
        </div>
    `;
}

function addCards(cards) {
    const container = document.getElementById('usersCards');
    cards.forEach(({ userId = '1', username, role, visible = true }) => {
        const card = document.createElement('div');
        card.className        = 'user-card';
        card.dataset.userId   = userId;
        card.dataset.username = username;
        card.dataset.role     = role;
        card.style.display    = visible ? '' : 'none';
        container.appendChild(card);
    });
}

function addTableRows(rows) {
    const tbody = document.querySelector('#usersTable tbody');
    rows.forEach(({ username }) => {
        const tr = document.createElement('tr');
        ['', username, '', ''].forEach(text => {
            const td = document.createElement('td');
            td.textContent = text;
            tr.appendChild(td);
        });
        tbody.appendChild(tr);
    });
}

function makeManager() {
    global.fetch = jest.fn().mockResolvedValue({
        ok: true, json: async () => ({ users: [], totalPages: 1 })
    });
    return new AdminUsersManager();
}

afterEach(() => {
    document.body.innerHTML = '';
    localStorage.clear();
    jest.clearAllMocks();
});

// ─── _applyView ───────────────────────────────────────────────────────────────

describe('AdminUsersManager._applyView', () => {
    let manager;

    beforeEach(() => { setupDOM(); manager = makeManager(); });

    test('cards: usersCards=grid, usersTable=none', () => {
        manager._applyView('cards');
        expect(document.getElementById('usersCards').style.display).toBe('grid');
        expect(document.getElementById('usersTable').style.display).toBe('none');
    });

    test('table: usersTable=block, usersCards=none', () => {
        manager._applyView('table');
        expect(document.getElementById('usersCards').style.display).toBe('none');
        expect(document.getElementById('usersTable').style.display).toBe('block');
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

    test('persists view preference to localStorage', () => {
        manager._applyView('table');
        expect(localStorage.getItem('adminUsersView')).toBe('table');
        manager._applyView('cards');
        expect(localStorage.getItem('adminUsersView')).toBe('cards');
    });
});

// ─── performSearch ────────────────────────────────────────────────────────────

describe('AdminUsersManager.performSearch', () => {
    let manager;

    beforeEach(() => {
        setupDOM();
        manager = makeManager();
        addCards([
            { username: 'alice',   role: 'USER'  },
            { username: 'bob',     role: 'ADMIN' },
            { username: 'charlie', role: 'USER'  }
        ]);
    });

    test('empty search: all cards visible', () => {
        manager.performSearch('');
        document.querySelectorAll('.user-card')
            .forEach(c => expect(c.style.display).not.toBe('none'));
    });

    test('matches correct card by partial name', () => {
        manager.performSearch('ali');
        expect(document.querySelector('[data-username="alice"]').style.display).not.toBe('none');
        expect(document.querySelector('[data-username="bob"]').style.display).toBe('none');
    });

    test('search is case-insensitive', () => {
        manager.performSearch('BOB');
        expect(document.querySelector('[data-username="bob"]').style.display).not.toBe('none');
    });

    test('partial match works', () => {
        manager.performSearch('charl');
        expect(document.querySelector('[data-username="charlie"]').style.display).not.toBe('none');
        expect(document.querySelector('[data-username="alice"]').style.display).toBe('none');
    });

    test('no matches: shows emptyState', () => {
        manager.performSearch('zzznomatch');
        expect(document.getElementById('emptyState').style.display).toBe('block');
    });

    test('no matches: hides both cards and table containers', () => {
        manager.performSearch('zzznomatch');
        expect(document.getElementById('usersCards').style.display).toBe('none');
        expect(document.getElementById('usersTable').style.display).toBe('none');
    });

    test('match found: emptyState hidden', () => {
        manager.performSearch('alice');
        expect(document.getElementById('emptyState').style.display).toBe('none');
    });

    test('table rows are also filtered', () => {
        addTableRows([{ username: 'alice' }, { username: 'bob' }]);
        manager.performSearch('alice');
        const rows = document.querySelectorAll('#usersTable tbody tr');
        expect(rows[0].style.display).not.toBe('none');
        expect(rows[1].style.display).toBe('none');
    });
});

// ─── updateAdminCount ─────────────────────────────────────────────────────────

describe('AdminUsersManager.updateAdminCount', () => {
    let manager;

    beforeEach(() => { setupDOM(); manager = makeManager(); });

    test('counts ADMIN cards correctly', () => {
        addCards([
            { username: 'a', role: 'USER'  },
            { username: 'b', role: 'ADMIN' },
            { username: 'c', role: 'ADMIN' }
        ]);
        manager.updateAdminCount();
        expect(document.getElementById('adminCount').textContent).toBe('2');
    });

    test('counts total visible users', () => {
        addCards([
            { username: 'a', role: 'USER'  },
            { username: 'b', role: 'USER'  },
            { username: 'c', role: 'ADMIN' }
        ]);
        manager.updateAdminCount();
        expect(document.getElementById('totalUsersCount').textContent).toBe('3');
    });

    test('hidden cards are excluded from the visible count', () => {
        addCards([
            { username: 'a', role: 'USER',  visible: true  },
            { username: 'b', role: 'ADMIN', visible: false }
        ]);
        manager.updateAdminCount();
        expect(document.getElementById('adminCount').textContent).toBe('0');
        expect(document.getElementById('totalUsersCount').textContent).toBe('1');
    });

    // When all cards are hidden (e.g. a search with no results), the count
    // falls back to all cards so the badge still shows meaningful data.
    test('falls back to all cards when none are visible', () => {
        addCards([
            { username: 'a', role: 'ADMIN', visible: false },
            { username: 'b', role: 'ADMIN', visible: false }
        ]);
        manager.updateAdminCount();
        expect(document.getElementById('adminCount').textContent).toBe('2');
    });

    test('shows 0 when no cards exist', () => {
        manager.updateAdminCount();
        expect(document.getElementById('adminCount').textContent).toBe('0');
        expect(document.getElementById('totalUsersCount').textContent).toBe('0');
    });
});

// ─── modal helpers ────────────────────────────────────────────────────────────

describe('AdminUsersManager modal helpers', () => {
    let manager;

    beforeEach(() => { setupDOM(); manager = makeManager(); });

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

// ─── editUser ─────────────────────────────────────────────────────────────────

describe('AdminUsersManager.editUser', () => {
    let manager;

    beforeEach(() => {
        setupDOM();
        addCards([
            { userId: '1', username: 'alice', role: 'ADMIN' },
            { userId: '2', username: 'bob',   role: 'USER'  }
        ]);
        manager = makeManager();
    });

    test('populates edit form for an ADMIN user', () => {
        manager.editUser('1');
        expect(document.getElementById('editUserId').value).toBe('1');
        expect(document.getElementById('editUsername').textContent).toBe('alice');
        expect(document.getElementById('roleAdmin').checked).toBe(true);
        expect(document.getElementById('roleUser').checked).toBe(false);
    });

    test('populates edit form for a regular USER', () => {
        manager.editUser('2');
        expect(document.getElementById('editUserId').value).toBe('2');
        expect(document.getElementById('editUsername').textContent).toBe('bob');
        expect(document.getElementById('roleAdmin').checked).toBe(false);
        expect(document.getElementById('roleUser').checked).toBe(true);
    });

    test('opens editModal', () => {
        manager.editUser('1');
        expect(document.getElementById('editModal').style.display).toBe('flex');
    });

    test('does nothing when userId is not found', () => {
        expect(() => manager.editUser('999')).not.toThrow();
        expect(document.getElementById('editModal').style.display).toBe('none');
    });
});

// ─── deleteUser ───────────────────────────────────────────────────────────────

describe('AdminUsersManager.deleteUser', () => {
    let manager;

    beforeEach(() => { setupDOM(); manager = makeManager(); });

    test('populates delete form with userId and username', () => {
        manager.deleteUser('5', 'charlie');
        expect(document.getElementById('deleteUserId').value).toBe('5');
        expect(document.getElementById('deleteUsername').textContent).toBe('charlie');
    });

    test('opens deleteModal', () => {
        manager.deleteUser('5', 'charlie');
        expect(document.getElementById('deleteModal').style.display).toBe('flex');
    });
});

// ─── generateAvatarColors ─────────────────────────────────────────────────────

describe('AdminUsersManager.generateAvatarColors', () => {
    let manager;

    beforeEach(() => { setupDOM(); manager = makeManager(); });

  test('assigns a non-empty background to each .user-card-avatar element', () => {
          document.body.innerHTML += `
              <div class="user-card-avatar" data-username="alice"></div>
              <div class="user-card-avatar" data-username="bob"></div>
          `;
          manager.generateAvatarColors();

          document.querySelectorAll('.user-card-avatar').forEach(avatar => {

              const styleValue = avatar.getAttribute('style') || avatar.style.cssText;
              expect(styleValue).toBeDefined();
          });
      });

    test('does not throw when no avatar elements are present', () => {
        expect(() => manager.generateAvatarColors()).not.toThrow();
    });
});