/**
 * Common utility functions used across the application.
 */

/**
 * HTML utility functions for escaping and decoding.
 */
export const htmlUtils = {
    /**
     * Escapes HTML characters in a string to prevent XSS.
     * @param {string} str - The string to escape.
     * @returns {string} The escaped string.
     */
    escape(str) {
        if (typeof str !== 'string' || !str) return '';


        return str.replace(/&/g, '&amp;')
                  .replace(/</g, '&lt;')
                  .replace(/>/g, '&gt;')
                  .replace(/"/g, '&quot;')
                  .replace(/'/g, '&#39;');
    },

    /**
     * Decodes HTML entities in a string.
     * @param {string} html - The HTML string to decode.
     * @returns {string} The decoded string.
     */
    decode(html) {
        if (!html) return '';
        const txt = document.createElement('textarea');
        txt.innerHTML = html;
        return txt.value;
    }
};

/**
 * String utility functions.
 */
export const stringUtils = {
    /**
     * Capitalizes the first letter of a string.
     * @param {string} str - The string to capitalize.
     * @returns {string} The capitalized string.
     */
    capitalize(str) {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }
};

/**
 * DOM utility functions.
 */
export const domUtils = {
    /**
     * Creates an element with given attributes and children.
     * @param {string} tag - The HTML tag name.
     * @param {Object} attrs - Attributes to set on the element.
     * @param {Array|string} children - Child elements or text content.
     * @returns {HTMLElement} The created element.
     */
    createElement(tag, attrs = {}, children = []) {
        const element = document.createElement(tag);

        Object.entries(attrs).forEach(([key, value]) => {
            if (key === 'className') {
                element.className = value;
            } else if (key === 'dataset') {
                Object.entries(value).forEach(([dataKey, dataValue]) => {
                    element.dataset[dataKey] = dataValue;
                });
            } else if (key.startsWith('on') && typeof value === 'function') {
                element.addEventListener(key.substring(2).toLowerCase(), value);
            } else {
                element.setAttribute(key, value);
            }
        });

        if (typeof children === 'string') {
            element.textContent = children;
        } else if (Array.isArray(children)) {
            children.forEach(child => {
                if (typeof child === 'string') {
                    element.appendChild(document.createTextNode(child));
                } else if (child instanceof Node) {
                    element.appendChild(child);
                }
            });
        }

        return element;
    }
};

/**
 * Security utility functions.
 */
export const securityUtils = {
    /**
     * Gets CSRF token from meta tag.
     * @returns {string|null} CSRF token or null if not found.
     */
    getCsrfToken() {
        const meta = document.querySelector('meta[name="_csrf_token"]');
        if (!meta) {
            console.error('CSRF token not found in page meta tags');
            return null;
        }
        return meta.getAttribute('content');
    },

    /**
     * Creates a hidden CSRF input element.
     * @returns {HTMLInputElement} Input element with CSRF token.
     */
    createCsrfInput() {
        const token = this.getCsrfToken();
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = '_csrf';
        input.value = token || '';
        return input;
    },

    /**
     * Adds CSRF token to FormData.
     * @param {FormData} formData - FormData object to add token to.
     * @returns {FormData} FormData with token added.
     */
    addCsrfToFormData(formData) {
        const token = this.getCsrfToken();
        if (token) {
            formData.append('_csrf', token);
        }
        return formData;
    }
};

/**
 * Entity-specific utility functions.
 */
export const entityUtils = {
    /**
     * Gets the appropriate item type name (Tags or Genres) for entity type.
     * @param {string} entityType - The entity type (e.g., 'games', 'movies').
     * @returns {string} Item type name.
     */
    getItemTypeName(entityType) {
        return entityType === 'games' ? 'Tags' : 'Genres';
    },

    /**
     * Gets available items (tags or genres) for entity type.
     * @param {string} entityType - The entity type.
     * @returns {Array} Array of available items.
     */
    getAvailableItems(entityType) {
        if (entityType === 'games') {
            return window.allAvailableTags || [];
        }
        return window.allAvailableGenres || [];
    },

    /**
     * Gets the singular name for an entity type.
     * @param {string} entityType - The entity type.
     * @returns {string} Singular entity name.
     */
    getSingularName(entityType) {
        const names = {
            games: 'Game',
            movies: 'Movie',
            books: 'Book',
            shows: 'Show'
        };
        return names[entityType] || entityType;
    }
};

/**
 * UI Icons constants.
 */
export const ICONS = {
    COMPLETED: '✅',
    NOT_COMPLETED: '❌',
    EDIT: '✏️',
    DELETE: '🗑️',
    SAVE: '💾',
    CANCEL: '❌',
    ARROW_UP: '▲',
    ARROW_DOWN: '▼'
};

/**
 * Constants used across the application.
 */
export const CONSTANTS = {
    SEARCH_DEBOUNCE_MS: 500,
    AJAX_HEADER: 'X-Requested-With-AJAX',
    AJAX_HEADER_VALUE: 'true',
    DEFAULT_ROWS_PER_PAGE: 10,
    SCORE_THRESHOLDS: {
        GAMES: { low: 49, medium: 74 },
        MOVIES: { low: 39, medium: 60 },
        SHOWS: { low: 39, medium: 60 },
        BOOKS: { low: 49, medium: 79 }
    }
};