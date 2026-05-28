/**
 * Card Inline Edit Manager
 * Manages inline editing for cards - similar to table row inline editing
 */

import { htmlUtils, entityUtils, securityUtils, ICONS } from '../../core/utils.js';
import { ErrorHandler } from '../../core/errorHandler.js';

export class CardInlineEditManager {
    constructor(cardsContainer, config) {
        this.cardsContainer = cardsContainer;
        this.config = config;
        this.currentEditCard = null;
    }

    init() {
        if (!this.cardsContainer) {
            console.error('[CardInlineEditManager] Missing cards container');
            return;
        }
    }

    /**
     * Get available tags/genres
     */
    getAvailableItems() {
        return entityUtils.getAvailableItems(this.config.entityType);
    }

    /**
     * Toggle card between view and edit mode
     */
    toggleCardEdit(card) {
        const isEditing = card.classList.contains('is-editing');

        if (isEditing) {
            this.saveCard(card);
        } else {
            // Switch previous card to view mode
            if (this.currentEditCard && this.currentEditCard !== card) {
                this.switchToViewMode(this.currentEditCard);
            }
            this.currentEditCard = card;
            this.switchToEditMode(card);
        }
    }

    /**
     * Switch card to edit mode
     */
    switchToEditMode(card) {
        card.classList.add('is-editing');

        const cardContent = card.querySelector('.card-content');
        if (!cardContent) return;

        // Get original values
        const originalName = card.dataset.originalName;
        const originalScore = card.dataset.originalScore;
        const originalTagIds = card.dataset.originalTagIds || '';
        const originalCoverUrl = card.dataset.originalCoverUrl || '';

        // Replace content with edit form
        cardContent.innerHTML = `
            <div class="card-edit-form">
                <div class="card-edit-field">
                    <label>Name:</label>
                    <input type="text"
                           class="edit-name-input"
                           value="${htmlUtils.escape(originalName)}">
                </div>

                <div class="card-edit-field">
                    <label>Score:</label>
                    <input type="number"
                           class="edit-score-input"
                           value="${originalScore}"
                           min="1"
                           max="100"
                           placeholder="1-100">
                </div>

                <div class="card-edit-field">
                    <label>Cover:</label>
                    <input type="url"
                           class="edit-cover-url-input"
                           value="${htmlUtils.escape(originalCoverUrl)}"
                           placeholder="https://...">
                </div>

                <div class="card-edit-field tags-field">
                    <label>${this.config.entityType === 'games' ? 'Tags:' : 'Genres:'}</label>
                    <div class="edit-tags-display">
                        ${this.renderTagsEditDisplay(card)}
                    </div>
                    <button type="button" class="edit-tags-btn">
                        ${ICONS.EDIT || '✏️'} Edit
                    </button>
                </div>

                <div class="card-edit-actions">
                    <button type="button" class="save-edit-btn">
                        ${ICONS.SAVE || '💾'} Save
                    </button>
                    <button type="button" class="cancel-edit-btn">
                        ${ICONS.CANCEL || '❌'} Cancel
                    </button>
                </div>
            </div>
        `;

        // Attach event listeners
        const editTagsBtn = cardContent.querySelector('.edit-tags-btn');
        const saveBtn = cardContent.querySelector('.save-edit-btn');
        const cancelBtn = cardContent.querySelector('.cancel-edit-btn');

        if (editTagsBtn) {
            editTagsBtn.addEventListener('click', () => this.openTagsModal(card));
        }

        if (saveBtn) {
            saveBtn.addEventListener('click', () => this.saveCard(card));
        }

        if (cancelBtn) {
            cancelBtn.addEventListener('click', () => this.switchToViewMode(card));
        }
    }

    /**
     * Render tags/genres for edit display
     */
    renderTagsEditDisplay(card) {
        const originalTagIds = (card.dataset.originalTagIds || '').split(',').filter(Boolean);

        if (originalTagIds.length === 0) {
            return '<span class="no-tags-text">No tags selected</span>';
        }

        const availableItems = this.getAvailableItems();
        const selectedItems = availableItems.filter((item) =>
            originalTagIds.includes(String(item.id))
        );

        return selectedItems
            .map((item) => `<span class="tag-badge">${htmlUtils.escape(item.name)}</span>`)
            .join(' ');
    }

