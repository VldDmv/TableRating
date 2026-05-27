import { ENTITY_CONFIGS } from './core/config.js';
import { StateManager } from './core/stateManager.js';
import { DataService } from './core/dataService.js';
import { ErrorHandler } from './core/errorHandler.js';
import { htmlUtils, CONSTANTS, securityUtils } from './core/utils.js';

import { PaginationManager } from './features/table/tablePagination.js';
import { SortManager } from './features/table/tableSorting.js';
import { SearchManager } from './features/table/tableSearch.js';
import { FilterManager } from './features/table/tableFiltering.js';
import { initScoreRange } from './features/table/scoreRangeFilter.js';
import { ColumnManager } from './features/table/columnManager.js';
import { InlineEditManager } from './features/table/tableInlineEdit.js';
import { TableRenderer } from './features/table/tableRenderer.js';

import { CardRenderer } from './features/cards/cardRenderer.js';
import { CardInlineEditManager } from './features/cards/cardInlineEdit.js';
import { SortControls } from './features/cards/cardsSorting.js';

import { CoverModal } from './features/cover/coverModal.js';
import { CoverClickHandler } from './features/cover/coverClickHandler.js';

import { ViewToggleManager } from './features/view/viewToggle.js';

import { ItemActionsManager } from './items/itemActions.js';
import { ItemFormManager } from './items/itemForm.js';

import { UniversalAutocomplete } from './features/forms/categoryAutocomplete.js';
import { CollapsibleForm } from './features/forms/collapsibleForm.js';
import { TagChipsManager } from './features/forms/tagchipsManager.js';

class CategoryPageController {
    constructor(config) {
        this.config    = config;
        this.lastItems = [];

        this.initializeElements();
        this.initializeManagers();
        this.setupStateListeners();
    }

    initializeElements() {
        this.elements = {
            tableBody:         document.querySelector(this.config.selectors.tableBody),
            tagFilter:         document.querySelector('#tagFilter'),
            searchInput:       document.querySelector(this.config.selectors.searchBox),
            rowsPerPageSelect: document.querySelector(this.config.selectors.rowsPerPageSelect),
            sortableHeaders:   document.querySelectorAll('thead th'),
            editButton:        document.querySelector(this.config.selectors.editButton),
            addForm:           document.querySelector(this.config.selectors.addForm),
            scoreInput:        document.querySelector(this.config.selectors.scoreInput),
            toggleTagsBtn:          document.getElementById('toggle-tags-btn'),
            tagsCheckboxContainer:  document.querySelector('.tags-checkbox-container')
        };

        if (!this.elements.tableBody) {
            throw new Error('Table body element not found');
        }
    }

