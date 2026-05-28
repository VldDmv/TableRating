import { jest } from '@jest/globals';

// ─── Mock dependencies ────────────────────────────────────────────────────────

jest.unstable_mockModule('@/tableScripts/core/errorHandler.js', () => ({
    ErrorHandler: {
        parseErrorResponse: jest.fn(async () => 'Server Error'),
        handle: jest.fn(),
    },
}));

jest.unstable_mockModule('@/tableScripts/core/utils.js', () => ({
    securityUtils: { getCsrfToken: jest.fn(() => 'mock-csrf-token') },
    ICONS: { COMPLETED: '✅', NOT_COMPLETED: '❌' },
}));

const { ItemActionsManager } = await import('@/tableScripts/items/itemActions.js');
const { ErrorHandler } = await import('@/tableScripts/core/errorHandler.js');
const { securityUtils } = await import('@/tableScripts/core/utils.js');

// ─── Helpers ──────────────────────────────────────────────────────────────────

function makeConfig() {
    return {
        entityType: 'games',
        selectors: {
            statusButtonClass: '.status-button',
            editIconButtonClass: '.edit-button',
            deleteIconButtonClass: '.delete-button',
        },
    };
}

function makeTableBody() {
    const tb = document.createElement('tbody');
    document.body.appendChild(tb);
    return tb;
}

function makeRow(options = {}) {
    const tr = document.createElement('tr');
    if (options.editing) tr.classList.add('is-editing');

    const td = document.createElement('td');
    const btn = document.createElement('button');
    btn.className = options.btnClass || 'status-button';
    btn.dataset.itemName = options.itemName || 'Test Game';
    btn.innerHTML = options.icon || '✅';
    td.appendChild(btn);
    tr.appendChild(td);
    return { tr, btn };
}

// ─── setButtonLoading ─────────────────────────────────────────────────────────

describe('ItemActionsManager.setButtonLoading', () => {
    let manager;
    let tableBody;

    beforeEach(() => {
        tableBody = makeTableBody();
        manager = new ItemActionsManager(tableBody, makeConfig(), null);
    });

    afterEach(() => {
        document.body.innerHTML = '';
    });

    test('disables button when loading=true', () => {
        const btn = document.createElement('button');
        manager.setButtonLoading(btn, true);
        expect(btn.disabled).toBe(true);
    });

    test('reduces opacity to 0.5 when loading', () => {
        const btn = document.createElement('button');
        manager.setButtonLoading(btn, true);
        expect(btn.style.opacity).toBe('0.5');
    });

    test('enables button when loading=false', () => {
        const btn = document.createElement('button');
        btn.disabled = true;
        manager.setButtonLoading(btn, false);
        expect(btn.disabled).toBe(false);
    });

    test('restores opacity to 1 when not loading', () => {
        const btn = document.createElement('button');
        btn.style.opacity = '0.5';
        manager.setButtonLoading(btn, false);
        expect(btn.style.opacity).toBe('1');
    });
});

// ─── handleToggleStatus ───────────────────────────────────────────────────────

