/**
 * Card Renderer — rendering data in the form of cards.
 */

import { htmlUtils, ICONS } from '../../core/utils.js';

export class CardRenderer {
  /**
  * @param {Object} config
  * @param {Object|null} coverModal — a CoverModal instance (or null if not needed)
  */
    constructor(config, coverModal = null) {
        this.config          = config;
        this.coverModal      = coverModal;
        this.cardsContainer  = null;
        this.onRenderCallback = null;
    }

    onRender(callback) {
        this.onRenderCallback = callback;
        return this;
    }

    // ─── Public API ────────────────────────────────────────────────────────

    render(items) {
        this.cardsContainer = document.getElementById('cards-container');

        if (!this.cardsContainer) {
            console.error('[CardRenderer] #cards-container not found');
            return;
        }

        this.cardsContainer.innerHTML = '';

        if (!items || items.length === 0) {
            this._renderEmpty();
            this.onRenderCallback?.(this.cardsContainer);
            return;
        }

        const fragment = document.createDocumentFragment();
        items.forEach(item => fragment.appendChild(this.createCard(item)));
        this.cardsContainer.appendChild(fragment);

        this.config.applyScoreStyling?.(this.cardsContainer);
        this.onRenderCallback?.(this.cardsContainer);

    }

    renderLoading() {
        if (!this.cardsContainer) return;
        this.cardsContainer.innerHTML = `
            <div class="cards-loading-state"><p>Loading...</p></div>
        `;
    }

    getContainer() {
        return this.cardsContainer;
    }

    // ─── Creating card ──────────────────────────────────────────────────

    createCard(item) {
        const card = document.createElement('div');
        card.className = 'media-card';

        const tagsOrGenres = item.tags || item.genres || [];

        card.dataset.originalName     = item.name;
        card.dataset.originalScore    = item.score;
        card.dataset.originalTagIds   = tagsOrGenres.map(t => t.id).join(',');
        card.dataset.initialTagIds    = card.dataset.originalTagIds;
        card.dataset.originalCoverUrl = item.coverUrl || '';
        card.dataset.completed        = item.completed ? 'true' : 'false';

        const escapedName   = htmlUtils.escape(item.name);
        const completedIcon = item.completed ? ICONS.COMPLETED  : ICONS.NOT_COMPLETED;
        const editIcon      = ICONS.EDIT     ?? '✏️';
        const deleteIcon    = ICONS.DELETE   ?? '🗑️';

        card.innerHTML = `
            <div class="card-cover">
                ${this._createCoverHtml(item)}
            </div>
            <div class="card-content">
                <h3 class="card-title">${htmlUtils.escape(item.name)}</h3>
                <div class="card-score">
                    <span class="score-cell">${item.score}</span>
                </div>
                <div class="card-tags">
                    ${this._createTagsHtml(tagsOrGenres)}
                </div>
                ${this.config.hideActions ? '' : `
                <div class="card-status">
                    <button class="status-button" data-item-name="${escapedName}">
                        ${completedIcon}
                    </button>
                    <span class="status-label">${item.completed ? 'Completed' : 'Not Completed'}</span>
                </div>
                <div class="card-actions">
                    <button class="edit-button card-action-btn"
                            data-action="edit"
                            data-item-name="${escapedName}">
                        ${editIcon} Edit
                    </button>
                    <button class="delete-button card-action-btn"
                            data-item-name="${escapedName}">
                        ${deleteIcon} Delete
                    </button>
                </div>
                `}
            </div>
        `;

        this._attachCoverClickListener(card, item);

        return card;
    }

    // ─── Private methods ─────────────────────────────────────────────────────

    _renderEmpty() {
        this.cardsContainer.innerHTML = `
            <div class="cards-empty-state">
                <p>No items found matching your criteria.</p>
            </div>
        `;
    }

    _createCoverHtml(item) {
        if (item.coverUrl) {
            return `
                <img src="${item.coverUrl}"
                     alt="${htmlUtils.escape(item.name)}"
                     class="card-cover-image"
                     title="Click to view | Ctrl+Click to edit">
            `;
        }
        return `<div class="card-cover-placeholder" title="Click to add cover">📦</div>`;
    }

    _createTagsHtml(tags) {
        if (!tags || tags.length === 0) {
            return '<span class="card-no-tags">No tags</span>';
        }
        return tags.map(tag => {
            const name = htmlUtils.decode(tag.name || 'Unknown');
            return `<span class="tag-badge" data-tag-id="${tag.id}">${name}</span>`;
        }).join(' ');
    }

  /**
  * Attaches a click handler to the card cover.
  * Ctrl+Click → coverClickHandler intercepts via delegation to the tbody.
  * Regular click → opens the view modal via the injected coverModal.
  */
    _attachCoverClickListener(card, item) {
        const coverEl = card.querySelector('.card-cover-image, .card-cover-placeholder');
        if (!coverEl) return;

        coverEl.addEventListener('click', (e) => {
            if (e.ctrlKey || e.metaKey) return;

            if (item.coverUrl) {
                e.stopPropagation();
                this.coverModal?.show(item.coverUrl, item.name);
            }
        });
    }
}