    initializeManagers() {
        // ── Cover Modal ───────────────────────────────────────────────────────

        this.coverModal = new CoverModal();

        // ── State ─────────────────────────────────────────────────────────────
        this.stateManager = new StateManager({
            currentPage: 1,
            rowsPerPage: this.elements.rowsPerPageSelect
                ? parseInt(this.elements.rowsPerPageSelect.value)
                : CONSTANTS.DEFAULT_ROWS_PER_PAGE,
            searchTerm: this.elements.searchInput?.value ?? '',
            filterId:   this.elements.tagFilter?.value   ?? 'all',
            minScore:   '',
            maxScore:   '',
            sortBy:    'name',
            sortOrder: 'asc'
        });

        this.dataService = new DataService(this.config);

        // ── Renderers ─────────────────────────────────────────────────────────

        this.tableRenderer = new TableRenderer(this.config, this.elements.tableBody, this.coverModal);
        this.cardRenderer  = new CardRenderer(this.config, this.coverModal);

        // ── Item Actions ──────────────────────────────────────────────────────
        this.itemActionsManager = new ItemActionsManager(
            this.elements.tableBody,
            this.config,
            null
        );

        this.cardRenderer.onRender((cardsContainer) => {
        });

        // ── View Toggle ───────────────────────────────────────────────────────

        this.viewToggleManager = new ViewToggleManager(
            this.config,
            this.tableRenderer,
            this.cardRenderer,
            () => this.lastItems
        );

        // ── Sort Controls (card view) ─────────────────────────────────────────
        this.cardsSorting = new SortControls(this.config, this.stateManager);
        this.cardsSorting.init();

        this.viewToggleManager.onViewChange = (view) => {
            view === 'cards' ? this.cardsSorting.show() : this.cardsSorting.hide();
        };

        this.viewToggleManager.isCardsView()
            ? this.cardsSorting.show()
            : this.cardsSorting.hide();

        // ── Pagination ────────────────────────────────────────────────────────
        this.paginationManager = new PaginationManager({
            prevButton:        document.getElementById('prevPage'),
            nextButton:        document.getElementById('nextPage'),
            rowsPerPageSelect: this.elements.rowsPerPageSelect,
            pageDropdown:      document.getElementById('pageDropdown'),
            pageList:          document.getElementById('pageList')
        });

        this.paginationManager
            .onPageChange((newPage) => this.stateManager.setState({ currentPage: newPage }))
            .onRowsPerPageChange((newSize) => this.stateManager.setState({ rowsPerPage: newSize, currentPage: 1 }))
            .init();

        // ── Sorting ───────────────────────────────────────────────────────────
        this.sortManager = new SortManager(this.elements.sortableHeaders, this.config.columns);
        this.sortManager
            .onSort((sortBy, sortOrder) => this.stateManager.setState({ sortBy, sortOrder }))
            .init();

        // ── Inline Edit ───────────────────────────────────────────────────────
        this.inlineEditManager = new InlineEditManager(this.elements.tableBody, this.config);
        this.inlineEditManager.init();

        this.cardInlineEditManager = new CardInlineEditManager(
            document.getElementById('cards-container'),
            this.config
        );
        this.cardInlineEditManager.init();

        this.itemActionsManager.inlineEditManager = this.inlineEditManager;
        this.itemActionsManager.init();

        // ── Search ────────────────────────────────────────────────────────────
        if (this.elements.searchInput) {
            this.searchManager = new SearchManager(this.elements.searchInput);
            this.searchManager
                .onSearch((searchTerm) => this.stateManager.setState({ searchTerm, currentPage: 1 }))
                .init();
        }

        // ── Filter ────────────────────────────────────────────────────────────
        if (this.elements.tagFilter) {
            this.filterManager = new FilterManager(this.elements.tagFilter);
            this.filterManager
                .onChange((filterId) => this.stateManager.setState({ filterId, currentPage: 1 }))
                .init();
        }

        // ── Score range slider ──────────────────────────────────────────────────
        initScoreRange(({ minScore, maxScore }) =>
            this.stateManager.setState({ minScore, maxScore, currentPage: 1 }));

        // ── Columns ───────────────────────────────────────────────────────────
        this.columnManager = new ColumnManager(this.config);
        this.columnManager.init();

        // ── Form ──────────────────────────────────────────────────────────────
        if (this.elements.addForm && this.elements.scoreInput) {
            this.formManager = new ItemFormManager(
                this.elements.addForm,
                this.elements.scoreInput,
                this.config.validateScore
            );
            this.formManager.init();
            this.initializeAutocomplete();
            this.initializeCollapsibleForm();

            this.elements.addForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handleAddItem();
            });

