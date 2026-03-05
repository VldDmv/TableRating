/**
 * Manages the visibility of table columns using in-memory state.
 */

export class ColumnManager {
    /**
     * Creates a new ColumnManager instance.
     * @param {Object} config - Configuration with entityType and columns.
     */
    constructor(config) {
        this.entityType = config.entityType;
        this.columnsConfig = config.columns;

        this.tableElement = document.querySelector(`table[data-entity-type="${this.entityType}"]`);
        this.containerElement = document.getElementById('column-toggle-container');


        this.initStateStorage();
    }

    /**
     * Initializes the global state storage for column visibility.
     */
    initStateStorage() {
        if (!window.__columnVisibilityState) {
            window.__columnVisibilityState = {};
        }
        this.stateStorage = window.__columnVisibilityState;
    }

    /**
     * Gets hidden columns for current entity.
     * @returns {Array<string>} Array of hidden column keys.
     */
    getHiddenColumns() {
        return this.stateStorage[this.entityType] || [];
    }

    /**
     * Saves hidden columns for current entity.
     * @param {Array<string>} hiddenColumns - Array of hidden column keys.
     */
    saveHiddenColumns(hiddenColumns) {
        this.stateStorage[this.entityType] = hiddenColumns;
    }

    /**
     * Applies visibility settings to the table.
     */
    applyVisibility() {
        if (!this.tableElement) return;

        const hiddenColumns = this.getHiddenColumns();
        this.tableElement.dataset.hiddenColumns = hiddenColumns.join(' ');
    }

    /**
     * Handles checkbox toggle events.
     */
    handleToggleChange() {
        if (!this.containerElement) return;

        const hiddenKeys = [];
        this.containerElement.querySelectorAll('input[type="checkbox"]').forEach(checkbox => {
            if (!checkbox.checked) {
                hiddenKeys.push(checkbox.dataset.columnKey);
            }
        });

        this.saveHiddenColumns(hiddenKeys);
        this.applyVisibility();
    }

    /**
     * Creates the UI for column toggle checkboxes.
     */
    createToggleUI() {
        if (!this.containerElement) return;

        const hiddenColumns = this.getHiddenColumns();
        this.containerElement.innerHTML = '<span class="column-controls-title">Show Columns:</span>';

        for (const key in this.columnsConfig) {
            const column = this.columnsConfig[key];
            if (column.hideable) {
                const label = this.createCheckboxLabel(key, column, hiddenColumns);
                this.containerElement.appendChild(label);
            }
        }
    }

    /**
     * Creates a checkbox label for a column.
     * @param {string} key - Column key.
     * @param {Object} column - Column configuration.
     * @param {Array<string>} hiddenColumns - Currently hidden columns.
     * @returns {HTMLElement} Label element.
     */
    createCheckboxLabel(key, column, hiddenColumns) {
        const label = document.createElement('label');
        const checkbox = document.createElement('input');

        checkbox.type = 'checkbox';
        checkbox.dataset.columnKey = key;
        checkbox.checked = !hiddenColumns.includes(key);
        checkbox.addEventListener('change', () => this.handleToggleChange());

        label.appendChild(checkbox);
        label.appendChild(document.createTextNode(` ${column.name}`));

        return label;
    }

    /**
     * Initializes the column manager.
     */
    init() {
        if (!this.tableElement || !this.containerElement || !this.columnsConfig) {
            console.warn("Column manager init failed: required elements or config not found.");
            return;
        }

        this.createToggleUI();
        this.applyVisibility();
    }

    /**
     * Resets column visibility to default (all visible).
     */
    reset() {
        this.saveHiddenColumns([]);
        this.applyVisibility();
        this.createToggleUI();
    }

    /**
     * Toggles a specific column's visibility.
     * @param {string} columnKey - Column key to toggle.
     */
    toggleColumn(columnKey) {
        const hiddenColumns = this.getHiddenColumns();
        const index = hiddenColumns.indexOf(columnKey);

        if (index > -1) {
            hiddenColumns.splice(index, 1);
        } else {
            hiddenColumns.push(columnKey);
        }

        this.saveHiddenColumns(hiddenColumns);
        this.applyVisibility();
    }
}