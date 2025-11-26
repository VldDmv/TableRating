/**
 * Manages inline editing for category table rows, including tags/genres modal.
 * Refactored with improved separation of concerns and utility usage.
 */

import { htmlUtils, entityUtils, securityUtils, ICONS } from '../core/utils.js';
import { ErrorHandler } from '../core/errorHandler.js';

/**
 * Manages inline editing functionality for table rows.
 */
export class InlineEditManager {
    /**
     * Creates a new InlineEditManager instance.
     * @param {HTMLElement} tableBody - Table body element.
     * @param {Object} config - Configuration object.
     */
    constructor(tableBody, config) {
        this.tableBody = tableBody;
        this.config = config;
        this.currentEditRow = null;
    }

    /**
     * Initializes the manager.
     */
    init() {
        if (!this.tableBody) {
            console.error("InlineEditManager: Missing required tableBody.");
            return;
        }
        this.setupModalHandlers();
    }

    /**
     * Gets available items (tags or genres) for current entity type.
     * @returns {Array} Array of available items.
     */
    getAvailableItems() {
        return entityUtils.getAvailableItems(this.config.entityType);
    }

    /**
     * Toggles a single row between View and Edit mode.
     * If the row is already in edit mode, it attempts to save it.
     * @param {HTMLElement} row - The <tr> element to toggle.
     */
    toggleRowEdit(row) {
        const isEditing = row.classList.contains('is-editing');

        if (isEditing) {
            this.saveRow(row);
        } else {

            if (this.currentEditRow && this.currentEditRow !== row) {
                this.switchToViewRow(this.currentEditRow);
            }
            this.currentEditRow = row;
            this.switchToEditRow(row);
        }
    }

    /**
     * Sets up modal event handlers.
     */
    setupModalHandlers() {
        const saveButton = document.getElementById('modal-save-tags');
        const cancelButton = document.getElementById('modal-cancel-tags');
        const closeButton = document.querySelector('.modal .close');

        if (saveButton) {
            saveButton.onclick = () => this.saveTagsFromModal();
        }
        if (cancelButton) {
            cancelButton.onclick = () => this.closeTagsModal();
        }
        if (closeButton) {
            closeButton.onclick = () => this.closeTagsModal();
        }
    }

    /**
     * Switches a row to edit mode.
     * @param {HTMLElement} row - Table row element.
     */
    switchToEditRow(row) {
        row.classList.add('is-editing');

        const nameCell = row.children[this.config.columns.name.index];
        const scoreCell = row.children[this.config.columns.score.index];
        const editButton = row.querySelector(this.config.selectors.editIconButtonClass);

        // Store original values
        this.storeOriginalValues(row, nameCell, scoreCell);

        // Create input fields
        this.createNameInput(row, nameCell);
        this.createScoreInput(row, scoreCell);

        // Update tags display (adds "Edit Tags" button)
        this.updateTagsInRow(row, true);

        // Change icon to "Save"
        if (editButton) {
            editButton.innerHTML = ICONS.SAVE || '💾';
            editButton.title = `Save ${this.config.entityNameSingular}`;
        }
    }

    /**
     * Stores original values in row dataset.
     * @param {HTMLElement} row - Table row.
     * @param {HTMLElement} nameCell - Name cell.
     * @param {HTMLElement} scoreCell - Score cell.
     */
    storeOriginalValues(row, nameCell, scoreCell) {
        if (row.dataset.originalName === undefined) {
            row.dataset.originalName = nameCell.textContent.trim();
        }
        if (row.dataset.originalScore === undefined) {
            const scoreCellElement = scoreCell.querySelector('.score-cell');
            row.dataset.originalScore = scoreCellElement ?
                scoreCellElement.textContent.trim() : '';
        }
        if (row.dataset.initialTagIds === undefined) {
            const initialTagIds = Array.from(row.querySelectorAll('.tag-badge'))
                .map(badge => badge.dataset.tagId)
                .join(',');
            row.dataset.initialTagIds = initialTagIds;
            row.dataset.originalTagIds = initialTagIds;
        }
    }