    /**
     * Switch card back to view mode
     */
    switchToViewMode(card) {
        card.classList.remove('is-editing');

        const originalName = card.dataset.originalName;
        const originalScore = card.dataset.originalScore;
        const originalTagIds = (card.dataset.originalTagIds || '').split(',').filter(Boolean);
        const originalCoverUrl = card.dataset.originalCoverUrl || '';

        const cardContent = card.querySelector('.card-content');
        if (!cardContent) return;

        // Get tags for display
        const availableItems = this.getAvailableItems();
        const selectedItems = availableItems.filter((item) =>
            originalTagIds.includes(String(item.id))
        );

        const tagsHtml =
            selectedItems.length > 0
                ? selectedItems
                      .map(
                          (item) =>
                              `<span class="tag-badge" data-tag-id="${item.id}">${htmlUtils.escape(item.name)}</span>`
                      )
                      .join(' ')
                : '<span class="card-no-tags">No tags</span>';

        // Get completed status from card dataset (or assume false)
        const completed = card.dataset.completed === 'true';
        const completedIcon = completed ? ICONS.COMPLETED || '✓' : ICONS.NOT_COMPLETED || '✗';

        // Restore view mode content
        cardContent.innerHTML = `
            <h3 class="card-title">${htmlUtils.escape(originalName)}</h3>

            <div class="card-score">
                <span class="score-cell">${originalScore}</span>
            </div>

            <div class="card-tags">
                ${tagsHtml}
            </div>

            ${
                this.config.hideActions
                    ? ''
                    : `
            <div class="card-status">
                <button class="status-button" data-item-name="${htmlUtils.escape(originalName)}">
                    ${completedIcon}
                </button>
                <span class="status-label">
                    ${completed ? 'Completed' : 'In Progress'}
                </span>
            </div>

            <div class="card-actions">
                <button class="edit-button card-action-btn"
                        data-action="edit"
                        data-item-name="${htmlUtils.escape(originalName)}">
                    ${ICONS.EDIT || '✏️'} Edit
                </button>
                <button class="delete-button card-action-btn"
                        data-item-name="${htmlUtils.escape(originalName)}">
                    ${ICONS.DELETE || '🗑️'} Delete
                </button>
            </div>
            `
            }
        `;

        // Update cover if changed
        const coverDiv = card.querySelector('.card-cover');
        if (coverDiv) {
            if (originalCoverUrl) {
                coverDiv.innerHTML = `
                    <img src="${originalCoverUrl}"
                         alt="${htmlUtils.escape(originalName)}"
                         class="card-cover-image"
                         title="Click to view | Ctrl+Click to edit">
                `;
            } else {
                coverDiv.innerHTML = `
                    <div class="card-cover-placeholder" title="Click to add cover">
                        📦
                    </div>
                `;
            }
        }

        // Re-apply score styling
        if (typeof this.config.applyScoreStyling === 'function') {
            this.config.applyScoreStyling(this.cardsContainer);
        }

        if (this.currentEditCard === card) {
            this.currentEditCard = null;
        }
    }