            this.tagChipsManager = new TagChipsManager(
                'tagChipsDisplay', 'tagDropdown', 'addTagBtn', 'tagSearchInput'
            );
        }

        this.initializeTagsToggle();

        // ── Cover Click Handler ───────────────────────────────────────────────

        this.coverClickHandler = new CoverClickHandler(
            this.elements.tableBody,
            this.config,
            () => this.fetchAndRender()
        );
        this.coverClickHandler.init();

        this.setupSyncHelper();
        this._attachCardsClickHandler();
    }

    // ─── Cards click delegation ───────────────────────────────────────────────

    _attachCardsClickHandler() {
        const cardsContainer = document.getElementById('cards-container');
        if (!cardsContainer) return;

        cardsContainer.addEventListener('click', (event) => {
            // Delete
            const deleteBtn = event.target.closest('.delete-button');
            if (deleteBtn) {
                event.stopImmediatePropagation();
                event.preventDefault();
                const itemName = deleteBtn.dataset.itemName;
                if (confirm(`Are you sure you want to delete "${itemName}"?`)) {
                    this.handleDeleteItem(itemName);
                }
                return;
            }

            // Status toggle
            const statusBtn = event.target.closest('.status-button');
            if (statusBtn) {
                event.stopPropagation();
                event.preventDefault();
                this.handleToggleStatus(statusBtn.dataset.itemName);
                return;
            }

            // Edit (inline)
            const editBtn = event.target.closest('[data-action="edit"]');
            if (editBtn) {
                event.stopPropagation();
                const card = editBtn.closest('.media-card');
                if (card) this.cardInlineEditManager?.toggleCardEdit(card);
                return;
            }

            // Cover Ctrl+Click
            const coverEl = event.target.closest('.card-cover-image, .card-cover-placeholder');
            if (coverEl) {
                const card = event.target.closest('.media-card');
                if (!card) return;

                if (event.ctrlKey || event.metaKey) {
                    event.preventDefault();
                    event.stopPropagation();

                    this.coverClickHandler._showEditPrompt(
                        card.dataset.originalName,
                        card.dataset.originalCoverUrl || ''
                    );
                } else if (coverEl.classList.contains('card-cover-placeholder')) {
                    event.stopPropagation();
                    this.coverClickHandler._showEditPrompt(card.dataset.originalName, '');
                }
            }
        });
    }

    // ─── State ────────────────────────────────────────────────────────────────

    setupStateListeners() {
        this.stateManager.subscribe((oldState, newState) => {
            const changed =
                oldState.currentPage !== newState.currentPage ||
                oldState.rowsPerPage !== newState.rowsPerPage ||
                oldState.searchTerm  !== newState.searchTerm  ||
                oldState.filterId    !== newState.filterId    ||
                oldState.minScore    !== newState.minScore    ||
                oldState.maxScore    !== newState.maxScore    ||
                oldState.sortBy      !== newState.sortBy      ||
                oldState.sortOrder   !== newState.sortOrder;

            if (changed) this.fetchAndRender();
        });
    }

    // ─── Data ─────────────────────────────────────────────────────────────────

    async fetchAndRender() {
        const state = this.stateManager.getState();

        this.viewToggleManager.isTableView()
            ? this.tableRenderer.renderLoading()
            : this.cardRenderer.renderLoading();

        try {
            const pageResult = await this.dataService.fetchData(state);

            this.lastItems = pageResult.items;

            this.stateManager.setState({ currentPage: pageResult.currentPage });
            this.tableRenderer.render(pageResult.items);
            this.cardRenderer.render(pageResult.items);
            this.paginationManager.update(pageResult);
            this.sortManager.updateHeaders(state.sortBy, state.sortOrder);

        } catch (error) {
            ErrorHandler.handle(error, 'Failed to load data', this.elements.tableBody);
        }
    }

    loadInitialData() {
        if (window.initialPageData) {

            this.lastItems = window.initialPageData.items;

            this.tableRenderer.render(window.initialPageData.items);
            this.cardRenderer.render(window.initialPageData.items);
            this.paginationManager.update(window.initialPageData);
            this.config.applyScoreStyling?.(this.elements.tableBody);
        } else {
            this.fetchAndRender();
        }
    }

    // ─── Sync helper ─────────────────────────────────────────────────────────

    setupSyncHelper() {
        window.syncItemUpdate = (oldName, newData) => {

            const item = this.lastItems.find(i => i.name === oldName);
            if (item) {
                item.name     = newData.name;
                item.score    = newData.score;
                item.coverUrl = newData.coverUrl;

                if (item.tags) {
                    const allTags = window.allAvailableTags || [];
                    item.tags = allTags.filter(t => newData.tagIds.includes(t.id));
                } else if (item.genres) {
                    const allGenres = window.allAvailableGenres || [];
                    item.genres = allGenres.filter(g => newData.tagIds.includes(g.id));
                }
            }

            const currentView = this.viewToggleManager?.getCurrentView() ?? 'table';

            const card = document.querySelector(`.media-card[data-original-name="${CSS.escape(oldName)}"]`);
            if (card && currentView === 'table') {
                card.dataset.originalName     = newData.name;
                card.dataset.originalScore    = newData.score;
                card.dataset.originalCoverUrl = newData.coverUrl || '';
                card.dataset.originalTagIds   = newData.tagIds.join(',');
                card.dataset.initialTagIds    = newData.tagIds.join(',');
                if (item) card.dataset.completed = item.completed ? 'true' : 'false';
                this.cardInlineEditManager?.switchToViewMode(card);
            }

            const row = Array.from(this.elements.tableBody.querySelectorAll('tr'))
                .find(r => r.dataset.originalName === oldName);
            if (row && currentView === 'cards') {
                row.dataset.originalName     = newData.name;
                row.dataset.originalScore    = newData.score;
                row.dataset.originalCoverUrl = newData.coverUrl || '';
                row.dataset.originalTagIds   = newData.tagIds.join(',');
                row.dataset.initialTagIds    = newData.tagIds.join(',');
                this.inlineEditManager?.switchToViewRow(row);
            }
        };

        window.syncItemCompleted = (itemName, newCompleted) => {
            const item = this.lastItems.find(i => i.name === itemName);
            if (item) item.completed = newCompleted;

            const card = document.querySelector(`.media-card[data-original-name="${CSS.escape(itemName)}"]`);
            if (card) {
                card.dataset.completed = newCompleted ? 'true' : 'false';
                const btn = card.querySelector('.status-button');
                if (btn) btn.textContent = newCompleted ? '✅' : '❌';
                const label = card.querySelector('.status-label');
                if (label) label.textContent = newCompleted ? 'Completed' : 'Not Completed';
            }
        };
    }

    // ─── Handlers ────────────────────────────────────────────────────────────

    async handleAddItem() {
        if (!this.formManager.handleSubmit()) return;

        const formData     = new FormData(this.elements.addForm);
        const submitButton = this.elements.addForm.querySelector('button[type="submit"]');

        try {
            submitButton.disabled     = true;
            submitButton.textContent  = 'Adding...';

            const csrfToken = securityUtils.getCsrfToken();
            if (!csrfToken) throw new Error('CSRF token not found. Please refresh the page.');

            const categoryKey  = this.config.entityType === 'games' ? 'tagIds' : 'genreIds';
            const allCheckboxes = this.elements.addForm.querySelectorAll('input[type="checkbox"]');
            const selectedIds   = Array.from(allCheckboxes)
                .filter(cb => cb.checked && cb.name === 'selectedIds')
                .map(cb => parseInt(cb.value, 10));

            const requestBody = {
                name:         formData.get('name'),
                score:        parseInt(formData.get('score'), 10),
                coverUrl:     formData.get('coverUrl') || '',
                [categoryKey]: selectedIds
            };

            const response = await fetch(`/api/${this.config.entityType}`, {
                method:  'POST',
                headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrfToken },
                body:    JSON.stringify(requestBody)
            });

            if (!response.ok) {
                throw new Error(await ErrorHandler.parseErrorResponse(response));
            }

            const data = await response.json();

            if (data.message || data.success !== false) {
                this.formManager.reset();

                const coverUrlInput = this.elements.addForm.querySelector('input[name="coverUrl"]');
                if (coverUrlInput) coverUrlInput.value = '';
                allCheckboxes.forEach(cb => { cb.checked = false; });
                this.tagChipsManager?.clearForm();

                await this.fetchAndRender();
            } else {
                throw new Error(data.message || 'Failed to add item.');
            }

        } catch (error) {
            ErrorHandler.handle(error, `Error adding item: ${error.message}`);
        } finally {
            submitButton.disabled    = false;
            submitButton.textContent = `Add ${this.config.entityNameSingular}`;
        }
    }

    async handleDeleteItem(itemName) {
        try {
            const csrfToken = securityUtils.getCsrfToken();
            if (!csrfToken) throw new Error('CSRF token not found. Please refresh the page.');

            const response = await fetch(
                `/api/${this.config.entityType}/${encodeURIComponent(itemName)}`,
                { method: 'DELETE', headers: { 'X-XSRF-TOKEN': csrfToken } }
            );

            if (!response.ok) throw new Error(await ErrorHandler.parseErrorResponse(response));

            await this.fetchAndRender();

        } catch (error) {
            ErrorHandler.handle(error, `Error deleting item: ${error.message}`);
        }
    }

    async handleToggleStatus(itemName) {
        try {
            const csrfToken = securityUtils.getCsrfToken();
            if (!csrfToken) throw new Error('CSRF token not found. Please refresh the page.');

            const response = await fetch(
                `/api/${this.config.entityType}/${encodeURIComponent(itemName)}/toggle`,
                { method: 'PATCH', headers: { 'X-XSRF-TOKEN': csrfToken } }
            );

            if (!response.ok) throw new Error(await ErrorHandler.parseErrorResponse(response));

            const { completed: newCompleted } = await response.json();

            const item = this.lastItems.find(i => i.name === itemName);
            if (item) item.completed = newCompleted;

            const ICON_OK = '✅';
            const ICON_X  = '❌';

            const card = document.querySelector(`.media-card[data-original-name="${CSS.escape(itemName)}"]`);
            if (card) {
                card.dataset.completed = newCompleted ? 'true' : 'false';
                const btn = card.querySelector('.status-button');
                if (btn) btn.textContent = newCompleted ? ICON_OK : ICON_X;
                const label = card.querySelector('.status-label');
                if (label) label.textContent = newCompleted ? 'Completed' : 'Not Completed';
            }

            const row = Array.from(this.elements.tableBody.querySelectorAll('tr'))
                .find(r => r.dataset.originalName === itemName);
            if (row) {
                const btn = row.querySelector('.status-button');
                if (btn) btn.textContent = newCompleted ? ICON_OK : ICON_X;
            }

        } catch (error) {
            ErrorHandler.handle(error, `Error toggling status: ${error.message}`);
        }
    }

    // ─── Autocomplete / Form helpers ──────────────────────────────────────────

    initializeAutocomplete() {
        const nameInput = this.elements.addForm?.querySelector('input[name="name"]');
        if (!nameInput) return;

        new UniversalAutocomplete(nameInput, this.config.entityType);
    }

    initializeCollapsibleForm() {
        if (!this.elements.addForm) return;
        new CollapsibleForm(this.elements.addForm, {
            buttonText: `Add ${this.config.entityNameSingular}`,
            startCollapsed: true
        });
    }

    initializeTagsToggle() {
        if (!this.elements.toggleTagsBtn || !this.elements.tagsCheckboxContainer) return;

        this.elements.toggleTagsBtn.addEventListener('click', () => {
            const isVisible = this.elements.tagsCheckboxContainer.style.display !== 'none';
            this.elements.tagsCheckboxContainer.style.display = isVisible ? 'none' : 'grid';
            this.elements.toggleTagsBtn.textContent = isVisible ? '[+]' : '[-]';
        });
    }

    init() {
        this.loadInitialData();
    }
}

// ─── Entry point ──────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
    const entityType = document.querySelector('meta[name="_entity_type"]')?.getAttribute('content');

    if (!entityType) {
        return;
    }

    const config = ENTITY_CONFIGS[entityType];
    if (!config) {
        return;
    }

    window.categoryConfig = config;

    try {
        const app = new CategoryPageController(config);
        app.init();

    } catch (error) {
        ErrorHandler.handle(error, 'Application initialization failed');
    }
});