    /**
     * Creates name input field.
     * @param {HTMLElement} row - Table row.
     * @param {HTMLElement} nameCell - Name cell.
     */
    createNameInput(row, nameCell) {
        if (nameCell.querySelector("input")) return;

        const nameInput = document.createElement("input");
        nameInput.type = "text";
        nameInput.value = row.dataset.originalName;
        nameInput.className = "edit-name-input";
        nameCell.textContent = "";
        nameCell.appendChild(nameInput);
    }

    /**
     * Creates score input field.
     * @param {HTMLElement} row - Table row.
     * @param {HTMLElement} scoreCell - Score cell.
     */
    createScoreInput(row, scoreCell) {
        if (scoreCell.querySelector("input")) return;

        const scoreInput = document.createElement("input");
        scoreInput.type = "number";
        scoreInput.min = "1";
        scoreInput.max = "100";
        scoreInput.value = row.dataset.originalScore;
        scoreInput.className = "edit-score-input";
        scoreCell.textContent = "";
        scoreCell.appendChild(scoreInput);
    }

    /**
     * Switches a row back to view mode, restoring original values.
     * @param {HTMLElement} row - Table row element.
     */
    switchToViewRow(row) {
        row.classList.remove('is-editing');

        const nameCell = row.children[this.config.columns.name.index];
        const scoreCell = row.children[this.config.columns.score.index];
        const editButton = row.querySelector(this.config.selectors.editIconButtonClass);

        const newName = row.dataset.originalName;

        nameCell.textContent = newName;
        scoreCell.innerHTML = `<span class="score-cell">${row.dataset.originalScore}</span>`;

        // Update button attributes
        this.updateRowButtons(row, newName);

        // Update tags display (removes "Edit Tags" button)
        this.updateTagsInRow(row, false);

        // Re-apply score styling to the restored cell
        if (typeof this.config.applyScoreStyling === 'function') {
            this.config.applyScoreStyling(this.tableBody);
        }

        // Change icon back to "Edit"
        if (editButton) {
            editButton.innerHTML = ICONS.EDIT || '✏️';
            editButton.title = `Edit ${this.config.entityNameSingular}`;
        }

        if (this.currentEditRow === row) {
            this.currentEditRow = null;
        }
    }

    /**
     * Updates button attributes in a row.
     * @param {HTMLElement} row - Table row.
     * @param {string} newName - New item name.
     */
    updateRowButtons(row, newName) {
        const statusButton = row.querySelector(this.config.selectors.statusButtonClass);
        if (statusButton) {
            statusButton.setAttribute('data-item-name', newName);
        }

        const deleteButton = row.querySelector(this.config.selectors.deleteButtonClass);
        if (deleteButton) {
            deleteButton.setAttribute('data-item-name', newName);
        }

        const removeItemInput = row.querySelector(
            `input[name="${this.config.paramNames.removeItem}"]`
        );
        if (removeItemInput) {
            removeItemInput.value = newName;
        }
    }

    /**
     * Updates tags/genres display in a row.
     * @param {HTMLElement} row - Table row.
     * @param {boolean} isEditMode - Whether the row is in edit mode.
     */
    updateTagsInRow(row, isEditMode) {
        const tagsColumnConfig = this.config.columns.tags || this.config.columns.genres;
        if (!tagsColumnConfig) return;

        const tagsCell = row.children[tagsColumnConfig.index];
        const tagIds = (row.dataset.originalTagIds || '').split(',').filter(Boolean);

        tagsCell.innerHTML = '';

        const tagsContainer = document.createElement('div');
        tagsContainer.className = 'tags-container';

        // Create tag badges
        if (tagIds.length > 0) {
            this.createTagBadges(tagIds, tagsContainer);
        }

        // Add edit button in edit mode
        if (isEditMode) {
            this.createEditTagsButton(row, tagsContainer);
        }

        tagsCell.appendChild(tagsContainer);
    }