    /**
     * Save card changes
     */
    async saveCard(card) {
        const cardContent = card.querySelector('.card-content');
        if (!cardContent) return;

        const nameInput = cardContent.querySelector('.edit-name-input');
        const scoreInput = cardContent.querySelector('.edit-score-input');
        const coverInput = cardContent.querySelector('.edit-cover-url-input');

        if (!nameInput || !scoreInput) {
            alert('Missing input fields');
            return;
        }

        const newName = nameInput.value.trim();
        const newScore = parseInt(scoreInput.value, 10);
        const newCoverUrl = coverInput ? coverInput.value.trim() : '';

        // Validation
        if (!newName) {
            alert('Name cannot be empty');
            nameInput.focus();
            return;
        }

        if (isNaN(newScore) || newScore < 1 || newScore > 100) {
            alert('Score must be between 1 and 100');
            scoreInput.focus();
            return;
        }

        // Get tag IDs
        const newTagIds = card.dataset.originalTagIds.split(',').filter(Boolean).map(Number);

        // Prepare data
        const originalName = card.dataset.originalName;
        const updateData = {
            name: newName,
            score: newScore,
            coverUrl: newCoverUrl,
            [`${this.config.entityType === 'games' ? 'tagIds' : 'genreIds'}`]: newTagIds,
        };

        try {
            const csrfToken = securityUtils.getCsrfToken();
            const response = await fetch(
                `/api/${this.config.entityType}/${encodeURIComponent(originalName)}`,
                {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-XSRF-TOKEN': csrfToken,
                    },
                    body: JSON.stringify(updateData),
                }
            );

            if (!response.ok) {
                const errorMsg = await ErrorHandler.parseErrorResponse(response);
                throw new Error(errorMsg);
            }

            //  Update card dataset with all new values
            card.dataset.originalName = newName;
            card.dataset.originalScore = newScore;
            card.dataset.originalCoverUrl = newCoverUrl;
            card.dataset.originalTagIds = newTagIds.join(',');
            card.dataset.initialTagIds = newTagIds.join(',');

            //  Sync to OTHER view (table)
            if (typeof window.syncItemUpdate === 'function') {
                window.syncItemUpdate(originalName, {
                    name: newName,
                    score: newScore,
                    coverUrl: newCoverUrl,
                    tagIds: newTagIds,
                });
            }

            this.switchToViewMode(card);
        } catch (error) {
            ErrorHandler.handle(error, 'Error saving changes');
        }
    }

    /**
     * Open tags/genres modal for editing
     */
    openTagsModal(card) {
        const modal = document.getElementById('tags-edit-modal');
        if (!modal) {
            console.error('[CardInlineEditManager] Tags modal not found');
            return;
        }

        const saveBtn = document.getElementById('modal-save-tags');
        const cancelBtn = document.getElementById('modal-cancel-tags');
        const closeBtn = modal.querySelector('.close-btn');
        if (saveBtn) saveBtn.onclick = () => this.saveTagsFromModal();
        if (cancelBtn) cancelBtn.onclick = () => this.closeTagsModal();
        if (closeBtn) closeBtn.onclick = () => this.closeTagsModal();

        const modalBody = modal.querySelector('.modal-body');
        const modalCategoryName = document.getElementById('modal-category-name');

        if (modalCategoryName) {
            modalCategoryName.textContent = this.config.entityType === 'games' ? 'Tags' : 'Genres';
        }

        if (modalBody) {
            const currentTagIds = (card.dataset.originalTagIds || '').split(',').filter(Boolean);
            const availableItems = this.getAvailableItems();

            modalBody.innerHTML = availableItems
                .map((item) => {
                    const isChecked = currentTagIds.includes(String(item.id));
                    return `
                    <label class="modal-tag-label">
                        <input type="checkbox"
                               value="${item.id}"
                               ${isChecked ? 'checked' : ''}>
                        ${htmlUtils.escape(item.name)}
                    </label>
                `;
                })
                .join('');
        }

        // Store reference to card for saving later
        this.currentModalCard = card;
        modal.style.display = 'block';
    }

    /**
     * Save tags from modal
     */
    saveTagsFromModal() {
        if (!this.currentModalCard) return;

        const modal = document.getElementById('tags-edit-modal');
        const checkboxes = modal.querySelectorAll('input[type="checkbox"]:checked');
        const selectedIds = Array.from(checkboxes).map((cb) => cb.value);

        // Update card dataset
        this.currentModalCard.dataset.originalTagIds = selectedIds.join(',');

        // Update display in edit form
        const editTagsDisplay = this.currentModalCard.querySelector('.edit-tags-display');
        if (editTagsDisplay) {
            editTagsDisplay.innerHTML = this.renderTagsEditDisplay(this.currentModalCard);
        }

        this.closeTagsModal();
    }

    /**
     * Close tags modal
     */
    closeTagsModal() {
        const modal = document.getElementById('tags-edit-modal');
        if (modal) {
            modal.style.display = 'none';
        }
        this.currentModalCard = null;
    }
}
