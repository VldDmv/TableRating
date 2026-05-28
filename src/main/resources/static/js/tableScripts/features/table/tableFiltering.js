/**
 * Manages filtering functionality for category tables.
 */

export class FilterManager {
    /**
     * Creates a new FilterManager instance.
     * @param {HTMLSelectElement} filterSelect - Filter select element.
     */
    constructor(filterSelect) {
        this.filterSelect = filterSelect;
        this.listeners = [];
    }

    /**
     * Subscribes to filter change events.
     * @param {Function} callback - Callback(filterId) => void.
     * @returns {FilterManager} This instance for chaining.
     */
    onChange(callback) {
        this.listeners.push(callback);
        return this;
    }

    /**
     * Initializes filtering functionality.
     */
    init() {
        if (!this.filterSelect) {
            console.warn('FilterManager: Filter select element not found');
            return;
        }

        this.filterSelect.addEventListener('change', () => {
            const filterId = this.filterSelect.value;
            this.listeners.forEach((callback) => callback(filterId));
        });
    }

    /**
     * Gets current filter value.
     * @returns {string} Current filter ID.
     */
    getValue() {
        return this.filterSelect ? this.filterSelect.value : 'all';
    }

    /**
     * Sets filter value.
     * @param {string} value - Filter value to set.
     */
    setValue(value) {
        if (this.filterSelect) {
            this.filterSelect.value = value;
        }
    }

    /**
     * Resets filter to default.
     */
    reset() {
        this.setValue('all');
    }
}
