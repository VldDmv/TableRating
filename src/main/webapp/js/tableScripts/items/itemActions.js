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
               console.error("ItemActionsManager: Missing tableBody or selectors configuration.");
               return;
           }

           const statusButtonSelector = this.config.selectors.statusButtonClass;
           const deleteButtonSelector = this.config.selectors.deleteIconButtonClass;
           const editButtonSelector = this.config.selectors.editIconButtonClass;

           if (!statusButtonSelector || !deleteButtonSelector || !editButtonSelector) {
               console.error("ItemActionsManager: Critical selectors (status, delete, edit) are undefined in config!");
               return;
           }

           this.tableBody.addEventListener('click', (event) => {
               const target = event.target;
               const button = target.closest('button');
               if (!button) return;

               // Check what action button was clicked
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
     * @param {HTMLButtonElement} button The status button that was clicked.
     */
    async handleToggleStatus(button) {
        const itemName = button.dataset.itemName;
        if (!itemName) {
            console.error("Item name not found in data-item-name attribute.");
            return;
        }

        // Prevent action if the row is currently being edited
       const row = button.closest('tr');
               if (!row) {
                    console.error("Could not find parent <tr> for status button.");
                    return;
               }
               if (row.classList.contains('is-editing')) {
                   alert("Please save or cancel your edits before changing the status.");
                   return;
               }

        const csrfToken = securityUtils.getCsrfToken();
        if (!csrfToken) {
            alert("Security token missing. Please refresh the page.");
            return;
        }

        const originalContent = button.innerHTML;
        this.setButtonLoading(button, true);

        try {
            const formData = new URLSearchParams();
            formData.append(this.config.paramNames.toggleItemStatus, itemName);
            formData.append(this.config.csrfParameterName, csrfToken);

            const response = await fetch(this.config.entityType, {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: formData.toString()
            });

            if (!response.ok) {
                const errorMsg = await ErrorHandler.parseErrorResponse(response);
                throw new Error(errorMsg);
            }

            const data = await response.json();

                     if (data.success && data.data && typeof data.data.newStatus !== 'undefined') {
                         button.innerHTML = data.data.newStatus ? ICONS.COMPLETED : ICONS.NOT_COMPLETED;
                     } else {
                         throw new Error(data.message || "Invalid server response.");
                     }
        } catch (error) {
            ErrorHandler.handle(error, "Error toggling item status");
            button.innerHTML = originalContent;
        } finally {
            this.setButtonLoading(button, false);
        }
    }

    /**
     * Shows a confirmation dialog and submits the parent form if confirmed.
     * @param {HTMLButtonElement} button The delete button (icon) that was clicked.
     */

       async handleDeleteConfirmation(button) {
           const itemName = button.dataset.itemName;
           if (!itemName) return;

           const row = button.closest('tr');

           if (row && row.classList.contains('is-editing')) {
               alert("Please save or cancel your edits before deleting.");
               return;
           }

           const confirmMessage = `Are you sure you want to delete "${itemName}" (${this.config.entityNameSingular})?`;

           if (!confirm(confirmMessage)) {
               return;
           }

           const originalContent = button.innerHTML;
           this.setButtonLoading(button, true);

           const csrfToken = securityUtils.getCsrfToken();
           if (!csrfToken) {
               alert("Security token missing.");
               this.setButtonLoading(button, false);
               return;
           }

           try {

               const formData = new URLSearchParams();

               formData.append(this.config.paramNames.removeItem, itemName);
               formData.append(this.config.csrfParameterName, csrfToken);

               const response = await fetch(this.config.entityType, {
                   method: "POST",
                   headers: { "Content-Type": "application/x-www-form-urlencoded" },
                   body: formData.toString()
               });

               if (!response.ok) {
                   const errorMsg = await ErrorHandler.parseErrorResponse(response);
                   throw new Error(errorMsg);
               }


               row.style.transition = 'opacity 0.3s';
               row.style.opacity = '0';
               setTimeout(() => row.remove(), 300);

           } catch (error) {
               ErrorHandler.handle(error, "Error deleting item");
               button.innerHTML = originalContent;
               this.setButtonLoading(button, false);
           }

       }
     /**
     * Handles edit button click.
     * @param {HTMLButtonElement} button The edit button that was clicked.
     */
    handleEditClick(button) {
        if (!this.inlineEditManager) {
            console.error("InlineEditManager is not available.");
            return;
        }

        const row = button.closest('tr');
        if (!row) {
            console.error("Could not find parent <tr> for edit button.");
            return;
        }

        this.inlineEditManager.toggleRowEdit(row);
    }
    /**
     * Sets button loading state (disables and fades it).
     * @param {HTMLElement} button - Button element.
     * @param {boolean} loading - Loading state.
     */
    setButtonLoading(button, loading) {
        button.disabled = loading;
        button.style.opacity = loading ? '0.5' : '1';
    }
}