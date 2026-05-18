/**
 * Table Renderer
 * Renders data as an HTML table.
 */

import { htmlUtils, ICONS } from '../../core/utils.js';

export class TableRenderer {
    /**
     * @param {Object}      config
     * @param {HTMLElement} tableBody
     * @param {Object|null} coverModal
     */
    constructor(config, tableBody, coverModal = null) {
        this.config     = config;
        this.tableBody  = tableBody;
        this.coverModal = coverModal;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    render(items) {
        this.tableBody.innerHTML = '';

        if (!items || items.length === 0) {
            this.renderEmpty();
            return;
        }

        const fragment = document.createDocumentFragment();
        items.forEach(item => fragment.appendChild(this.createRow(item)));
        this.tableBody.appendChild(fragment);

        this.config.applyScoreStyling?.(this.tableBody);
    }

    renderEmpty() {
        this.tableBody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align:center; padding:20px; color:#666;">
                    No items found matching your criteria.
                </td>
            </tr>
        `;
    }

    renderLoading() {
        this.tableBody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align:center; padding:20px;">
                    Loading...
                </td>
            </tr>
        `;
    }

    // ─── Row building ─────────────────────────────────────────────────────────

    createRow(item) {
        const row = document.createElement('tr');

        const tagsOrGenres = item.tags || item.genres || [];

        row.dataset.originalName     = item.name;
        row.dataset.originalScore    = item.score;
        row.dataset.originalTagIds   = tagsOrGenres.map(t => t.id).join(',');
        row.dataset.initialTagIds    = row.dataset.originalTagIds;
        row.dataset.originalCoverUrl = item.coverUrl || '';

        const escapedName   = htmlUtils.escape(item.name);
        const completedIcon = item.completed ? ICONS.COMPLETED    : ICONS.NOT_COMPLETED;
        const editIcon      = ICONS.EDIT      ?? '✏️';
        const deleteIcon    = ICONS.DELETE    ?? '🗑️';

        row.appendChild(this._createCoverCell(item));
        row.appendChild(this._createTextCell('col-name',  item.name));
        row.appendChild(this._createScoreCell(item.score));
        row.appendChild(this._createTagsCell(tagsOrGenres));
        row.appendChild(this._createCompletedCell(escapedName, completedIcon));
        row.appendChild(this._createStatusCell(item));
        row.appendChild(this._createActionsCell(escapedName, editIcon, deleteIcon));

        return row;
    }

    // ─── Cells ────────────────────────────────────────────────────────────────

    _createCoverCell(item) {
        const cell = document.createElement('td');
        cell.className = 'col-cover';

        if (item.coverUrl) {
            const img = document.createElement('img');
            img.src       = item.coverUrl;
            img.alt       = item.name;
            img.className = 'cover-thumbnail';
            img.title     = 'Click to view | Ctrl+Click to edit';

            img.addEventListener('click', (e) => {
                if (!e.ctrlKey && !e.metaKey) {
                    e.stopPropagation();
                    this.coverModal?.show(item.coverUrl, item.name);
                }
            });

            cell.appendChild(img);
        } else {
            const placeholder = document.createElement('div');
            placeholder.className   = 'cover-placeholder';
            placeholder.textContent = '📦';
            placeholder.title       = 'Click to add cover';
            cell.appendChild(placeholder);
        }

        return cell;
    }

    _createTextCell(className, text) {
        const cell = document.createElement('td');
        cell.className   = className;
        cell.textContent = text;
        return cell;
    }

    _createScoreCell(score) {
        const cell = document.createElement('td');
        cell.className = 'col-score';
        cell.innerHTML = `<span class="score-cell">${score}</span>`;
        return cell;
    }

    _createTagsCell(tags) {
        const cell = document.createElement('td');
        cell.className = this.config.entityType === 'games' ? 'col-tags' : 'col-genres';
        cell.innerHTML  = this._createTagsHtml(tags) || '<span style="color:#999;">-</span>';
        return cell;
    }

    _createCompletedCell(escapedName, completedIcon) {
        const cell = document.createElement('td');
        cell.className = 'col-completed';
        cell.innerHTML = `
            <button class="status-button" data-item-name="${escapedName}">
                ${completedIcon}
            </button>
        `;
        return cell;
    }

    _createStatusCell(item) {
        const cell = document.createElement('td');
        cell.className = 'col-status';
        const current = item.status || 'NONE';
        const safeName = htmlUtils.escape(item.name);
        cell.innerHTML = `
            <select class="status-select status-${current.toLowerCase()}"
                    data-item-name="${safeName}">
                <option value="NONE">—</option>
                <option value="WISHLIST">Wishlist</option>
                <option value="BACKLOG">Backlog</option>
                <option value="DROPPED">Dropped</option>
            </select>
        `;
        cell.querySelector('select').value = current;
        return cell;
    }

    _createActionsCell(escapedName, editIcon, deleteIcon) {
        const cell = document.createElement('td');
        cell.className = 'col-actions';
        cell.innerHTML = `
            <div class="action-buttons-group">
                <button class="edit-button"   data-item-name="${escapedName}">${editIcon}</button>
                <button class="delete-button" data-item-name="${escapedName}">${deleteIcon}</button>
            </div>
        `;
        return cell;
    }

    _createTagsHtml(tags) {
        if (!tags || tags.length === 0) return '';

        const valid = tags.filter(t => t?.name);
        if (valid.length === 0) return '';

        return valid.map(tag => {
            const name = htmlUtils.decode(tag.name);
            return `<span class="tag-badge" data-tag-id="${tag.id}">${name}</span>`;
        }).join(' ');
    }
}