describe('ItemActionsManager.handleToggleStatus', () => {
    let manager;
    let tableBody;

    beforeEach(() => {
        tableBody = makeTableBody();
        manager = new ItemActionsManager(tableBody, makeConfig(), null);
        global.fetch = jest.fn();
        securityUtils.getCsrfToken.mockReturnValue('mock-csrf');
    });

    afterEach(() => {
        document.body.innerHTML = '';
        jest.resetAllMocks();
    });

    test('does nothing when button has no itemName', async () => {
        const btn = document.createElement('button');
        btn.dataset.itemName = '';
        await manager.handleToggleStatus(btn);
        expect(global.fetch).not.toHaveBeenCalled();
    });

    test('alerts and returns when row is editing', async () => {
        const { tr, btn } = makeRow({ editing: true, itemName: 'Game' });
        tableBody.appendChild(tr);
        global.alert = jest.fn();

        await manager.handleToggleStatus(btn);

        expect(global.alert).toHaveBeenCalled();
        expect(global.fetch).not.toHaveBeenCalled();
    });

    test('calls correct PATCH endpoint', async () => {
        const { tr, btn } = makeRow({ itemName: 'Witcher 3' });
        tableBody.appendChild(tr);
        global.fetch.mockResolvedValue({
            ok: true,
            json: async () => ({ completed: true }),
        });

        await manager.handleToggleStatus(btn);

        expect(global.fetch).toHaveBeenCalledWith(
            '/api/games/Witcher%203/toggle',
            expect.objectContaining({ method: 'PATCH' })
        );
    });

    test('sends X-XSRF-TOKEN header', async () => {
        const { tr, btn } = makeRow({ itemName: 'Game' });
        tableBody.appendChild(tr);
        global.fetch.mockResolvedValue({
            ok: true,
            json: async () => ({ completed: false }),
        });

        await manager.handleToggleStatus(btn);

        expect(global.fetch).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({
                headers: expect.objectContaining({ 'X-XSRF-TOKEN': 'mock-csrf' }),
            })
        );
    });

    test('updates button to COMPLETED icon when server returns completed:true', async () => {
        const { tr, btn } = makeRow({ itemName: 'Game', icon: '❌' });
        tableBody.appendChild(tr);
        global.fetch.mockResolvedValue({
            ok: true,
            json: async () => ({ completed: true }),
        });

        await manager.handleToggleStatus(btn);

        expect(btn.innerHTML).toBe('✅');
    });

    test('updates button to NOT_COMPLETED icon when server returns completed:false', async () => {
        const { tr, btn } = makeRow({ itemName: 'Game', icon: '✅' });
        tableBody.appendChild(tr);
        global.fetch.mockResolvedValue({
            ok: true,
            json: async () => ({ completed: false }),
        });

        await manager.handleToggleStatus(btn);

        expect(btn.innerHTML).toBe('❌');
    });

    test('restores original button content on fetch error', async () => {
        const { tr, btn } = makeRow({ itemName: 'Game', icon: '✅' });
        tableBody.appendChild(tr);
        global.fetch.mockResolvedValue({ ok: false, status: 500 });

        await manager.handleToggleStatus(btn);

        expect(btn.innerHTML).toBe('✅');
    });
});

// ─── handleDeleteConfirmation ────────────────────────────────────────────────

describe('ItemActionsManager.handleDeleteConfirmation', () => {
    let manager;
    let tableBody;

    beforeEach(() => {
        tableBody = makeTableBody();
        manager = new ItemActionsManager(tableBody, makeConfig(), null);
        global.fetch = jest.fn();
        global.confirm = jest.fn(() => true);
        securityUtils.getCsrfToken.mockReturnValue('mock-csrf');
    });

    afterEach(() => {
        document.body.innerHTML = '';
        jest.resetAllMocks();
    });

    test('does nothing when no itemName', async () => {
        const btn = document.createElement('button');
        btn.dataset.itemName = '';
        await manager.handleDeleteConfirmation(btn);
        expect(global.fetch).not.toHaveBeenCalled();
    });

    test('does not delete when user cancels confirmation', async () => {
        global.confirm = jest.fn(() => false);
        const { tr, btn } = makeRow({ itemName: 'Game' });
        tableBody.appendChild(tr);

        await manager.handleDeleteConfirmation(btn);

        expect(global.fetch).not.toHaveBeenCalled();
    });

    test('alerts and returns when row is editing', async () => {
        const { tr, btn } = makeRow({ editing: true, btnClass: 'delete-button', itemName: 'Game' });
        tableBody.appendChild(tr);
        global.alert = jest.fn();

        await manager.handleDeleteConfirmation(btn);

        expect(global.alert).toHaveBeenCalled();
        expect(global.fetch).not.toHaveBeenCalled();
    });

    test('calls DELETE endpoint with encoded name', async () => {
        const { tr, btn } = makeRow({ btnClass: 'delete-button', itemName: 'Dark Souls 3' });
        tableBody.appendChild(tr);
        global.fetch.mockResolvedValue({ ok: true });

        await manager.handleDeleteConfirmation(btn);

        expect(global.fetch).toHaveBeenCalledWith(
            '/api/games/Dark%20Souls%203',
            expect.objectContaining({ method: 'DELETE' })
        );
    });

    test('removes row from DOM after successful delete', async () => {
        jest.useFakeTimers();
        const { tr, btn } = makeRow({ btnClass: 'delete-button', itemName: 'Game' });
        tableBody.appendChild(tr);
        global.fetch.mockResolvedValue({ ok: true });

        await manager.handleDeleteConfirmation(btn);
        jest.advanceTimersByTime(400);

        expect(tableBody.contains(tr)).toBe(false);
        jest.useRealTimers();
    });

    test('shows confirmation dialog with item name', async () => {
        const { tr, btn } = makeRow({ btnClass: 'delete-button', itemName: 'My Game' });
        tableBody.appendChild(tr);
        global.fetch.mockResolvedValue({ ok: true });

        await manager.handleDeleteConfirmation(btn);

        expect(global.confirm).toHaveBeenCalledWith(expect.stringContaining('My Game'));
    });
});

