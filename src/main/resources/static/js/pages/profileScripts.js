export { getScoreClass, getSortIcon, escapeHtml, buildApiUrl, renderTableRow, renderCard, getEmptyMessage, ProfilePageManager }

const config = window.profileConfig || {};
const username = config.username || '';
const contextPath = config.contextPath || '';

// State management
let currentCategory = 'games';
let currentViewMode = 'table'; // 'table' or 'cards'
let cachedData = {}; // Cache for API responses

// Filter & Search & Sort state
let currentPage = 1;
let totalPages = 1;
let rowsPerPage = 10;
let searchTerm = '';
let filterId = 'all';
let sortBy = 'name';
let sortOrder = 'asc';

// DOM Elements
let prevPageBtn;
let nextPageBtn;
let pageDropdown;
let pageList;
let paginationContainer;
let searchBox;
let tagFilter;
let rowsPerPageSelect;
let sortBySelect;
let sortOrderBtn;
let sortControls;
let tableViewBtn;
let cardsViewBtn;
let tabContentContainer;
let cardsContainer;

/**
 * Initializes the profile page
 */
function init() {


    // Setup DOM elements
    setupDomElements();

    // Setup tabs
    setupTabs();

    // Setup controls
    setupSearchAndFilter();
    setupPagination();
    setupViewToggle();
    setupSort();
    setupRowsPerPage();

    // Load initial data
    if (config.initialResult?.items) {

        cachedData[currentCategory] = config.initialResult;
        currentPage = config.initialResult.currentPage || 1;
        totalPages = config.initialResult.totalPages || 1;
        populateFilterDropdown();
        renderContent(config.initialResult);
        updatePagination();
    } else {

        populateFilterDropdown();
        loadCategoryData();
    }


}

/**
 * Sets up DOM element references
 */
function setupDomElements() {
    prevPageBtn = document.getElementById('prevPage');
    nextPageBtn = document.getElementById('nextPage');
    pageDropdown = document.getElementById('pageDropdown');
    pageList = document.getElementById('pageList');
    paginationContainer = document.getElementById('paginationContainer');
    searchBox = document.getElementById('searchBox');
    tagFilter = document.getElementById('tagFilter');
    rowsPerPageSelect = document.getElementById('rowsPerPage');
    sortBySelect = document.getElementById('sortBySelect');
    sortOrderBtn = document.getElementById('sortOrderBtn');
    sortControls = document.getElementById('sort-controls');
    tableViewBtn = document.getElementById('tableViewBtn');
    cardsViewBtn = document.getElementById('cardsViewBtn');
    tabContentContainer = document.getElementById('tab-content-container');
    cardsContainer = document.getElementById('cards-container');
}

/**
 * Sets up tab switching
 */
function setupTabs() {
    const tabs = document.querySelectorAll('.tab-link');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            // Update active tab
            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');

            // Switch category
            const category = tab.dataset.category;
            switchCategory(category);
        });
    });
}

/**
 * Sets up search and filter
 */
function setupSearchAndFilter() {
    // Search with debounce
    let searchTimeout;
    if (searchBox) {
        searchBox.addEventListener('input', (e) => {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                searchTerm = e.target.value.trim();
                currentPage = 1; // Reset to page 1
                loadCategoryData();
            }, 500);
        });
    }

    // Filter dropdown
    if (tagFilter) {
        tagFilter.addEventListener('change', (e) => {
            filterId = e.target.value;
            currentPage = 1; // Reset to page 1
            loadCategoryData();
        });
    }
}

/**
 * Sets up pagination event listeners
 */
function setupPagination() {
    if (!prevPageBtn || !nextPageBtn || !pageDropdown || !pageList) {
        console.warn('⚠️ Pagination elements not found');
        return;
    }

    // Previous button
    prevPageBtn.addEventListener('click', () => {
        if (currentPage > 1) {
            loadCategoryData(currentPage - 1);
        }
    });

    // Next button
    nextPageBtn.addEventListener('click', () => {
        if (currentPage < totalPages) {
            loadCategoryData(currentPage + 1);
        }
    });

    // Dropdown toggle
    pageDropdown.addEventListener('click', (e) => {
        e.stopPropagation();
        const isVisible = pageList.style.display === 'block';
        pageList.style.display = isVisible ? 'none' : 'block';
    });

    // Close dropdown when clicking outside
    document.addEventListener('click', () => {
        if (pageList) {
            pageList.style.display = 'none';
        }
    });

}

