/**
 * Manages all dynamic functionality for the user profile page, including
 * tab navigation, AJAX content loading, and DOM rendering.
 */

import { htmlUtils, stringUtils, ICONS } from './tableScripts/core/utils.js';

/**
 * Applies color-coded styling to all score cells based on their values.
 */
function applyScoreStylingToAllTables() {
    document.querySelectorAll(".score-cell").forEach(cell => {
        cell.classList.remove("score-low", "score-medium", "score-high");

        const score = parseInt(cell.textContent.trim(), 10);
        if (isNaN(score)) return;

        if (score <= 49) {
            cell.classList.add("score-low");
        } else if (score <= 74) {
            cell.classList.add("score-medium");
        } else {
            cell.classList.add("score-high");
        }
    });
}

/**
 * ProfilePageManager handles tab navigation and content loading for user profiles.
 */
class ProfilePageManager {
    constructor(config) {
        this.config = config;
        this.tabContainer = document.querySelector('.profile-tabs');
        this.contentContainer = document.getElementById('tab-content-container');

        if (!this.tabContainer || !this.contentContainer) {
            console.error('ProfilePageManager: Required elements for profile page not found.');
            return;
        }
        this.htmlUtils = htmlUtils;
        this.stringUtils = stringUtils;
        this.icons = ICONS;

        this.init();
    }

    /**
     * Initializes the profile page functionality.
     */
    init() {
        this.setupEventListeners();
        this.renderContent(this.config.initialResult, 'games');
    }

    /**
     * Sets up event listeners for tab switching and pagination.
     */
    setupEventListeners() {
        this.tabContainer.addEventListener('click', (event) => {
            const tab = event.target.closest('.tab-link');
            if (tab) {
                this.handleTabClick(tab);
            }
        });

        this.contentContainer.addEventListener('click', (event) => {
            const pageBtn = event.target.closest('.pagination-btn');
            if (pageBtn) {
                this.handlePaginationClick(pageBtn);
            }
        });
    }

    /**
     * Handles the logic when a category tab is clicked.
     * @param {HTMLElement} tab - The clicked tab element.
     */
    handleTabClick(tab) {
        const activeTab = this.tabContainer.querySelector('.active');
        if (activeTab) {
            activeTab.classList.remove('active');
        }
        tab.classList.add('active');

        const category = tab.dataset.category;
        this.loadCategory(category);
    }

    /**
     * Handles the logic when a pagination button is clicked.
     * @param {HTMLElement} button - The clicked pagination button element.
     */
    handlePaginationClick(button) {
        const category = this.tabContainer.querySelector('.active').dataset.category;
        const page = button.dataset.page;
        this.loadCategory(category, page);
    }

    /**
     * Loads data for a specific category and page via AJAX.
     * @param {string} category - The category to load (e.g., 'games', 'movies').
     * @param {number} [page=1] - The page number to load.
     */
    async loadCategory(category, page = 1) {
        this.showLoading();

        try {
            const url = `${this.config.contextPath}/profile-data?user=${this.config.username}&category=${category}&page=${page}`;
            const response = await fetch(url);

            if (!response.ok) {
                throw new Error(`Network response was not ok, status: ${response.status}`);
            }

            const pageResult = await response.json();
            this.renderContent(pageResult, category);
        } catch (error) {
            console.error('Failed to load category data:', error);
            this.showError();
        }
    }

    /**
     * Shows a loading indicator in the content area.
     */
    showLoading() {
        this.contentContainer.innerHTML = '<p class="loading-text">Loading...</p>';
    }

    /**
     * Shows an error message in the content area.
     */
    showError() {
        this.contentContainer.innerHTML = '<p class="error-message">Failed to load content. Please try again.</p>';
    }

    /**
     * Renders the fetched content (table and pagination) into the DOM.
     * @param {object} pageResult - The paginated data result from the server.
     * @param {string} category - The category being rendered.
     */
    renderContent(pageResult, category) {
        this.contentContainer.innerHTML = '';

        if (!pageResult || !pageResult.items || pageResult.items.length === 0) {
            this.contentContainer.innerHTML = `<p>This user hasn't added any ${category} yet.</p>`;
            return;
        }

        const table = this.createTable(pageResult.items, category);
        this.contentContainer.appendChild(table);

        if (pageResult.totalPages > 1) {
            const pagination = this.createPagination(pageResult);
            this.contentContainer.appendChild(pagination);
        }

        applyScoreStylingToAllTables();
    }