// ─── handleEditClick ──────────────────────────────────────────────────────────

describe('ItemActionsManager.handleEditClick', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    test('calls inlineEditManager.toggleRowEdit with the row', () => {
        const tableBody = makeTableBody();
        const inlineEditMock = { toggleRowEdit: jest.fn() };
        const manager = new ItemActionsManager(tableBody, makeConfig(), inlineEditMock);

        const tr = document.createElement('tr');
        const btn = document.createElement('button');
        tr.appendChild(btn);
        tableBody.appendChild(tr);

        manager.handleEditClick(btn);

        expect(inlineEditMock.toggleRowEdit).toHaveBeenCalledWith(tr);
    });

    test('does nothing when inlineEditManager is null', () => {
        const tableBody = makeTableBody();
        const manager = new ItemActionsManager(tableBody, makeConfig(), null);
        const btn = document.createElement('button');

        expect(() => manager.handleEditClick(btn)).not.toThrow();
    });
});

// ─── init event delegation ────────────────────────────────────────────────────

describe('ItemActionsManager.init event delegation', () => {
    let tableBody;
    let manager;

    beforeEach(() => {
        tableBody = makeTableBody();
        manager = new ItemActionsManager(tableBody, makeConfig(), null);
        manager.init();
    });

    afterEach(() => {
        document.body.innerHTML = '';
    });

    test('delegates status button click to handleToggleStatus', () => {
        const spy = jest.spyOn(manager, 'handleToggleStatus').mockResolvedValue();

        const tr = document.createElement('tr');
        const btn = document.createElement('button');
        btn.className = 'status-button';
        btn.dataset.itemName = 'Game';
        tr.appendChild(btn);
        tableBody.appendChild(tr);

        btn.click();

        expect(spy).toHaveBeenCalledWith(btn);
    });

    test('delegates delete button click to handleDeleteConfirmation', () => {
        const spy = jest.spyOn(manager, 'handleDeleteConfirmation').mockResolvedValue();

        const tr = document.createElement('tr');
        const btn = document.createElement('button');
        btn.className = 'delete-button';
        btn.dataset.itemName = 'Game';
        tr.appendChild(btn);
        tableBody.appendChild(tr);

        btn.click();

        expect(spy).toHaveBeenCalledWith(btn);
    });

    test('delegates edit button click to handleEditClick', () => {
        const spy = jest.spyOn(manager, 'handleEditClick').mockImplementation(() => {});

        const tr = document.createElement('tr');
        const btn = document.createElement('button');
        btn.className = 'edit-button';
        btn.dataset.itemName = 'Game';
        tr.appendChild(btn);
        tableBody.appendChild(tr);

        btn.click();

        expect(spy).toHaveBeenCalledWith(btn);
    });
});