/**
 * Sets up view toggle (table/cards)
 */
function setupViewToggle() {
    if (!tableViewBtn || !cardsViewBtn) return;

    tableViewBtn.addEventListener('click', () => switchView('table'));
    cardsViewBtn.addEventListener('click', () => switchView('cards'));

    // Restore saved view preference
    const savedView = localStorage.getItem(`profile-view-${username}`);
    if (savedView === 'cards') {
        switchView('cards');
    }
}

/**
 * Sets up sort controls
 */
function setupSort() {
    if (sortBySelect) {
        sortBySelect.addEventListener('change', (e) => {
            sortBy = e.target.value;
            currentPage = 1;
            loadCategoryData();
            updateSortUI();
        });
    }

    if (sortOrderBtn) {
        sortOrderBtn.addEventListener('click', () => {
            sortOrder = sortOrder === 'asc' ? 'desc' : 'asc';
            currentPage = 1;
            loadCategoryData();
            updateSortUI();
        });
    }
}

/**
 * Sets up rows per page control
 */
function setupRowsPerPage() {
    if (rowsPerPageSelect) {
        rowsPerPageSelect.addEventListener('change', (e) => {
            rowsPerPage = parseInt(e.target.value, 10);
            currentPage = 1; // Reset to page 1
            loadCategoryData();
        });
    }
}

/**
 * Populates filter dropdown with tags/genres
 */
function populateFilterDropdown() {
    if (!tagFilter) return;

    // Clear existing options except "All"
    tagFilter.innerHTML = '<option value="all">All</option>';

    // Update label
    const filterLabel = document.getElementById('filterLabel');
    if (filterLabel) {
        filterLabel.textContent = currentCategory === 'games' ? 'Filter by Tag:' : 'Filter by Genre:';
    }

    // Get tags/genres for current category
    let items = [];
    if (currentCategory === 'games' && window.allGameTags) {
        items = window.allGameTags;
    } else if (currentCategory === 'movies' && window.allMovieGenres) {
        items = window.allMovieGenres;
    } else if (currentCategory === 'books' && window.allBookGenres) {
        items = window.allBookGenres;
    } else if (currentCategory === 'shows' && window.allShowGenres) {
        items = window.allShowGenres;
    }

    // Populate dropdown
    items.forEach(item => {
        const option = document.createElement('option');
        option.value = item.id;
        option.textContent = item.name;
        tagFilter.appendChild(option);
    });

    // Reset filter when switching categories
    tagFilter.value = 'all';
    filterId = 'all';
}

/**
 * Switches between categories
 */
function switchCategory(category) {
    currentCategory = category;
    currentPage = 1;
    searchTerm = '';
    filterId = 'all';

    // Clear search box
    if (searchBox) searchBox.value = '';

    // Update filter dropdown
    populateFilterDropdown();

    // Load data
    loadCategoryData();

    // Notify the stats panel so it can refresh for the new category
    document.dispatchEvent(new CustomEvent('profile:categoryChanged', { detail: { category } }));
}

/**
 * Loads category data from server
 */
async function loadCategoryData(page = currentPage) {
    const container = tabContentContainer;
    if (!container) return;

    // Show loading
    container.innerHTML = `
        <div class="loading-state">
            <div class="spinner"></div>
            <p>Loading ${currentCategory}...</p>
        </div>
    `;
    if (cardsContainer) {
        cardsContainer.innerHTML = '';
    }

    try {
        // Build URL with all parameters
        const url = buildApiUrl(page);


        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();


        // Update state
        currentPage = data.currentPage || 1;
        totalPages = data.totalPages || 1;

        // Cache the data
        cachedData[currentCategory] = data;

        // Render content
        renderContent(data);

        // Update pagination
        updatePagination();

    } catch (error) {
        console.error('❌ Error loading data:', error);
        container.innerHTML = `
            <div class="error-state">
                <div class="error-icon">⚠️</div>
                <h3>Error Loading Data</h3>
                <p>${error.message}</p>
            </div>
        `;
    }
}

