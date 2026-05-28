/**
 * Sort Controls for Card View
 * Provides sorting UI when table headers are not visible
 */

export class SortControls {
    constructor(config, stateManager) {
        this.config = config;
        this.stateManager = stateManager;
        this.container = null;
    }

    /**
     * Initialize sort controls
     */
    init() {
        this.createControls();
        this.updateActiveSort();
    }

    /**
     * Create sort controls UI
     */
    createControls() {
        // Find table-controls or create after view toggle
        const tableControls = document.querySelector('.table-controls');
        if (!tableControls) {
            console.warn('[SortControls] .table-controls not found');
            return;
        }

        // Create sort controls container
        this.container = document.createElement('div');
        this.container.className = 'sort-controls-container';
        this.container.id = 'sort-controls';

        // Build sortable columns from config
        const sortableColumns = this.getSortableColumns();

        this.container.innerHTML = `
            <label class="sort-controls-label">
                Sort by:
                <div class="sort-controls-group">
                    <select class="sort-select" id="sortBySelect">
                        ${sortableColumns
                            .map(
                                (col) => `
                            <option value="${col.key}">${col.name}</option>
                        `
                            )
                            .join('')}
                    </select>
                    <button type="button" class="sort-order-btn" id="sortOrderBtn" title="Toggle sort order">
                        <span class="sort-icon">↓</span>
                    </button>
                </div>
            </label>
        `;

        // Insert into table-controls
        tableControls.appendChild(this.container);

        // Attach event listeners
        this.attachEventListeners();
    }

    /**
     * Get sortable columns from config
     * @returns {Array} Array of {key, name} objects
     */
    getSortableColumns() {
        const columns = [];

        for (const key in this.config.columns) {
            const col = this.config.columns[key];
            if (col.sortable !== false && key !== 'actions' && key !== 'cover') {
                columns.push({
                    key: key,
                    name: col.name,
                });
            }
        }

        return columns;
    }

    /**
     * Attach event listeners
     */
    attachEventListeners() {
        const sortSelect = document.getElementById('sortBySelect');
        const sortOrderBtn = document.getElementById('sortOrderBtn');

        if (sortSelect) {
            sortSelect.addEventListener('change', () => {
                const newSortBy = sortSelect.value;
                const currentState = this.stateManager.getState();

                this.stateManager.setState({
                    sortBy: newSortBy,
                    currentPage: 1,
                });

                this.updateActiveSort();
            });
        }

        if (sortOrderBtn) {
            sortOrderBtn.addEventListener('click', () => {
                const currentState = this.stateManager.getState();
                const newOrder = currentState.sortOrder === 'asc' ? 'desc' : 'asc';

                this.stateManager.setState({
                    sortOrder: newOrder,
                });

                this.updateActiveSort();
            });
        }
    }

    /**
     * Update UI to reflect current sort state
     */
    updateActiveSort() {
        const state = this.stateManager.getState();

        // Update select value
        const sortSelect = document.getElementById('sortBySelect');
        if (sortSelect) {
            sortSelect.value = state.sortBy;
        }

        // Update order button icon
        const sortOrderBtn = document.getElementById('sortOrderBtn');
        const sortIcon = sortOrderBtn?.querySelector('.sort-icon');

        if (sortIcon) {
            sortIcon.textContent = state.sortOrder === 'asc' ? '↑' : '↓';
            sortOrderBtn.title = state.sortOrder === 'asc' ? 'Sort descending' : 'Sort ascending';
        }

        // Add active class to button
        if (sortOrderBtn) {
            sortOrderBtn.classList.toggle('sort-asc', state.sortOrder === 'asc');
            sortOrderBtn.classList.toggle('sort-desc', state.sortOrder === 'desc');
        }
    }

    /**
     * Show sort controls (for card view)
     */
    show() {
        if (this.container) {
            this.container.style.display = 'flex';
        }
    }

    /**
     * Hide sort controls (for table view)
     */
    hide() {
        if (this.container) {
            this.container.style.display = 'none';
        }
    }

    /**
     * Get current sort state
     * @returns {Object} {sortBy, sortOrder}
     */
    getCurrentSort() {
        const state = this.stateManager.getState();
        return {
            sortBy: state.sortBy,
            sortOrder: state.sortOrder,
        };
    }
}
