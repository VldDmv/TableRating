/**
 * Manages table sorting functionality for category pages.
 */

export class SortManager {
    /**
     * Creates a new SortManager instance.
     * @param {NodeList} headers - Table header elements.
     * @param {Object} columnsConfig - Column configuration.
     */
    constructor(headers, columnsConfig) {
        this.headers = headers;
        this.columnsConfig = columnsConfig;
        this.listeners = [];
    }

    /**
     * Subscribes to sort events.
     * @param {Function} callback - Callback(columnKey, order) => void.
     * @returns {SortManager} This instance for chaining.
     */
    onSort(callback) {
        this.listeners.push(callback);
        return this;
    }

    /**
     * Initializes sorting functionality.
     */
    init() {
        this.headers.forEach((header) => {
            const columnKey = Object.keys(this.columnsConfig).find(
                (key) => this.columnsConfig[key].index === header.cellIndex
            );

            if (columnKey) {
                const columnInfo = this.columnsConfig[columnKey];
                if (columnInfo.sortable !== false) {
                    header.dataset.columnKey = columnKey;
                    header.style.cursor = 'pointer';
                    header.addEventListener('click', () => this.handleHeaderClick(header));
                }
            }
        });
    }

    /**
     * Handles click on table header.
     * @param {HTMLElement} headerCell - Clicked header cell.
     */
    handleHeaderClick(headerCell) {
        const columnKey = headerCell.dataset.columnKey;
        if (!columnKey) return;

        const columnInfo = this.columnsConfig[columnKey];
        if (!columnInfo || columnInfo.sortable === false) return;

        const currentOrder = headerCell.dataset.sortOrder;
        const newOrder = currentOrder === 'asc' ? 'desc' : 'asc';

        this.listeners.forEach((callback) => callback(columnKey, newOrder));
    }

    /**
     * Updates sort indicators in headers.
     * @param {string} sortBy - Column key being sorted.
     * @param {string} sortOrder - Sort order ('asc' or 'desc').
     */
    updateHeaders(sortBy, sortOrder) {
        this.headers.forEach((th) => {
            th.dataset.sortOrder = '';

            th.textContent = th.textContent.replace(/[▲▼]/g, '').trim();

            const columnKey = th.dataset.columnKey;
            if (columnKey === sortBy) {
                th.dataset.sortOrder = sortOrder;
                th.textContent += sortOrder === 'asc' ? ' ▲' : ' ▼';
            }
        });
    }
}
