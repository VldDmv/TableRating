/**
 * Handles rendering of table content.
 */

import { htmlUtils, ICONS } from '../core/utils.js';

export class TableRenderer {
    /**
     * Creates a new TableRenderer instance.
     * @param {Object} config - Configuration object.
     * @param {HTMLElement} tableBody - Table body element.
     */
    constructor(config, tableBody) {
        this.config = config;
        this.tableBody = tableBody;
    }

    /**
     * Renders items in the table body.
     * @param {Array} items - Items to render.
     */
    render(items) {
        this.tableBody.innerHTML = '';

        if (!items || items.length === 0) {
            this.renderEmpty();
            return;
        }

     const fragment = document.createDocumentFragment();

         items.forEach(item => {
             const row = this.createRow(item);
             fragment.appendChild(row);
         });
         this.tableBody.appendChild(fragment);

         if (typeof this.config.applyScoreStyling === 'function') {
             this.config.applyScoreStyling(this.tableBody);
         }
    }

    /**
     * Renders empty state message.
     */
    renderEmpty() {
        this.tableBody.innerHTML = `
            <tr>
                <td colspan="5" style="text-align: center; padding: 20px; color: #666;">
                    No items found matching your criteria.
                </td>
            </tr>
        `;
    }

    /**
     * Renders loading state.
     */
    renderLoading() {
        this.tableBody.innerHTML = `
            <tr>
                <td colspan="5" style="text-align: center; padding: 20px;">
                    Loading...
                </td>
            </tr>
        `;
    }

    /**
     * Creates a table row for an item.
     * @param {Object} item - Item data.
     * @returns {HTMLElement} Table row element.
     */
    createRow(item) {
        const row = document.createElement('tr');
        const tagsOrGenres = item.tags || item.genres || [];
        const tagsHtml = this.createTagsHtml(tagsOrGenres);
        const csrfToken = document.querySelector('meta[name=_csrf_token]')?.content || '';
        const escapedItemName = htmlUtils.escape(item.name);

        const completedIcon = item.completed ? ICONS.COMPLETED : ICONS.NOT_COMPLETED;
        const editIcon = ICONS.EDIT || '✏️';
        const deleteIcon = ICONS.DELETE || '🗑️';

        row.innerHTML = `
            <td class="col-name">${escapedItemName}</td>
            <td class="col-score">
                <span class="score-cell">${item.score}</span>
            </td>
            <td class="col-tags">
                <div class="tags-container">${tagsHtml}</div>
            </td>
            <td class="col-completed">
                <button class="status-button" data-item-name="${escapedItemName}">
                    ${completedIcon}
                </button>
            </td>

            <td class="col-actions">
                <div class="action-buttons-group">
                    <button class="btn btn--icon btn--edit" title="Edit ${this.config.entityNameSingular}" data-item-name="${escapedItemName}">${editIcon}</button>

                    <form action="${this.config.entityType}" method="post" class="delete-form-icon">
                        <input type="hidden" name="${this.config.paramNames.removeItem}" value="${escapedItemName}">
                        <input type="hidden" name="_csrf" value="${csrfToken}">
                        <button type="button" class="btn btn--icon btn--danger btn--delete" title="Delete ${this.config.entityNameSingular}" data-item-name="${escapedItemName}">${deleteIcon}</button>
                    </form>
                </div>
            </td>
        `;

        return row;
    }

    /**
     * Creates HTML for tags/genres badges.
     * @param {Array} tags - Array of tag/genre objects.
     * @returns {string} HTML string.
     */
    createTagsHtml(tags) {
        if (!tags || tags.length === 0) return '';

        return tags
            .map(tag => `<span class="tag-badge" data-tag-id="${tag.id}">${htmlUtils.escape(tag.name)}</span>`)
            .join(' ');
    }
}