/**
 * Builds API URL with all parameters.
 * Accepts a page number (uses module-level state) or an options object for
 * fully explicit calls (used by tests and ProfilePageManager).
 */
function buildApiUrl(pageOrOptions) {
    let opts;
    if (typeof pageOrOptions === 'object' && pageOrOptions !== null) {
        opts = {
            username, currentCategory, currentPage: 1, rowsPerPage,
            sortBy, sortOrder, searchTerm: '', filterId: 'all', contextPath,
            ...pageOrOptions
        };
    } else {
        opts = {
            username, currentCategory, currentPage: pageOrOptions, rowsPerPage,
            sortBy, sortOrder, searchTerm, filterId, contextPath
        };
    }

    const apiPath = `${opts.contextPath}/profile-data`.replace(/\/+/g, '/');
    const origin =
        (typeof window !== 'undefined' && window.location?.origin && window.location.origin !== 'null')
            ? window.location.origin
            : 'http://localhost';
    const url = new URL(`${origin}${apiPath}`);

    url.searchParams.set('username',  opts.username);
    url.searchParams.set('category',  opts.currentCategory);
    url.searchParams.set('page',      opts.currentPage);
    url.searchParams.set('pageSize',  opts.rowsPerPage);
    url.searchParams.set('sortBy',    opts.sortBy);
    url.searchParams.set('sortOrder', opts.sortOrder);

    if (opts.searchTerm) {
        url.searchParams.set('search', opts.searchTerm);
    }

    if (opts.filterId && opts.filterId !== 'all') {
        const filterParam = opts.currentCategory === 'games' ? 'tag_id' : 'genre_id';
        url.searchParams.set(filterParam, opts.filterId);
    }

    return url.toString();
}

/**
 * Renders content based on view mode
 */
function renderContent(data) {
    if (!data.items || data.items.length === 0) {
        showEmptyState();
        return;
    }

    if (currentViewMode === 'table') {
        renderTable(data.items);
    } else {
        renderCards(data.items);
    }
}

/**
 * Shows empty state
 */
