/**
 * Main entry point for initializing category page functionality.
 * Refactored to use class-based managers and separation of concerns.
 */

import { getCurrentConfig } from './core/config.js';
import { StateManager } from './core/stateManager.js';
import { DataService } from './core/dataService.js';
import { TableRenderer } from './core/tableRenderer.js';
import { ErrorHandler } from './core/errorHandler.js';
import { htmlUtils, CONSTANTS, securityUtils } from './core/utils.js';

// Import managers
import { PaginationManager } from './features/tablePagination.js';
import { SortManager } from './features/tableSorting.js';
import { SearchManager } from './features/tableSearch.js';
import { FilterManager } from './features/tableFiltering.js';
import { ColumnManager } from './features/columnManager.js';
import { ItemActionsManager } from './items/itemActions.js';
import { ItemFormManager } from './items/itemForm.js';
import { InlineEditManager } from './features/tableGenericInlineEdit.js';

/**
 * Main application controller.
 */
class CategoryPageController {
    /**
     * Creates a new CategoryPageController instance.
     * @param {Object} config - Configuration object.
     */
    constructor(config) {
        this.config = config;
        this.initializeElements();
        this.initializeManagers();
        this.setupStateListeners();
    }

    /**
     * Initializes DOM element references.
     */
    initializeElements() {
        this.elements = {
            tableBody: document.querySelector(this.config.selectors.tableBody),
            tagFilter: document.querySelector('#tagFilter'),
            searchInput: document.querySelector(this.config.selectors.searchBox),
            rowsPerPageSelect: document.querySelector(this.config.selectors.rowsPerPageSelect),
            sortableHeaders: document.querySelectorAll('thead th'),
            editButton: document.querySelector(this.config.selectors.editButton),
            addForm: document.querySelector(this.config.selectors.addForm),
            scoreInput: document.querySelector(this.config.selectors.scoreInput),
            toggleTagsBtn: document.getElementById('toggle-tags-btn'),
            tagsCheckboxContainer: document.querySelector('.tags-checkbox-container')
        };

        if (!this.elements.tableBody) {
            throw new Error('Table body element not found');
        }
    }

