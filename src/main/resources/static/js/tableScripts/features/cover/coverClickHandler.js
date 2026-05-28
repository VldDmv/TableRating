/**
 * Cover Click Handler
 * Handles Ctrl+Click for quick cover edit and modal edit button.
 */

import { securityUtils } from '../../core/utils.js';

export class CoverClickHandler {
    /**
     * @param {HTMLElement} tableBody
     * @param {Object} config
     * @param {Function} onCoverUpdated — callback called after the cover is successfully updated
     */
    constructor(tableBody, config, onCoverUpdated) {
        this.tableBody = tableBody;
        this.config = config;
        this.onCoverUpdated = onCoverUpdated;

        if (typeof onCoverUpdated !== 'function') {
            console.warn(
                '[CoverClickHandler] onCoverUpdated not passed - will hard reload after update'
            );
        }
    }

    init() {
        this.tableBody.addEventListener('click', (e) => {
            const coverImg = e.target.closest('.cover-thumbnail');
            const coverPlaceholder = e.target.closest('.cover-placeholder');

            if (!coverImg && !coverPlaceholder) return;

            const row = e.target.closest('tr');
            if (!row) return;

            const isCtrlClick = e.ctrlKey || e.metaKey;

            if (isCtrlClick) {
                e.preventDefault();
                e.stopPropagation();
                this.handleCoverEdit(row);
            } else if (coverPlaceholder) {
                e.stopPropagation();
                this.handleCoverEdit(row);
            }
        });

        document.addEventListener('coverEditRequested', (e) => {
            const { itemName, currentUrl } = e.detail;
            this.handleCoverEditByName(itemName, currentUrl);
        });
    }

    // ─── Edit flow ────────────────────────────────────────────────────────────

    async handleCoverEdit(row) {
        await this._showEditPrompt(row.dataset.originalName, row.dataset.originalCoverUrl || '');
    }

    async handleCoverEditByName(itemName, currentUrl) {
        await this._showEditPrompt(itemName, currentUrl);
    }

    async _showEditPrompt(itemName, currentUrl) {
        const newUrl = prompt(
            `Edit cover URL for "${itemName}":\n(Leave empty to remove cover)`,
            currentUrl
        );

        if (newUrl === null) return;

        const trimmed = newUrl.trim();

        if (trimmed !== '' && !this._isValidUrl(trimmed)) {
            alert('Invalid URL format. Please enter a valid URL.');
            return;
        }

        await this._updateCover(itemName, trimmed);
    }

    // ─── API ──────────────────────────────────────────────────────────────────

    async _updateCover(itemName, coverUrl) {
        const csrfToken = securityUtils.getCsrfToken();

        if (!csrfToken) {
            alert('Security token missing. Please refresh the page.');
            return;
        }

        try {
            const response = await fetch(
                `/api/${this.config.entityType}/${encodeURIComponent(itemName)}/cover`,
                {
                    method: 'PATCH',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-XSRF-TOKEN': csrfToken,
                    },
                    body: JSON.stringify({ coverUrl }),
                }
            );

            if (!response.ok) {
                throw new Error((await response.text()) || 'Failed to update cover');
            }

            if (typeof this.onCoverUpdated === 'function') {
                await this.onCoverUpdated();
            } else {
                location.reload();
            }
        } catch (error) {
            console.error('[CoverClickHandler] Cover update error:', error);
            alert(`Failed to update cover: ${error.message}`);
        }
    }

    // ─── Utils ────────────────────────────────────────────────────────────────

    _isValidUrl(url) {
        try {
            new URL(url);
            return true;
        } catch {
            return false;
        }
    }
}