function showEmptyState() {
    const message = searchTerm || filterId !== 'all'
        ? 'No items found matching your criteria.'
        : `No ${currentCategory} found`;

    if (currentViewMode === 'table') {
        tabContentContainer.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">📭</div>
                <h3>${message}</h3>
            </div>
        `;
        tabContentContainer.style.display = 'block';
        cardsContainer.style.display = 'none';
    } else {
        cardsContainer.innerHTML = `
            <div class="cards-empty-state">
                <p>${message}</p>
            </div>
        `;
        cardsContainer.style.display = 'block';
        tabContentContainer.innerHTML = '';
        tabContentContainer.style.display = 'none';
    }
}

/**
 * Renders table view
 */
function renderTable(items) {
    const categoryLabels = {
        'games': 'Tags',
        'movies': 'Genres',
        'books': 'Genres',
        'shows': 'Genres'
    };

    const html = `
        <table class="profile-table" data-entity-type="${currentCategory}">
            <thead>
                <tr>
                    <th class="col-cover">Cover</th>
                    <th class="col-name sortable" data-sort="name">${currentCategory.slice(0, -1).charAt(0).toUpperCase() + currentCategory.slice(1, -1)} ${getSortIcon('name')}</th>
                    <th class="col-score sortable" data-sort="score">Score ${getSortIcon('score')}</th>
                    <th class="col-tags">${categoryLabels[currentCategory]}</th>
                    <th class="col-completed sortable" data-sort="completed">Completed ${getSortIcon('completed')}</th>
                </tr>
            </thead>
            <tbody>
                ${items.map(item => renderTableRow(item)).join('')}
            </tbody>
        </table>
    `;

    tabContentContainer.innerHTML = html;
    tabContentContainer.style.display = 'block';
    cardsContainer.style.display = 'none';



    const sortableHeaders = tabContentContainer.querySelectorAll('.sortable');
    sortableHeaders.forEach(header => {
        header.style.cursor = 'pointer';
        header.addEventListener('click', () => {
            const newSortBy = header.dataset.sort;
            if (sortBy === newSortBy) {
                // Toggle order
                sortOrder = sortOrder === 'asc' ? 'desc' : 'asc';
            } else {
                // New column, default to asc
                sortBy = newSortBy;
                sortOrder = 'asc';
            }
            currentPage = 1;
            loadCategoryData();
        });
    });
}

/**
 * Gets sort icon for column
 */
function getSortIcon(columnKey, curSortBy = sortBy, curSortOrder = sortOrder) {
    if (curSortBy !== columnKey) {
        return '';
    }
    return curSortOrder === 'asc' ? '▲' : '▼';
}

/**
 * Renders a single table row
 */
function renderTableRow(item, category = currentCategory) {
    const tags = category === 'games'
        ? (item.tags || []).map(t => t.name).join(', ')
        : (item.genres || []).map(g => g.name).join(', ');

    const completedIcon = item.completed ? '✅' : '❌';
    const scoreClass = getScoreClass(item.score);

    return `
        <tr>
            <td class="col-cover">
                ${item.coverUrl
                    ? `<img src="${item.coverUrl}" alt="${item.name}" class="cover-thumbnail">`
                    : `<div class="cover-placeholder">📄</div>`
                }
            </td>
            <td class="col-name">${escapeHtml(item.name)}</td>
            <td class="col-score">
                <span class="score-cell ${scoreClass}">${item.score}</span>
            </td>
            <td class="col-tags">${tags || '-'}</td>
            <td class="col-completed">${completedIcon}</td>
        </tr>
    `;
}

/**
 * Renders cards view
 */
function renderCards(items) {
    const html = items.map(item => renderCard(item)).join('');
    cardsContainer.innerHTML = html;
    cardsContainer.style.display = 'grid';
    tabContentContainer.innerHTML = '';
    tabContentContainer.style.display = 'none';
}

/**
 * Renders a single card
 */
function renderCard(item, category = currentCategory) {
    const tags = category === 'games'
        ? (item.tags || []).map(t => `<span class="tag-badge">${t.name}</span>`).join('')
        : (item.genres || []).map(g => `<span class="tag-badge">${g.name}</span>`).join('');

    const completedIcon = item.completed ? '✅' : '❌';
    const scoreClass = getScoreClass(item.score);

    return `
        <div class="media-card">
            <div class="card-cover">
                ${item.coverUrl
                    ? `<img src="${item.coverUrl}" alt="${item.name}" class="card-cover-image">`
                    : `<div class="card-cover-placeholder">📄</div>`
                }
            </div>
            <div class="card-content">
                <h3 class="card-title">${escapeHtml(item.name)}</h3>
                <div class="card-score">
                    <span class="score-cell ${scoreClass}">${item.score}</span>
                </div>
                <div class="card-tags">
                    ${tags || '<span class="card-no-tags">No tags</span>'}
                </div>
                <div class="card-status">
                    <span class="status-label">Completed:</span>
                    <span>${completedIcon}</span>
                </div>
            </div>
        </div>
    `;
}

/**
 * Updates pagination display with dropdown
 */
function updatePagination() {
    if (!prevPageBtn || !nextPageBtn || !pageDropdown || !pageList) {
        console.warn('⚠️ Pagination elements not found');
        return;
    }

    // Update button states
    prevPageBtn.disabled = currentPage <= 1;
    nextPageBtn.disabled = currentPage >= totalPages;

    // Update dropdown text
    pageDropdown.textContent = `Page ${currentPage} of ${totalPages}`;

    // Build page list dropdown
    pageList.innerHTML = '';
    for (let i = 1; i <= totalPages; i++) {
        const li = document.createElement('li');
        const a = document.createElement('a');
        a.href = '#';
        a.textContent = i;
        a.className = i === currentPage ? 'active-page' : '';
        a.addEventListener('click', (e) => {
            e.preventDefault();
            pageList.style.display = 'none';
            loadCategoryData(i);
        });
        li.appendChild(a);
        pageList.appendChild(li);
    }

    // Show/hide pagination container
    if (paginationContainer) {
        if (totalPages <= 1) {
            paginationContainer.style.display = 'none';
        } else {
            paginationContainer.style.display = 'flex';
        }
    }
}

/**
 * Switches between view modes
 */
function switchView(mode) {
    currentViewMode = mode;
    localStorage.setItem(`profile-view-${username}`, mode);

    // Update buttons
    if (tableViewBtn && cardsViewBtn) {
        if (mode === 'table') {
            tableViewBtn.classList.add('active');
            cardsViewBtn.classList.remove('active');
        } else {
            cardsViewBtn.classList.add('active');
            tableViewBtn.classList.remove('active');
        }
    }

    // Show/hide sort controls (only for cards)
    if (sortControls) {
        sortControls.style.display = mode === 'cards' ? 'flex' : 'none';
    }

    // Re-render current data
    if (cachedData[currentCategory]) {
        renderContent(cachedData[currentCategory]);
    }
}

/**
 * Updates sort UI
 */
function updateSortUI() {
    if (sortBySelect) {
        sortBySelect.value = sortBy;
    }

    if (sortOrderBtn) {
        const icon = sortOrderBtn.querySelector('.sort-icon');
        if (icon) {
            icon.textContent = sortOrder === 'asc' ? '↑' : '↓';
        }
        sortOrderBtn.title = sortOrder === 'asc' ? 'Sort descending' : 'Sort ascending';
    }
}

/**
 * Gets score class for styling
 */
function getScoreClass(score) {
    if (isNaN(score)) return '';
    if (score >= 70) return 'score-high';
    if (score >= 40) return 'score-medium';
    return 'score-low';
}

/**
 * Escapes HTML to prevent XSS
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Returns the empty-state message for the given context.
 */
function getEmptyMessage(searchTerm, filterId, category) {
    if (searchTerm || filterId !== 'all') {
        return 'No items found matching your criteria.';
    }
    return `No ${category} found`;
}

/**
 * Class-based wrapper used by tests.
 */
class ProfilePageManager {
    constructor(config) {
        this.username        = config.username;
        this.currentCategory = config.category || 'games';
        this.currentViewMode = 'table';
        this.cachedData      = {};

        this.tableViewBtn        = document.getElementById('tableViewBtn');
        this.cardsViewBtn        = document.getElementById('cardsViewBtn');
        this.sortControls        = document.getElementById('sort-controls');
        this.tabContentContainer = document.getElementById('tab-content-container');
        this.cardsContainer      = document.getElementById('cards-container');
        this.prevPageBtn         = document.getElementById('prevPage');
        this.nextPageBtn         = document.getElementById('nextPage');
        this.pageDropdown        = document.getElementById('pageDropdown');
        this.pageList            = document.getElementById('pageList');
        this.paginationContainer = document.getElementById('paginationContainer');
    }

    switchView(mode) {
        this.currentViewMode = mode;
        localStorage.setItem(`profile-view-${this.username}`, mode);

        if (this.tableViewBtn && this.cardsViewBtn) {
            this.tableViewBtn.classList.toggle('active', mode === 'table');
            this.cardsViewBtn.classList.toggle('active', mode === 'cards');
        }

        if (this.sortControls) {
            this.sortControls.style.display = mode === 'cards' ? 'flex' : 'none';
        }
    }

    updatePagination(currentPage, totalPages) {
        if (!this.prevPageBtn || !this.nextPageBtn || !this.pageDropdown || !this.pageList) return;

        this.prevPageBtn.disabled = currentPage <= 1;
        this.nextPageBtn.disabled = currentPage >= totalPages;
        this.pageDropdown.textContent = `Page ${currentPage} of ${totalPages}`;

        this.pageList.innerHTML = '';
        for (let i = 1; i <= totalPages; i++) {
            const li = document.createElement('li');
            const a  = document.createElement('a');
            a.href        = '#';
            a.textContent = i;
            a.className   = i === currentPage ? 'active-page' : '';
            li.appendChild(a);
            this.pageList.appendChild(li);
        }

        if (this.paginationContainer) {
            this.paginationContainer.style.display = totalPages <= 1 ? 'none' : 'flex';
        }
    }
}

// Initialize on DOM ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}