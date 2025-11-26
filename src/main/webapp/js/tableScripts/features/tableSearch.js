/**
 * Manages search functionality with debouncing.
 */

import { CONSTANTS } from '../core/utils.js';

export class SearchManager {
    /**
     * Creates a new SearchManager instance.
     * @param {HTMLInputElement} searchInput - Search input element.
     * @param {number} debounceMs - Debounce delay in milliseconds.
     */
    constructor(searchInput, debounceMs = CONSTANTS.SEARCH_DEBOUNCE_MS) {
        this.searchInput = searchInput;
        this.debounceMs = debounceMs;
        this.debounceTimeout = null;
        this.listeners = [];
    }

    /**
     * Subscribes to search events.
     * @param {Function} callback - Callback(searchTerm) => void.
     * @returns {SearchManager} This instance for chaining.
     */
    onSearch(callback) {
        this.listeners.push(callback);
        return this;
    }

    /**
     * Initializes search functionality.
     */
    init() {
        if (!this.searchInput) {
            console.warn('SearchManager: Search input element not found');
            return;
        }

        this.searchInput.addEventListener('input', () => {
            clearTimeout(this.debounceTimeout);
            this.debounceTimeout = setTimeout(() => {
                const searchTerm = this.searchInput.value.trim();
                this.listeners.forEach(callback => callback(searchTerm));
            }, this.debounceMs);
        });
    }

    /**
     * Clears the search input.
     */
    clear() {
        if (this.searchInput) {
            this.searchInput.value = '';
        }
    }

    /**
     * Gets current search value.
     * @returns {string} Current search term.
     */
    getValue() {
        return this.searchInput ? this.searchInput.value.trim() : '';
    }
}