    /**
     * Initializes all managers.
     */
    initializeManagers() {
        // State manager
        this.stateManager = new StateManager({
            currentPage: 1,
            rowsPerPage: this.elements.rowsPerPageSelect ?
                parseInt(this.elements.rowsPerPageSelect.value) :
                CONSTANTS.DEFAULT_ROWS_PER_PAGE,
            searchTerm: this.elements.searchInput ? this.elements.searchInput.value : '',
            filterId: this.elements.tagFilter ? this.elements.tagFilter.value : 'all',
            sortBy: 'name',
            sortOrder: 'asc'
        });

        // Data service
        this.dataService = new DataService(this.config);

        // Table renderer
        this.tableRenderer = new TableRenderer(this.config, this.elements.tableBody);

        // Pagination manager
        this.paginationManager = new PaginationManager({
            prevButton: document.getElementById('prevPage'),
            nextButton: document.getElementById('nextPage'),
            rowsPerPageSelect: this.elements.rowsPerPageSelect,
            pageDropdown: document.getElementById('pageDropdown'),
            pageList: document.getElementById('pageList')
        });

        this.paginationManager
            .onPageChange((newPage) => {
                this.stateManager.setState({ currentPage: newPage });
            })
            .onRowsPerPageChange((newSize) => {
                this.stateManager.setState({
                    rowsPerPage: newSize,
                    currentPage: 1
                });
            })
            .init();

        // Sort manager
        this.sortManager = new SortManager(
            this.elements.sortableHeaders,
            this.config.columns
        );

        this.sortManager
            .onSort((newSortBy, newSortOrder) => {
                this.stateManager.setState({
                    sortBy: newSortBy,
                    sortOrder: newSortOrder
                });
            })
            .init();
            //InlineEditManager
            this.inlineEditManager = new InlineEditManager(
            this.elements.tableBody,
            this.config
        );
        this.inlineEditManager.init();
        // Search manager
        if (this.elements.searchInput) {
            this.searchManager = new SearchManager(this.elements.searchInput);
            this.searchManager
                .onSearch((newSearchTerm) => {
                    this.stateManager.setState({
                        searchTerm: newSearchTerm,
                        currentPage: 1
                    });
                })
                .init();
        }

        // Filter manager
        if (this.elements.tagFilter) {
            this.filterManager = new FilterManager(this.elements.tagFilter);
            this.filterManager
                .onChange((newFilterId) => {
                    this.stateManager.setState({
                        filterId: newFilterId,
                        currentPage: 1
                    });
                })
                .init();
        }

        // Column manager
        this.columnManager = new ColumnManager(this.config);
        this.columnManager.init();

        // Item actions manager
       this.itemActionsManager = new ItemActionsManager(
                   this.elements.tableBody,
                   this.config,
                   this.inlineEditManager
               );
               this.itemActionsManager.init();

        // Form manager
        if (this.elements.addForm && this.elements.scoreInput) {
            this.formManager = new ItemFormManager(
                this.elements.addForm,
                this.elements.scoreInput,
                this.config.validateScore
            );
            this.formManager.init();

            this.elements.addForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handleAddItem();
            });
        }



        // Tags toggle
        this.initializeTagsToggle();
    }

    /**
     * Sets up state change listeners.
     */
    setupStateListeners() {
        this.stateManager.subscribe((oldState, newState) => {
            // Only fetch if relevant state changed
            const stateChanged =
                oldState.currentPage !== newState.currentPage ||
                oldState.rowsPerPage !== newState.rowsPerPage ||
                oldState.searchTerm !== newState.searchTerm ||
                oldState.filterId !== newState.filterId ||
                oldState.sortBy !== newState.sortBy ||
                oldState.sortOrder !== newState.sortOrder;

            if (stateChanged) {
                this.fetchAndRender();
            }
        });
    }

    /**
     * Fetches and renders table data.
     */
    async fetchAndRender() {
        const state = this.stateManager.getState();

        this.tableRenderer.renderLoading();

        try {
            const pageResult = await this.dataService.fetchData(state);

            // Update state with server response
            this.stateManager.setState({
                currentPage: pageResult.currentPage
            });

            // Render data
            this.tableRenderer.render(pageResult.items);
            this.paginationManager.update(pageResult);
            this.sortManager.updateHeaders(state.sortBy, state.sortOrder);

        } catch (error) {
            ErrorHandler.handle(
                error,
                'Failed to load data',
                this.elements.tableBody
            );
        }
    }

    /**
     * Initializes tags toggle functionality.
     */
    initializeTagsToggle() {
        if (!this.elements.toggleTagsBtn || !this.elements.tagsCheckboxContainer) {
            return;
        }

        this.elements.toggleTagsBtn.addEventListener('click', () => {
            const isVisible = this.elements.tagsCheckboxContainer.style.display !== 'none';
            this.elements.tagsCheckboxContainer.style.display = isVisible ? 'none' : 'grid';
            this.elements.toggleTagsBtn.textContent = isVisible ? '[+]' : '[-]';
        });
    }

    /**
     * Loads initial data from JSP.
     */
    loadInitialData() {
        const initialDataElement = document.getElementById('initial-page-data');

        if (!initialDataElement) {
            // No initial data, fetch from server
            this.fetchAndRender();
            return;
        }

        try {
            const initialPageResult = JSON.parse(initialDataElement.textContent);
            const state = this.stateManager.getState();

            // Manually set initial state from the loaded data
            this.stateManager.setState({
                currentPage: initialPageResult.currentPage,
                rowsPerPage: initialPageResult.pageSize,

            });

            this.tableRenderer.render(initialPageResult.items);
            this.paginationManager.update(initialPageResult);
            this.sortManager.updateHeaders(state.sortBy, state.sortOrder);

        } catch (error) {
            console.error("Could not parse initial page data from JSP.", error);
            this.fetchAndRender(); // Fallback to fetching
        }
    }

    /**
     * Handles the "Add Item" form submission via AJAX.
     */
    async handleAddItem() {
        if (!this.formManager.handleSubmit()) {
            // Validation failed in ItemFormManager
            return;
        }

        const formData = new FormData(this.elements.addForm);
        const submitButton = this.elements.addForm.querySelector('button[type="submit"]');

        try {
            submitButton.disabled = true;
            submitButton.textContent = 'Adding...';

            const response = await fetch(this.config.entityType, {
                method: 'POST',
                body: new URLSearchParams(formData) // FormData includes the CSRF token
            });

            if (!response.ok) {
                const errorMsg = await ErrorHandler.parseErrorResponse(response);
                throw new Error(errorMsg);
            }

            const data = await response.json();

            if (data.success) {
                this.formManager.reset(); // Clear the form
                this.fetchAndRender();    // Refresh the table
            } else {
                throw new Error(data.message || "Failed to add item.");
            }

        } catch (error) {
            // Handle duplicate entry and other errors
            ErrorHandler.handle(error, `Error adding item: ${error.message}`);
            alert(`Error adding item: ${error.message}`); // Show a clear alert
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = `Add ${this.config.entityNameSingular}`;
        }
    }

    /**
     * Initializes the application.
     */
    init() {
        this.loadInitialData();
    }
}

/**
 * Application entry point.
 */
document.addEventListener('DOMContentLoaded', () => {
    const config = getCurrentConfig();

    if (!config) {
        console.error('Configuration not found for current page');
        return;
    }

    try {
        const app = new CategoryPageController(config);
        app.init();
    } catch (error) {
        console.error('Failed to initialize application:', error);
        ErrorHandler.handle(error, 'Application initialization failed');
    }
});