    /**
     * Creates tag badges.
     * @param {Array<string>} tagIds - Tag IDs.
     * @param {HTMLElement} container - Container element.
     */
    createTagBadges(tagIds, container) {
        const availableItems = this.getAvailableItems();
        const allItemsMap = new Map(
            availableItems.map(item => [String(item.id), item.name])
        );

        tagIds.forEach(id => {
            const badge = document.createElement('span');
            badge.className = 'tag-badge';
            badge.textContent = htmlUtils.decode(allItemsMap.get(id) || 'Unknown');
            badge.dataset.tagId = id;
            container.appendChild(badge);
        });
    }

    /**
     * Creates edit tags button.
     * @param {HTMLElement} row - Table row.
     * @param {HTMLElement} container - Container element.
     */
    createEditTagsButton(row, container) {
        const button = document.createElement('button');
        button.textContent = `Edit ${entityUtils.getItemTypeName(this.config.entityType)}`;
        button.className = 'edit-tags-button';
        button.onclick = () => this.openTagsModal(row);
        container.appendChild(button);
    }

    /**
     * Opens the tags/genres editing modal.
     * @param {HTMLElement} row - Table row being edited.
     */
    openTagsModal(row) {
        this.currentEditRow = row;
        const modal = document.getElementById('tags-edit-modal');
        const modalBody = modal.querySelector('.modal-body');

        modalBody.innerHTML = '';

        const currentTagIds = new Set(
            (row.dataset.originalTagIds || '').split(',').filter(Boolean)
        );
        const availableItems = this.getAvailableItems();

        if (availableItems.length === 0) {
            modalBody.textContent = 'List of available items not found. Please check JSP variables.';
            modal.style.display = 'block';
            return;
        }

        // Create checkboxes
        availableItems.forEach(item => {
            const label = this.createTagCheckbox(item, currentTagIds);
            modalBody.appendChild(label);
        });

        // Add clear button
        const clearButton = this.createClearTagsButton(modalBody);
        modalBody.appendChild(clearButton);

        modal.style.display = 'block';
    }

    /**
     * Creates a checkbox for a tag/genre.
     * @param {Object} item - Tag/genre item.
     * @param {Set<string>} currentTagIds - Currently selected tag IDs.
     * @returns {HTMLElement} Label element.
     */
    createTagCheckbox(item, currentTagIds) {
        const label = document.createElement('label');
        label.className = 'tag-checkbox-label';

        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.value = item.id;
        checkbox.checked = currentTagIds.has(String(item.id));

        label.appendChild(checkbox);
        label.appendChild(document.createTextNode(` ${htmlUtils.decode(item.name)}`));

        return label;
    }

    /**
     * Creates clear tags button.
     * @param {HTMLElement} modalBody - Modal body element.
     * @returns {HTMLElement} Button element.
     */
    createClearTagsButton(modalBody) {
        const clearButton = document.createElement('button');
        clearButton.textContent = `Clear ${entityUtils.getItemTypeName(this.config.entityType)}`;
        clearButton.className = 'clear-tags-button';
        clearButton.onclick = () => {
            modalBody.querySelectorAll('input[type="checkbox"]').forEach(cb => {
                cb.checked = false;
            });
        };
        return clearButton;
    }

    /**
     * Saves selected tags from modal.
     */
    saveTagsFromModal() {
        if (!this.currentEditRow) return;

        const modal = document.getElementById('tags-edit-modal');
        const selectedCheckboxes = modal.querySelectorAll('input[type="checkbox"]:checked');
        const newTagIds = Array.from(selectedCheckboxes).map(cb => cb.value);

        this.currentEditRow.dataset.originalTagIds = newTagIds.join(',');
        this.updateTagsInRow(this.currentEditRow, true);

        this.closeTagsModal();
    }

