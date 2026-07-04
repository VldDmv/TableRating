/**
 * Handles item status toggling, deletion, and edit initiation from table rows.
 */

import { ErrorHandler } from '../core/errorHandler.js';
import { securityUtils, ICONS } from '../core/utils.js';

export class ItemActionsManager {
    constructor(tableBody, config, inlineEditManager) {
        this.tableBody = tableBody;
        this.config = config;
        this.inlineEditManager = inlineEditManager;
    }

    /**
     * Initializes event listeners for actions.
     */
    init() {
        if (!this.tableBody || !this.config.selectors) {
            console.error('ItemActionsManager: Missing tableBody or selectors configuration.');
            return;
        }

        const statusButtonSelector = this.config.selectors.statusButtonClass;
        const deleteButtonSelector = this.config.selectors.deleteIconButtonClass;
        const editButtonSelector = this.config.selectors.editIconButtonClass;

        this.tableBody.addEventListener('click', (event) => {
            const target = event.target;
            const button = target.closest('button');
            if (!button) return;

            if (button.matches(statusButtonSelector)) {
                this.handleToggleStatus(button);
            } else if (button.matches(deleteButtonSelector)) {
                this.handleDeleteConfirmation(button);
            } else if (button.matches(editButtonSelector)) {
                this.handleEditClick(button);
            }
        });
    }

    /**
     * Toggles the completion status of an item via AJAX.
     */
    async handleToggleStatus(button) {
        const itemName = button.dataset.itemName;
        if (!itemName) return;

        const row = button.closest('tr');
        if (row && row.classList.contains('is-editing')) {
            alert('Please save or cancel your edits before changing the status.');
            return;
        }

        const csrfToken = securityUtils.getCsrfToken();
        const originalContent = button.innerHTML;
        this.setButtonLoading(button, true);

        try {
            const url = `/api/${this.config.entityType}/${encodeURIComponent(itemName)}/toggle`;

            const response = await fetch(url, {
                method: 'PATCH',
                headers: {
                    'X-XSRF-TOKEN': csrfToken,
                },
            });

            if (!response.ok) {
                const errorMsg = await ErrorHandler.parseErrorResponse(response);
                throw new Error(errorMsg);
            }

            const data = await response.json();

            if (data && typeof data.completed !== 'undefined') {
                button.innerHTML = data.completed ? ICONS.COMPLETED : ICONS.NOT_COMPLETED;

                if (typeof window.syncItemCompleted === 'function') {
                    window.syncItemCompleted(itemName, data.completed);
                }
            } else {
                throw new Error('Invalid server response.');
            }
        } catch (error) {
            ErrorHandler.handle(error, 'Error toggling item status');
            button.innerHTML = originalContent;
        } finally {
            this.setButtonLoading(button, false);
        }
    }

    async handleDeleteConfirmation(button) {
        const itemName = button.dataset.itemName;
        if (!itemName) return;

        const row = button.closest('tr');
        if (row && row.classList.contains('is-editing')) {
            alert('Please save or cancel your edits before deleting.');
            return;
        }

        const confirmMessage = `Are you sure you want to delete "${itemName}"?`;
        if (!confirm(confirmMessage)) return;

        const originalContent = button.innerHTML;
        this.setButtonLoading(button, true);

        const csrfToken = securityUtils.getCsrfToken();

        try {
            const url = `/api/${this.config.entityType}/${encodeURIComponent(itemName)}`;

            const response = await fetch(url, {
                method: 'DELETE',
                headers: {
                    'X-XSRF-TOKEN': csrfToken,
                },
            });

            if (!response.ok) {
                const errorMsg = await ErrorHandler.parseErrorResponse(response);
                throw new Error(errorMsg);
            }

            // Animate and remove row
            row.style.transition = 'opacity 0.3s';
            row.style.opacity = '0';
            setTimeout(() => row.remove(), 300);
        } catch (error) {
            ErrorHandler.handle(error, 'Error deleting item');
            button.innerHTML = originalContent;
            this.setButtonLoading(button, false);
        }
    }
    handleEditClick(button) {
        if (!this.inlineEditManager) return;
        const row = button.closest('tr');
        if (row) this.inlineEditManager.toggleRowEdit(row);
    }

    setButtonLoading(button, loading) {
        button.disabled = loading;
        button.style.opacity = loading ? '0.5' : '1';
    }
}
