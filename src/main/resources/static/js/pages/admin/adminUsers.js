/**
 * Admin Users Page
 * Handles view toggle, modals, live search, and avatar colors.
 */
 export { AdminUsersManager }
import { ToastService } from '../../shared/toast.js';
import { getAvatarColor } from '../../shared/avatarUtils.js';

class AdminUsersManager {
    constructor() {
        this.currentView = localStorage.getItem('adminUsersView') || 'cards';
        this.searchDebounceTimer = null;

        this.toast = new ToastService('toastContainer');

        this.cardsViewBtn = document.getElementById('cardsViewBtn');
        this.tableViewBtn = document.getElementById('tableViewBtn');
        this.usersCards   = document.getElementById('usersCards');
        this.usersTable   = document.getElementById('usersTable');
        this.liveSearch   = document.getElementById('liveSearch');
        this.emptyState   = document.getElementById('emptyState');
        this.editModal    = document.getElementById('editModal');
        this.deleteModal  = document.getElementById('deleteModal');

        this.init();
    }

    init() {
        this.restoreView();
        this.attachEventListeners();
        this.updateAdminCount();
        this.generateAvatarColors();
        this.toast.checkUrlMessages();
    }

    attachEventListeners() {
        this.cardsViewBtn?.addEventListener('click', () => this.switchView('cards'));
        this.tableViewBtn?.addEventListener('click', () => this.switchView('table'));

        this.liveSearch?.addEventListener('input', (e) => {
            clearTimeout(this.searchDebounceTimer);
            this.searchDebounceTimer = setTimeout(
                () => this.performSearch(e.target.value),
                500
            );
        });

        this.editModal?.addEventListener('click',  (e) => { if (e.target === this.editModal)  this.closeEditModal();  });
        this.deleteModal?.addEventListener('click', (e) => { if (e.target === this.deleteModal) this.closeDeleteModal(); });

        document.getElementById('editRoleForm')
            ?.addEventListener('submit', () => this.toast.show('Saving changes...', 'info', '⏳'));

        document.getElementById('deleteUserForm')
            ?.addEventListener('submit', () => this.toast.show('Deleting user...', 'info', '⏳'));
    }

    // ─── View ─────────────────────────────────────────────────────────────────

    switchView(view) {
        this.currentView = view;
        this._applyView(view);
    }

    restoreView() {
        this._applyView(this.currentView);
    }

    _applyView(view) {
        localStorage.setItem('adminUsersView', view);
        const isCards = view === 'cards';
        this.cardsViewBtn?.classList.toggle('active',  isCards);
        this.tableViewBtn?.classList.toggle('active', !isCards);
        this.usersCards.style.display = isCards ? 'grid'  : 'none';
        this.usersTable.style.display = isCards ? 'none'  : 'block';
    }

    editUser(userId) {
        const card = document.querySelector(`.user-card[data-user-id="${userId}"]`);
        if (!card) return;

        document.getElementById('editUserId').value = userId;
        document.getElementById('editUsername').textContent = card.dataset.username;

        const isAdmin = card.dataset.role === 'ADMIN';
        document.getElementById('roleAdmin').checked = isAdmin;
        document.getElementById('roleUser').checked  = !isAdmin;

        const modal = document.getElementById('editModal');
        modal.style.display = 'flex';
        setTimeout(() => modal.classList.add('show'), 10);
    }

    deleteUser(userId, username) {
        document.getElementById('deleteUserId').value = userId;
        document.getElementById('deleteUsername').textContent = username;

        const modal = document.getElementById('deleteModal');
        modal.style.display = 'flex';
        setTimeout(() => modal.classList.add('show'), 10);
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    performSearch(searchTerm) {
        const trimmed = searchTerm.trim().toLowerCase();

        const cards = this.usersCards.querySelectorAll('.user-card');
        let visibleCount = 0;

        cards.forEach(card => {
            const matches = card.dataset.username.toLowerCase().includes(trimmed) || trimmed === '';
            card.style.display = matches ? '' : 'none';
            if (matches) visibleCount++;
        });

        this.usersTable.querySelectorAll('tbody tr').forEach(row => {
            const matches = row.cells[1].textContent.toLowerCase().includes(trimmed) || trimmed === '';
            row.style.display = matches ? '' : 'none';
        });

        const isEmpty = visibleCount === 0 && trimmed !== '';
        this.emptyState.style.display = isEmpty ? 'block' : 'none';
        this.usersCards.style.display = isEmpty ? 'none' : (this.currentView === 'cards' ? 'grid'  : 'none');
        this.usersTable.style.display = isEmpty ? 'none' : (this.currentView === 'table' ? 'block' : 'none');

        this.updateAdminCount();
    }

    // ─── Stats ────────────────────────────────────────────────────────────────

    updateAdminCount() {
        const allCards     = document.querySelectorAll('.user-card');
        const visibleCards = Array.from(allCards).filter(c => c.style.display !== 'none');
        const source       = visibleCards.length > 0 ? visibleCards : Array.from(allCards);

        const adminCount = source.filter(c => c.dataset.role === 'ADMIN').length;

        const adminCountEl = document.getElementById('adminCount');
        const totalCountEl = document.getElementById('totalUsersCount');
        if (adminCountEl) adminCountEl.textContent = adminCount;
        if (totalCountEl) totalCountEl.textContent = source.length;
    }

    // ─── Avatars ──────────────────────────────────────────────────────────────

    generateAvatarColors() {
        document.querySelectorAll('.user-card-avatar').forEach(avatar => {
            avatar.style.background = getAvatarColor(avatar.dataset.username);
        });
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
}

// ─── Global functions for inline onclick handlers ─────────────────────────────

let adminManager;

function editUser(userId) {
    const card = document.querySelector(`.user-card[data-user-id="${userId}"]`);
    if (!card) return;

    document.getElementById('editUserId').value = userId;
    document.getElementById('editUsername').textContent = card.dataset.username;

    const isAdmin = card.dataset.role === 'ADMIN';
    document.getElementById('roleAdmin').checked = isAdmin;
    document.getElementById('roleUser').checked  = !isAdmin;

    const modal = document.getElementById('editModal');
    modal.style.display = 'flex';
    setTimeout(() => modal.classList.add('show'), 10);
}

function closeEditModal()   { adminManager?.closeEditModal();   }
function closeDeleteModal() { adminManager?.closeDeleteModal(); }

function deleteUser(userId, username) {
    document.getElementById('deleteUserId').value = userId;
    document.getElementById('deleteUsername').textContent = username;

    const modal = document.getElementById('deleteModal');
    modal.style.display = 'flex';
    setTimeout(() => modal.classList.add('show'), 10);
}

document.addEventListener('DOMContentLoaded', () => {
    adminManager = new AdminUsersManager();

    window.editUser         = editUser;
    window.closeEditModal   = closeEditModal;
    window.deleteUser       = deleteUser;
    window.closeDeleteModal = closeDeleteModal;
});