    /**
     * Closes the tags modal.
     */
    closeTagsModal() {
        const modal = document.getElementById('tags-edit-modal');
        if (modal) {
            modal.style.display = 'none';
        }

    }

    /**
     * Detects if a row has changes.
     * @param {HTMLElement} row - Table row.
     * @param {string} newName - New name.
     * @param {string} newScore - New score.
     * @param {Array<string>} newTagIds - New tag IDs.
     * @returns {boolean} True if changes detected.
     */
    detectChanges(row, newName, newScore, newTagIds) {
        const nameChanged = newName !== row.dataset.originalName;
        const scoreChanged = newScore !== row.dataset.originalScore;
        const tagsChanged = newTagIds.join(',') !== row.dataset.initialTagIds;

        return nameChanged || scoreChanged || tagsChanged;
    }

    /**
     * Saves a single row.
     * @param {HTMLElement} row - Table row.
     */
    async saveRow(row) {
        const csrfToken = securityUtils.getCsrfToken();
        if (!csrfToken) {
            alert("Security token missing. Please refresh the page.");
            return;
        }

        const nameInput = row.children[this.config.columns.name.index]?.querySelector("input");
        const scoreInput = row.children[this.config.columns.score.index]?.querySelector("input");
        const oldName = row.dataset.originalName;

        if (!nameInput || !scoreInput || oldName === undefined) {
            return;
        }

        const newName = nameInput.value.trim();
        const newScore = scoreInput.value.trim();
        const newTagIds = (row.dataset.originalTagIds || '').split(',').filter(Boolean);

        // Validate score
        if (!this.config.validateScore(newScore)) {
            scoreInput.focus();
            return;
        }

        const hasChanges = this.detectChanges(row, newName, newScore, newTagIds);

        if (hasChanges) {
            const formData = this.buildSaveFormData(oldName, newName, newScore, newTagIds, csrfToken);

            try {
                const response = await fetch(this.config.entityType, {
                    method: "POST",
                    headers: { "Content-Type": "application/x-www-form-urlencoded" },
                    body: formData.toString()
                });

                if (!response.ok) {
                    const errorMessage = await ErrorHandler.parseErrorResponse(response);
                    throw new Error(errorMessage);
                }

                const data = await response.json();
                if (data.success) {
                    this.updateRowData(row, newName, newScore, newTagIds);
                    this.switchToViewRow(row);
                } else {
                    throw new Error(data.message || 'Server error');
                }
            } catch (error) {
                console.error("Failed to save row:", error);
                alert(`Error saving: ${error.message}`);
                row.classList.add('save-error');
            }
        } else {
            this.switchToViewRow(row);
        }
    }

    /**
     * Builds form data for saving.
     * @param {string} oldName - Old name.
     * @param {string} newName - New name.
     * @param {string} newScore - New score.
     * @param {Array<string>} newTagIds - New tag IDs.
     * @param {string} csrfToken - CSRF token.
     * @returns {URLSearchParams} Form data.
     */
    buildSaveFormData(oldName, newName, newScore, newTagIds, csrfToken) {
        const formData = new URLSearchParams();
        formData.append(this.config.paramNames.oldItemName, oldName);
        formData.append(this.config.paramNames.updatedItemName, newName);
        formData.append(this.config.paramNames.updatedItemScore, newScore);

        newTagIds.forEach(tagId => {
            formData.append(this.config.paramNames.updatedItemTagIds, tagId);
        });

        formData.append(this.config.csrfParameterName, csrfToken);

        return formData;
    }

    /**
     * Updates row data after successful save.
     * @param {HTMLElement} row - Table row.
     * @param {string} newName - New name.
     * @param {string} newScore - New score.
     * @param {Array<string>} newTagIds - New tag IDs.
     */
    updateRowData(row, newName, newScore, newTagIds) {
        row.dataset.originalName = newName;
        row.dataset.originalScore = newScore;
        row.dataset.initialTagIds = newTagIds.join(',');
        row.dataset.originalTagIds = newTagIds.join(',');
    }
}