    /**
     * Creates and returns a complete HTML table element for the items.
     * @param {Array<object>} items - The items to display.
     * @param {string} category - The category being displayed.
     * @returns {HTMLTableElement} The created table element.
     */
    createTable(items, category) {
        const table = document.createElement('table');
        table.className = 'profile-table';

        const headerText = category === 'games' ? 'Tags' : 'Genres';
        const nameHeaderText = this.stringUtils.capitalize(
            category.endsWith('s') ? category.slice(0, -1) : category
        );

        table.innerHTML = `
            <thead>
                <tr>
                    <th class="col-name">${nameHeaderText}</th>
                    <th class="col-tags">${headerText}</th>
                    <th class="col-score">Score</th>
                    <th class="col-completed">Completed</th>
                </tr>
            </thead>
        `;

        const tbody = document.createElement('tbody');
        items.forEach(item => {
            tbody.appendChild(this.createTableRow(item));
        });

        table.appendChild(tbody);
        return table;
    }

    /**
     * Creates and returns a single HTML table row element for an item.
     * @param {object} item - The data for the item.
     * @returns {HTMLTableRowElement} The created table row element.
     */
    createTableRow(item) {
        const tr = document.createElement('tr');
        const tags = this.renderTags(item.tags || item.genres);
        const completedIcon = item.completed ? this.icons.COMPLETED : this.icons.NOT_COMPLETED;

        tr.innerHTML = `
            <td class="col-name">${this.htmlUtils.escape(item.name)}</td>
            <td class="col-tags">${tags}</td>
            <td class="col-score"><span class="score-cell">${item.score}</span></td>
            <td class="col-completed">${completedIcon}</td>
        `;

        return tr;
    }

    /**
     * Renders an array of tags/genres as a string of HTML badges.
     * @param {Array<object>} tags - The tags or genres to render.
     * @returns {string} The HTML string of tag badges.
     */
    renderTags(tags) {
        if (!tags || tags.length === 0) return '';
        return tags
            .map(tag => `<span class="tag-badge">${this.htmlUtils.escape(tag.name)}</span>`)
            .join(' ');
    }

    /**
     * Creates and returns the HTML element for pagination controls.
     * @param {object} pageResult - The paginated data result.
     * @returns {HTMLDivElement} The pagination div element.
     */
    createPagination(pageResult) {
        const paginationDiv = document.createElement('div');
        paginationDiv.className = 'pagination';

        if (pageResult.currentPage > 1) {
            const prevBtn = this.createPaginationButton(
                '&laquo; Previous',
                pageResult.currentPage - 1
            );
            paginationDiv.appendChild(prevBtn);
        }

        const pageInfo = document.createElement('span');
        pageInfo.textContent = `Page ${pageResult.currentPage} of ${pageResult.totalPages}`;
        paginationDiv.appendChild(pageInfo);

        if (pageResult.currentPage < pageResult.totalPages) {
            const nextBtn = this.createPaginationButton(
                'Next &raquo;',
                pageResult.currentPage + 1
            );
            paginationDiv.appendChild(nextBtn);
        }

        return paginationDiv;
    }

    /**
     * Creates and returns a single pagination button.
     * @param {string} text - The text/HTML content for the button.
     * @param {number} page - The page number this button navigates to.
     * @returns {HTMLButtonElement} The created button element.
     */
    createPaginationButton(text, page) {
        const button = document.createElement('button');
        button.innerHTML = text;
        button.className = 'pagination-btn';
        button.dataset.page = page;
        return button;
    }
}

// --- Application Entry Point ---
document.addEventListener('DOMContentLoaded', () => {
    // This is called once, after the initial page load (for the first table render).
    applyScoreStylingToAllTables();

    if (window.profileConfig) {
        new ProfilePageManager(window.profileConfig);
    }
});