import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';
import { ItemActionsManager } from '../../../../../main/webapp/js/tableScripts/items/itemActions.js';

describe('ItemActionsManager', () => {
    let itemActionsManager;
    let tableBody;
    let mockConfig;
    let mockInlineEditManager;

    beforeEach(() => {
        HTMLFormElement.prototype.submit = jest.fn();

        const meta = document.createElement('meta');
        meta.name = '_csrf_token';
        meta.content = 'test-token';
        document.head.appendChild(meta);

        tableBody = document.createElement('tbody');

        const row = document.createElement('tr');
        row.innerHTML = `
            <td>Test Game</td>
            <td>85</td>
            <td></td>
            <td>
                <button class="status-button" data-item-name="Test Game">✅</button>
            </td>
            <td>
                <button class="btn btn--icon btn--edit" data-item-name="Test Game">✏️</button>
                <form>
                    <button type="button" class="btn btn--icon btn--delete" data-item-name="Test Game">🗑️</button>
                </form>
            </td>
        `;
        tableBody.appendChild(row);

        mockConfig = {
            entityType: 'games',
            entityNameSingular: 'Game',
            selectors: {
                statusButtonClass: '.status-button',
                deleteIconButtonClass: '.btn--delete',
                editIconButtonClass: '.btn--edit'
            },
            paramNames: {
                toggleItemStatus: 'toggleGameStatus',
                removeItem: 'removeGame'
            },
            itemNameAttribute: 'data-item-name',
            csrfParameterName: '_csrf'
        };

        mockInlineEditManager = {
            toggleRowEdit: jest.fn()
        };

        itemActionsManager = new ItemActionsManager(tableBody, mockConfig, mockInlineEditManager);

        global.fetch = jest.fn();
        global.confirm = jest.fn(() => true);
        global.alert = jest.fn();
    });

    afterEach(() => {
        document.head.innerHTML = '';
        jest.restoreAllMocks();
        delete HTMLFormElement.prototype.submit;
    });

    describe('Constructor', () => {
        test('should initialize with tableBody, config, and inlineEditManager', () => {
            expect(itemActionsManager.tableBody).toBe(tableBody);
            expect(itemActionsManager.config).toBe(mockConfig);
            expect(itemActionsManager.inlineEditManager).toBe(mockInlineEditManager);
        });
    });

    describe('init', () => {
        test('should attach event listener to tableBody', () => {
            const spy = jest.spyOn(tableBody, 'addEventListener');

            itemActionsManager.init();

            expect(spy).toHaveBeenCalledWith('click', expect.any(Function));
        });

        test('should log error if tableBody is missing', () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
            const manager = new ItemActionsManager(null, mockConfig, mockInlineEditManager);

            manager.init();

            expect(consoleErrorSpy).toHaveBeenCalled();
            consoleErrorSpy.mockRestore();
        });

        test('should log error if selectors are missing', () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
            const badConfig = { ...mockConfig, selectors: null };
            const manager = new ItemActionsManager(tableBody, badConfig, mockInlineEditManager);

            manager.init();

            expect(consoleErrorSpy).toHaveBeenCalled();
            consoleErrorSpy.mockRestore();
        });
    });

    describe('handleToggleStatus', () => {
        beforeEach(() => {
            itemActionsManager.init();
        });

        test('should toggle status successfully', async () => {
            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => ({ success: true, data: { newStatus: false } })
            });

            const statusButton = tableBody.querySelector('.status-button');
            statusButton.click();

            await new Promise(resolve => setTimeout(resolve, 0));

            expect(global.fetch).toHaveBeenCalled();
            expect(statusButton.innerHTML).toBe('❌');
        });

        test('should send correct form data', async () => {
            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => ({ success: true, data: { newStatus: true } })
            });

            const statusButton = tableBody.querySelector('.status-button');
            statusButton.click();

            await new Promise(resolve => setTimeout(resolve, 0));

            const callArgs = global.fetch.mock.calls[0];
            const formData = new URLSearchParams(callArgs[1].body);

            expect(formData.get('toggleGameStatus')).toBe('Test Game');
            expect(formData.get('_csrf')).toBe('test-token');
        });

        test('should handle missing item name', () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
            const statusButton = tableBody.querySelector('.status-button');
            statusButton.removeAttribute('data-item-name');

            statusButton.click();

            expect(consoleErrorSpy).toHaveBeenCalled();
            expect(global.fetch).not.toHaveBeenCalled();

            consoleErrorSpy.mockRestore();
        });

        test('should prevent toggle during edit mode', () => {
            global.alert = jest.fn();

            const row = tableBody.querySelector('tr');
            row.classList.add('is-editing');

            const statusButton = tableBody.querySelector('.status-button');
            statusButton.click();

            expect(global.alert).toHaveBeenCalledWith(
                expect.stringContaining('save or cancel your edits')
            );
            expect(global.fetch).not.toHaveBeenCalled();
        });

        test('should handle missing CSRF token', () => {
            document.head.innerHTML = '';
            global.alert = jest.fn();

            const statusButton = tableBody.querySelector('.status-button');
            statusButton.click();

            expect(global.alert).toHaveBeenCalledWith(
                expect.stringContaining('Security token missing')
            );
            expect(global.fetch).not.toHaveBeenCalled();
        });

        test('should show loading state', async () => {
            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => ({ success: true, data: { newStatus: false } })
            });

            const statusButton = tableBody.querySelector('.status-button');

            statusButton.click();

            expect(statusButton.disabled).toBe(true);
            expect(statusButton.style.opacity).toBe('0.5');

            await new Promise(resolve => setTimeout(resolve, 0));

            expect(statusButton.disabled).toBe(false);
            expect(statusButton.style.opacity).toBe('1');
        });

        test('should handle server errors', async () => {
            global.fetch.mockResolvedValue({
                ok: false,
                status: 500,
                text: async () => 'Server error'
            });

            const statusButton = tableBody.querySelector('.status-button');
            const originalContent = statusButton.innerHTML;

            statusButton.click();

            await new Promise(resolve => setTimeout(resolve, 0));

            expect(statusButton.innerHTML).toBe(originalContent);
        });

        test('should handle network errors', async () => {
            global.fetch.mockRejectedValue(new Error('Network error'));

            const statusButton = tableBody.querySelector('.status-button');
            const originalContent = statusButton.innerHTML;

            statusButton.click();

            await new Promise(resolve => setTimeout(resolve, 0));

            expect(statusButton.innerHTML).toBe(originalContent);
        });

        test('should handle invalid response format', async () => {
            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => ({ success: false, message: 'Invalid' })
            });

            const statusButton = tableBody.querySelector('.status-button');
            const originalContent = statusButton.innerHTML;

            statusButton.click();

            await new Promise(resolve => setTimeout(resolve, 0));

            expect(statusButton.innerHTML).toBe(originalContent);
        });
    });

    describe('handleDeleteConfirmation', () => {
        beforeEach(() => {
            itemActionsManager.init();
        });

        test('should show confirmation dialog', () => {
            const deleteButton = tableBody.querySelector('.btn--delete');

            deleteButton.click();

            expect(global.confirm).toHaveBeenCalledWith(
                expect.stringContaining('Test Game')
            );
        });

        test('should delete item via AJAX on confirmation', async () => {
            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => ({ success: true })
            });

            const deleteButton = tableBody.querySelector('.btn--delete');
            deleteButton.click();

            await new Promise(resolve => setTimeout(resolve, 0));

            expect(global.confirm).toHaveBeenCalled();
            expect(global.fetch).toHaveBeenCalled();

            const callArgs = global.fetch.mock.calls[0];
            const formData = new URLSearchParams(callArgs[1].body);
            expect(formData.get('removeGame')).toBe('Test Game');
            expect(formData.get('_csrf')).toBe('test-token');
        });

        test('should not delete if user cancels', async () => {
            global.confirm = jest.fn(() => false);
            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => ({ success: true })
            });

            const deleteButton = tableBody.querySelector('.btn--delete');
            deleteButton.click();

            await new Promise(resolve => setTimeout(resolve, 0));

            expect(global.fetch).not.toHaveBeenCalled();
        });

        test('should prevent delete during edit mode', () => {
            global.alert = jest.fn();

            const row = tableBody.querySelector('tr');
            row.classList.add('is-editing');

            const deleteButton = tableBody.querySelector('.btn--delete');
            deleteButton.click();

            expect(global.alert).toHaveBeenCalledWith(
                expect.stringContaining('save or cancel your edits')
            );
            expect(global.confirm).not.toHaveBeenCalled();
        });

        test('should handle missing item name', () => {
            const deleteButton = tableBody.querySelector('.btn--delete');
            deleteButton.removeAttribute('data-item-name');

            deleteButton.click();

            expect(global.confirm).not.toHaveBeenCalled();
        });

        test('should handle missing CSRF token', async () => {
            document.head.innerHTML = '';
            global.alert = jest.fn();

            const deleteButton = tableBody.querySelector('.btn--delete');
            deleteButton.click();

            await new Promise(resolve => setTimeout(resolve, 0));

            expect(global.alert).toHaveBeenCalledWith(
                expect.stringContaining('Security token missing')
            );
            expect(global.fetch).not.toHaveBeenCalled();
        });

        test('should remove row after successful delete', async () => {
            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => ({ success: true })
            });

            const deleteButton = tableBody.querySelector('.btn--delete');
            const row = deleteButton.closest('tr');

            deleteButton.click();

            await new Promise(resolve => setTimeout(resolve, 0));

            expect(row.style.opacity).toBe('0');

            await new Promise(resolve => setTimeout(resolve, 350));
            expect(tableBody.contains(row)).toBe(false);
        });

        test('should handle server errors', async () => {
            global.fetch.mockResolvedValue({
                ok: false,
                status: 500,
                text: async () => 'Server error'
            });

            const deleteButton = tableBody.querySelector('.btn--delete');
            const originalContent = deleteButton.innerHTML;

            deleteButton.click();

            await new Promise(resolve => setTimeout(resolve, 0));

            expect(deleteButton.innerHTML).toBe(originalContent);
        });
    });

    describe('handleEditClick', () => {
        beforeEach(() => {
            itemActionsManager.init();
        });

        test('should call toggleRowEdit on inline edit manager', () => {
            const editButton = tableBody.querySelector('.btn--edit');

            editButton.click();

            const row = editButton.closest('tr');
            expect(mockInlineEditManager.toggleRowEdit).toHaveBeenCalledWith(row);
        });

        test('should handle missing inline edit manager', () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
            const manager = new ItemActionsManager(tableBody, mockConfig, null);
            manager.init();

            const editButton = tableBody.querySelector('.btn--edit');
            editButton.click();

            expect(consoleErrorSpy).toHaveBeenCalled();
            consoleErrorSpy.mockRestore();
        });

        test('should find correct row element', () => {
            const editButton = tableBody.querySelector('.btn--edit');
            editButton.click();

            const row = tableBody.querySelector('tr');
            expect(mockInlineEditManager.toggleRowEdit).toHaveBeenCalledWith(row);
        });
    });

    describe('Event Delegation', () => {
        beforeEach(() => {
            itemActionsManager.init();
        });

        test('should handle clicks on nested elements', () => {
            const span = document.createElement('span');
            span.textContent = 'Edit';
            const editButton = tableBody.querySelector('.btn--edit');
            editButton.appendChild(span);

            span.click();

            expect(mockInlineEditManager.toggleRowEdit).toHaveBeenCalled();
        });

        test('should not trigger actions for non-button clicks', () => {
            const td = tableBody.querySelector('td');

            td.click();

            expect(mockInlineEditManager.toggleRowEdit).not.toHaveBeenCalled();
            expect(global.fetch).not.toHaveBeenCalled();
            expect(global.confirm).not.toHaveBeenCalled();
        });

        test('should handle multiple rows', () => {
            tableBody.innerHTML += `
                <tr>
                    <td>Game 2</td>
                    <td>75</td>
                    <td></td>
                    <td>
                        <button class="status-button" data-item-name="Game 2">❌</button>
                    </td>
                    <td>
                        <button class="btn btn--edit" data-item-name="Game 2">✏️</button>
                        <form>
                            <button type="button" class="btn--delete" data-item-name="Game 2">🗑️</button>
                        </form>
                    </td>
                </tr>
            `;

            const editButtons = tableBody.querySelectorAll('.btn--edit');
            editButtons[1].click();

            expect(mockInlineEditManager.toggleRowEdit).toHaveBeenCalled();
        });
    });

    describe('setButtonLoading', () => {
        test('should disable and fade button when loading', () => {
            const button = document.createElement('button');

            itemActionsManager.setButtonLoading(button, true);

            expect(button.disabled).toBe(true);
            expect(button.style.opacity).toBe('0.5');
        });

        test('should enable and restore button when not loading', () => {
            const button = document.createElement('button');
            button.disabled = true;
            button.style.opacity = '0.5';

            itemActionsManager.setButtonLoading(button, false);

            expect(button.disabled).toBe(false);
            expect(button.style.opacity).toBe('1');
        });
    });

    describe('Integration', () => {
        test('should handle complete status toggle flow', async () => {
            itemActionsManager.init();

            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => ({ success: true, data: { newStatus: false } })
            });
            const statusButton = tableBody.querySelector('.status-button');
            expect(statusButton.innerHTML).toBe('✅');

            statusButton.click();

            await new Promise(resolve => setTimeout(resolve, 0));

            expect(statusButton.innerHTML).toBe('❌');
            expect(statusButton.disabled).toBe(false);
        });

        test('should handle complete delete flow', async () => {
            itemActionsManager.init();

            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => ({ success: true })
            });

            const deleteButton = tableBody.querySelector('.btn--delete');
            deleteButton.click();

            await new Promise(resolve => setTimeout(resolve, 0));

            expect(global.confirm).toHaveBeenCalled();
            expect(global.fetch).toHaveBeenCalled();
        });

        test('should handle complete edit flow', () => {
            itemActionsManager.init();

            const editButton = tableBody.querySelector('.btn--edit');
            editButton.click();

            expect(mockInlineEditManager.toggleRowEdit).toHaveBeenCalled();
        });
    });

    describe('Edge Cases', () => {
        test('should handle button with missing closest method', () => {
            itemActionsManager.init();

            const button = {
                dataset: { itemName: 'Test' }
            };

            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

            if (typeof button.closest !== 'function') {
                try {
                    itemActionsManager.handleEditClick(button);
                } catch (error) {
                    expect(error.message).toContain('closest');
                }
            }

            consoleErrorSpy.mockRestore();
        });

        test('should handle very long item names', async () => {
            itemActionsManager.init();

            const longName = 'A'.repeat(1000);
            const statusButton = tableBody.querySelector('.status-button');
            statusButton.dataset.itemName = longName;

            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => ({ success: true, data: { newStatus: false } })
            });

            statusButton.click();

            await new Promise(resolve => setTimeout(resolve, 0));

            expect(global.fetch).toHaveBeenCalled();
        });

        test('should handle special characters in item names', async () => {
            itemActionsManager.init();

            const specialName = '<>&"\'';
            const statusButton = tableBody.querySelector('.status-button');
            statusButton.dataset.itemName = specialName;

            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => ({ success: true, data: { newStatus: false } })
            });

            statusButton.click();

            await new Promise(resolve => setTimeout(resolve, 0));

            const formData = new URLSearchParams(global.fetch.mock.calls[0][1].body);
            expect(formData.get('toggleGameStatus')).toBe(specialName);
        });
    });
});