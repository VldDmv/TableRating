/**
 * Common utility functions used across the application.
 */

export const htmlUtils = {
    escape(str) {
        if (typeof str !== 'string' || !str) return '';
        return str
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    },

    decode(html) {
        if (!html) return '';
        const txt = document.createElement('textarea');
        txt.innerHTML = html;
        return txt.value;
    },
};

export const stringUtils = {
    capitalize(str) {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1);
    },
};

export const domUtils = {
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
            children.forEach((child) => {
                if (typeof child === 'string') {
                    element.appendChild(document.createTextNode(child));
                } else if (child instanceof Node) {
                    element.appendChild(child);
                }
            });
        }

        return element;
    },
};

/**
 * Security utility functions.

 */
export const securityUtils = {
    /**
     * Gets CSRF token from meta tag.
     * Token is set by Spring Security in JSP/Thymeleaf template.
     */
    getCsrfToken() {
        const meta = document.querySelector('meta[name="_csrf_token"]');
        if (!meta) {
            console.error('CSRF token meta tag not found');
            return null;
        }
        return meta.getAttribute('content');
    },

    /**
     * Gets CSRF header name.
     */
    getCsrfHeader() {
        return 'X-XSRF-TOKEN';
    },

    /**
     * Creates a hidden CSRF input element.
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
     */
    addCsrfToFormData(formData) {
        const token = this.getCsrfToken();
        if (token) {
            formData.append('_csrf', token);
        }
        return formData;
    },

    /**
     * Creates headers object with CSRF token.
     * Use this for fetch requests.
     */
    createSecureHeaders(additionalHeaders = {}) {
        const token = this.getCsrfToken();
        return {
            'X-XSRF-TOKEN': token,
            ...additionalHeaders,
        };
    },
};

export const entityUtils = {
    getItemTypeName(entityType) {
        return entityType === 'games' ? 'Tags' : 'Genres';
    },

    getAvailableItems(entityType) {
        if (entityType === 'games') {
            return window.allAvailableTags || [];
        }
        return window.allAvailableGenres || [];
    },

    getSingularName(entityType) {
        const names = {
            games: 'Game',
            movies: 'Movie',
            books: 'Book',
            shows: 'Show',
        };
        return names[entityType] || entityType;
    },
};

export const STATUS_META = {
    PLANNED: { icon: '📋', label: 'Planned' },
    IN_PROGRESS: { icon: '▶️', label: 'In Progress' },
    COMPLETED: { icon: '✅', label: 'Completed' },
    DROPPED: { icon: '🚫', label: 'Dropped' },
};

export const statusMeta = (status) => STATUS_META[status] || STATUS_META.PLANNED;

export const ICONS = {
    COMPLETED: '✅',
    NOT_COMPLETED: '❌',
    EDIT: '✏️',
    DELETE: '🗑️',
    SAVE: '💾',
    CANCEL: '❌',
    ARROW_UP: '▲',
    ARROW_DOWN: '▼',
};

export const CONSTANTS = {
    SEARCH_DEBOUNCE_MS: 500,
    AJAX_HEADER: 'X-Requested-With-AJAX',
    AJAX_HEADER_VALUE: 'true',
    DEFAULT_ROWS_PER_PAGE: 10,
    SCORE_THRESHOLDS: {
        GAMES: { low: 49, medium: 74 },
        MOVIES: { low: 39, medium: 60 },
        SHOWS: { low: 39, medium: 60 },
        BOOKS: { low: 49, medium: 79 },
    },
};
