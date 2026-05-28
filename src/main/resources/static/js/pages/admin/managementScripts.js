/**
 * Management Scripts (Tags / Genres)
 * Handles modals, search, filters, and CRUD operations.
 */
export { ManagementPage };
import { ToastService } from '../../shared/toast.js';

class ManagementPage {
    constructor() {
        this.currentView = localStorage.getItem('managementView') || 'cards';
        this.currentFilter = 'all';

        this.toast = new ToastService('toastContainer');

        this.searchInput = document.getElementById('managementSearch');
        this.editModal = document.getElementById('editModal');
        this.deleteModal = document.getElementById('deleteModal');
        this.emptyState = document.getElementById('emptyState');
        this.itemsCards = document.getElementById('itemsCards');
        this.itemsTable = document.getElementById('itemsTable');
        this.cardsViewBtn = document.getElementById('cardsViewBtn');
        this.tableViewBtn = document.getElementById('tableViewBtn');
        this.filterBadges = document.querySelectorAll('.filter-badge');

        this.init();
    }

    init() {
        this._applyView(this.currentView);
        this.attachEventListeners();
        this.toast.checkUrlMessages();
    }

    // ─── Events ───────────────────────────────────────────────────────────────

    attachEventListeners() {
        this.cardsViewBtn?.addEventListener('click', () => this.switchView('cards'));
        this.tableViewBtn?.addEventListener('click', () => this.switchView('table'));

        this.filterBadges.forEach((badge) =>
            badge.addEventListener('click', (e) => this.setFilter(e.currentTarget.dataset.filter))
        );

        this.searchInput?.addEventListener('input', () => this.applyFilters());

        this.editModal?.addEventListener('click', (e) => {
            if (e.target === this.editModal) this.closeEditModal();
        });
        this.deleteModal?.addEventListener('click', (e) => {
            if (e.target === this.deleteModal) this.closeDeleteModal();
        });
    }

    // ─── View ─────────────────────────────────────────────────────────────────

    switchView(view) {
        this.currentView = view;
        this._applyView(view);
    }

    _applyView(view) {
        localStorage.setItem('managementView', view);
        const isCards = view === 'cards';
        this.cardsViewBtn?.classList.toggle('active', isCards);
        this.tableViewBtn?.classList.toggle('active', !isCards);
        this.itemsCards.style.display = isCards ? 'grid' : 'none';
        this.itemsTable.style.display = isCards ? 'none' : 'block';
    }

    // ─── Filter ───────────────────────────────────────────────────────────────

    setFilter(filter) {
        this.currentFilter = filter;
        this.filterBadges.forEach((badge) =>
            badge.classList.toggle('active', badge.dataset.filter === filter)
        );
        this.applyFilters();
    }

    applyFilters(searchTerm, filter, view) {
        const term = (
            searchTerm !== undefined ? searchTerm : (this.searchInput?.value.trim() ?? '')
        ).toLowerCase();
        const activeFilter = filter !== undefined ? filter : this.currentFilter;
        const activeView = view !== undefined ? view : this.currentView;

        let visibleCount = 0;

        this.itemsCards.querySelectorAll('.management-item').forEach((card) => {
            const visible = this._itemMatchesFilters(
                card.querySelector('.item-name').textContent.toLowerCase(),
                card.querySelector('.item-media-types'),
                term,
                activeFilter
            );
            card.style.display = visible ? '' : 'none';
            if (visible) visibleCount++;
        });

        this.itemsTable.querySelectorAll('tbody tr').forEach((row) => {
            const visible = this._itemMatchesFilters(
                row.cells[1].textContent.toLowerCase(),
                row.querySelector('.table-media-types'),
                term,
                activeFilter
            );
            row.style.display = visible ? '' : 'none';
        });

        const isEmpty = visibleCount === 0;
        this.emptyState.style.display = isEmpty ? 'block' : 'none';

        if (!isEmpty) {
            this._applyView(activeView);
        } else {
            this.itemsCards.style.display = 'none';
            this.itemsTable.style.display = 'none';
        }
    }

    _itemMatchesFilters(itemName, mediaTypesEl, searchTerm, filter = this.currentFilter) {
        const matchesSearch = itemName.includes(searchTerm) || searchTerm === '';

        if (filter === 'all' || !mediaTypesEl) {
            return matchesSearch;
        }

        const badges = mediaTypesEl.querySelectorAll('.media-badge');
        const matchesFilter = Array.from(badges).some(
            (b) => b.textContent.toLowerCase() === filter.toLowerCase()
        );

        return matchesSearch && matchesFilter;
    }

    // ─── Modals ───────────────────────────────────────────────────────────────

    closeEditModal() {
        this.editModal.classList.remove('show');
        this.editModal.style.display = 'none';
    }

    closeDeleteModal() {
        this.deleteModal.classList.remove('show');
        this.deleteModal.style.display = 'none';
    }

    editItem(button) {
        const { id: itemId, name: itemName, mediaTypes } = button.dataset;

        document.getElementById('edit-id').value = itemId;
        document.getElementById('edit-name').value = itemName;

        if (mediaTypes) {
            const types = mediaTypes.split(',');
            document.querySelectorAll('#edit-media-types input[type="checkbox"]').forEach((cb) => {
                cb.checked = types.includes(cb.value);
            });
        }

        const modal = document.getElementById('editModal');
        modal.style.display = 'flex';
        setTimeout(() => modal.classList.add('show'), 10);
    }

    deleteItem(button) {
        const { id: itemId, name: itemName } = button.dataset;

        document.getElementById('delete-id').value = itemId;
        document.getElementById('delete-name').textContent = itemName;

        const modal = document.getElementById('deleteModal');
        modal.style.display = 'flex';
        setTimeout(() => modal.classList.add('show'), 10);
    }
}

// ─── Global functions for inline onclick handlers ─────────────────────────────

let managementPage;

function editItem(button) {
    const { id: itemId, name: itemName, mediaTypes } = button.dataset;

    document.getElementById('edit-id').value = itemId;
    document.getElementById('edit-name').value = itemName;

    if (mediaTypes) {
        const types = mediaTypes.split(',');
        document.querySelectorAll('#edit-media-types input[type="checkbox"]').forEach((cb) => {
            cb.checked = types.includes(cb.value);
        });
    }

    const modal = document.getElementById('editModal');
    modal.style.display = 'flex';
    setTimeout(() => modal.classList.add('show'), 10);
}

function closeEditModal() {
    managementPage?.closeEditModal();
}
function closeDeleteModal() {
    managementPage?.closeDeleteModal();
}

function deleteItem(button) {
    const { id: itemId, name: itemName } = button.dataset;

    document.getElementById('delete-id').value = itemId;
    document.getElementById('delete-name').textContent = itemName;

    const modal = document.getElementById('deleteModal');
    modal.style.display = 'flex';
    setTimeout(() => modal.classList.add('show'), 10);
}

document.addEventListener('DOMContentLoaded', () => {
    managementPage = new ManagementPage();

    window.editItem = editItem;
    window.closeEditModal = closeEditModal;
    window.deleteItem = deleteItem;
    window.closeDeleteModal = closeDeleteModal;
});
