/**
 * View Toggle Manager
* Controls switching between Table and Card views.
 */

export class ViewToggleManager {
   /**
   * @param {Object} config
   * @param {Object} tableRenderer
   * @param {Object} cardRenderer
   * @param {Function} getItems — a callback returning the current items array for the cards view
   */
    constructor(config, tableRenderer, cardRenderer, getItems) {
        this.config         = config;
        this.tableRenderer  = tableRenderer;
        this.cardRenderer   = cardRenderer;
        this.getItems       = getItems;

        if (typeof getItems !== 'function') {
            console.warn('[ViewToggle] getItems not passed - cards view may be empty when toggled');
        }

        this.tableContainer = document.querySelector('table');
        this.storageKey     = `view-preference-${config.entityType}`;
        this.currentView    = this._loadPreference();
        this.onViewChange   = null;

        this.init();
    }

    init() {
        this._createToggleButton();
        this.applyView(this.currentView);
    }

    // ─── Public API ────────────────────────────────────────────────────────

    switchView(view) {
        if (view !== 'table' && view !== 'cards') {
            console.error('[ViewToggle] Unknown type:', view);
            return;
        }

        this.currentView = view;
        this._savePreference(view);
        this.applyView(view);
        this._updateActiveButton();

    }

    applyView(view) {
        view === 'table' ? this._showTable() : this._showCards();
        this.onViewChange?.(view);
    }

    getCurrentView()  { return this.currentView; }
    isCardsView()     { return this.currentView === 'cards'; }
    isTableView()     { return this.currentView === 'table'; }

    // ─── Private methods ─────────────────────────────────────────────────────

    _showTable() {
        if (this.tableContainer) {
            this.tableContainer.style.display = 'table';
        }

        const cardsContainer  = document.getElementById('cards-container');
        const columnControls  = document.getElementById('column-toggle-container');

        if (cardsContainer) cardsContainer.style.display  = 'none';
        if (columnControls) columnControls.style.display  = 'flex';
    }

    _showCards() {
        if (this.tableContainer) {
            this.tableContainer.style.display = 'none';
        }

        const columnControls = document.getElementById('column-toggle-container');
        if (columnControls) columnControls.style.display = 'none';

        let cardsContainer = document.getElementById('cards-container');
        if (!cardsContainer) {
            cardsContainer = document.createElement('div');
            cardsContainer.id        = 'cards-container';
            cardsContainer.className = 'cards-grid';

            this.tableContainer?.parentNode?.insertBefore(
                cardsContainer,
                this.tableContainer.nextSibling
            );
        }
        cardsContainer.style.display = 'grid';

        const items = this.getItems?.();
        if (items) {
            this.cardRenderer.render(items);
        }
    }

    _createToggleButton() {
        const tableControls = document.querySelector('.table-controls');
        if (!tableControls) {
            console.warn('[ViewToggle] .table-controls not found');
            return;
        }

        const container = document.createElement('div');
        container.className = 'view-toggle-container';
        container.innerHTML = `
            <label class="view-toggle-label">
                View:
                <div class="view-toggle-buttons">
                    <button type="button" class="view-toggle-btn" data-view="table" title="Table View">
                        <span class="view-icon">📋</span>
                        <span class="view-label">Table</span>
                    </button>
                    <button type="button" class="view-toggle-btn" data-view="cards" title="Card View">
                        <span class="view-icon">🎴</span>
                        <span class="view-label">Cards</span>
                    </button>
                </div>
            </label>
        `;

        container.querySelectorAll('.view-toggle-btn').forEach(btn => {
            btn.addEventListener('click', () => this.switchView(btn.dataset.view));
        });

        tableControls.appendChild(container);
        this._updateActiveButton();
    }

    _updateActiveButton() {
        document.querySelectorAll('.view-toggle-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.view === this.currentView);
        });
    }

    _loadPreference() {
        const saved = localStorage.getItem(this.storageKey);
        return saved === 'cards' || saved === 'table' ? saved : 'table';
    }

    _savePreference(view) {
        localStorage.setItem(this.storageKey, view);
    }
}