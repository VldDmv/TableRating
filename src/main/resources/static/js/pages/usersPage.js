export { UsersPageManager, getAvatarColor };

import { htmlUtils } from '../tableScripts/core/utils.js';
import { getAvatarColor } from '../shared/avatarUtils.js';

class UsersPageManager {
    constructor() {
        this.currentPage = 1;
        this.pageSize = 18;
        this.searchTerm = '';
        this.sortBy = 'totalItems';
        this.sortOrder = 'desc';
        this.totalPages = 1;
        this.totalUsers = 0;
        this.currentView = localStorage.getItem('usersView') || 'cards';

        this.usersGrid = document.getElementById('usersGrid');
        this.usersList = document.getElementById('usersList');
        this.searchInput = document.getElementById('userSearch');
        this.sortBySelect = document.getElementById('sortBy');
        this.sortOrderBtn = document.getElementById('sortOrderBtn');
        this.cardsViewBtn = document.getElementById('cardsViewBtn');
        this.listViewBtn = document.getElementById('listViewBtn');
        this.prevPageBtn = document.getElementById('prevPage');
        this.nextPageBtn = document.getElementById('nextPage');
        this.pageDropdown = document.getElementById('pageDropdown');
        this.pageList = document.getElementById('pageList');
        this.emptyState = document.getElementById('emptyState');
        this.totalUsersElement = document.getElementById('totalUsers');
        this.paginationContainer = document.getElementById('paginationContainer');

        this.searchDebounceTimer = null;

        this.init();
    }

    init() {
        this.attachEventListeners();
        this.restoreView();
        this.loadUsers();
    }

    // ─── Events ───────────────────────────────────────────────────────────────

    attachEventListeners() {
        this.searchInput.addEventListener('input', () => {
            clearTimeout(this.searchDebounceTimer);
            this.searchDebounceTimer = setTimeout(() => {
                this.searchTerm = this.searchInput.value.trim();
                this.currentPage = 1;
                this.loadUsers();
            }, 500);
        });

        this.sortBySelect.addEventListener('change', () => {
            this.sortBy = this.sortBySelect.value;
            this.currentPage = 1;
            this.loadUsers();
        });

        this.sortOrderBtn.addEventListener('click', () => {
            this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc';
            this.sortOrderBtn.classList.toggle('desc', this.sortOrder === 'desc');
            this.sortOrderBtn.querySelector('.sort-icon').textContent =
                this.sortOrder === 'asc' ? '↑' : '↓';
            this.loadUsers();
        });

        this.cardsViewBtn.addEventListener('click', () => this.switchView('cards'));
        this.listViewBtn.addEventListener('click', () => this.switchView('list'));

        this.prevPageBtn.addEventListener('click', () => {
            if (this.currentPage > 1) {
                this.currentPage--;
                this.loadUsers();
            }
        });

        this.nextPageBtn.addEventListener('click', () => {
            if (this.currentPage < this.totalPages) {
                this.currentPage++;
                this.loadUsers();
            }
        });

        this.pageDropdown.addEventListener('click', (e) => {
            e.stopPropagation();
            this.pageList.style.display =
                this.pageList.style.display === 'block' ? 'none' : 'block';
        });

        document.addEventListener('click', () => {
            this.pageList.style.display = 'none';
        });
    }

    // ─── View ─────────────────────────────────────────────────────────────────

    switchView(view) {
        this.currentView = view;
        this._applyView(view);
        this.loadUsers();
    }

    restoreView() {
        this._applyView(this.currentView);
    }

    toggleSortOrder() {
        this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc';
        this.sortOrderBtn.classList.toggle('desc', this.sortOrder === 'desc');
        this.sortOrderBtn.querySelector('.sort-icon').textContent =
            this.sortOrder === 'asc' ? '↑' : '↓';
    }

    _applyView(view) {
        localStorage.setItem('usersView', view);
        const isCards = view === 'cards';

        this.cardsViewBtn.classList.toggle('active', isCards);
        this.listViewBtn.classList.toggle('active', !isCards);
        this.usersGrid.style.display = isCards ? 'grid' : 'none';
        this.usersList.style.display = isCards ? 'none' : 'flex';
    }

    // ─── Data ─────────────────────────────────────────────────────────────────

    async loadUsers() {
        try {
            this.showLoading();

            const params = new URLSearchParams({
                page: this.currentPage,
                size: this.pageSize,
                sortBy: this.sortBy,
                sortOrder: this.sortOrder,
            });

            if (this.searchTerm) params.append('search', this.searchTerm);

            const response = await fetch(`/api/users?${params}`);
            if (!response.ok) throw new Error('Failed to load users');

            const data = await response.json();

            this.totalPages = data.totalPages;
            this.totalUsers = data.totalItems;

            this.renderUsers(data.items);
            this.updatePagination();
            this.updateStats();
        } catch (error) {
            console.error('Error loading users:', error);
            this.showError('Failed to load users. Please try again.');
        }
    }

    // ─── Render ───────────────────────────────────────────────────────────────

    showLoading() {
        const html = `
            <div class="loading-state">
                <div class="spinner"></div>
                <p>Loading users...</p>
            </div>
        `;
        this._getActiveContainer().innerHTML = html;
        this.emptyState.style.display = 'none';
    }

    showError(message) {
        this._getActiveContainer().innerHTML = `
            <div class="loading-state">
                <p style="color: var(--color-danger);">⚠️ ${htmlUtils.escape(message)}</p>
            </div>
        `;
    }

    renderUsers(users) {
        if (!users || users.length === 0) {
            this.usersGrid.innerHTML = '';
            this.usersList.innerHTML = '';
            this.emptyState.style.display = 'block';
            this.paginationContainer.style.display = 'none';
            return;
        }

        this.emptyState.style.display = 'none';
        this.paginationContainer.style.display = 'flex';

        if (this.currentView === 'cards') {
            this._renderCards(users);
        } else {
            this._renderList(users);
        }
    }

    _renderCards(users) {
        this.usersGrid.innerHTML = users.map((u) => this._createUserCard(u)).join('');

        this.usersGrid.querySelectorAll('.user-card').forEach((card) => {
            card.addEventListener('click', () => {
                window.location.href = `/profile?username=${encodeURIComponent(card.dataset.username)}`;
            });
        });
    }

    _renderList(users) {
        this.usersList.innerHTML = users.map((u) => this._createUserListItem(u)).join('');

        this.usersList.querySelectorAll('.user-list-item').forEach((item) => {
            item.addEventListener('click', () => {
                window.location.href = `/profile?username=${encodeURIComponent(item.dataset.username)}`;
            });
        });
    }

    _createUserCard(user) {
        const initial = user.name.charAt(0).toUpperCase();
        const joinDate = user.createdAt ? this._formatDate(user.createdAt) : 'Unknown';

        return `
            <div class="user-card" data-username="${htmlUtils.escape(user.name)}">
                <span class="total-items-badge">${user.totalItems} items</span>

                <div class="user-card-header">
                    <div class="user-avatar" style="background: ${getAvatarColor(user.name)}">
                        ${initial}
                    </div>
                    <div class="user-info">
                        <div class="user-name">${htmlUtils.escape(user.name)}</div>
                        <div class="user-joined">📅 ${joinDate}</div>
                    </div>
                </div>

                <div class="user-stats">
                    ${this._createStatBox('🎮', user.gamesCount, 'Games')}
                    ${this._createStatBox('🎬', user.moviesCount, 'Movies')}
                    ${this._createStatBox('📚', user.booksCount, 'Books')}
                    ${this._createStatBox('📺', user.showsCount, 'Shows')}
                </div>
            </div>
        `;
    }

    _createUserListItem(user) {
        const initial = user.name.charAt(0).toUpperCase();
        const joinDate = user.createdAt ? this._formatDate(user.createdAt) : 'Unknown';

        return `
            <div class="user-list-item" data-username="${htmlUtils.escape(user.name)}">
                <div class="user-avatar" style="background: ${getAvatarColor(user.name)}">
                    ${initial}
                </div>
                <div class="user-list-info">
                    <div class="user-list-name-row">
                        <div class="user-list-name">${htmlUtils.escape(user.name)}</div>
                        <div class="user-list-joined">📅 Joined ${joinDate}</div>
                    </div>
                    <div class="user-list-stats">
                        ${this._createListStat('🎮', user.gamesCount, 'Games')}
                        ${this._createListStat('🎬', user.moviesCount, 'Movies')}
                        ${this._createListStat('📚', user.booksCount, 'Books')}
                        ${this._createListStat('📺', user.showsCount, 'Shows')}
                    </div>
                </div>
                <div class="user-list-total">${user.totalItems} items</div>
            </div>
        `;
    }

    _createStatBox(icon, count, label) {
        return `
            <div class="stat-box">
                <span class="stat-box-icon">${icon}</span>
                <div class="stat-box-content">
                    <div class="stat-box-value">${count}</div>
                    <div class="stat-box-label">${label}</div>
                </div>
            </div>
        `;
    }

    _createListStat(icon, count, label) {
        return `
            <span class="user-list-stat">
                <span class="user-list-stat-icon">${icon}</span>
                ${count} ${label}
            </span>
        `;
    }

    // ─── Pagination ───────────────────────────────────────────────────────────

    updatePagination() {
        this.prevPageBtn.disabled = this.currentPage <= 1;
        this.nextPageBtn.disabled = this.currentPage >= this.totalPages;
        this.pageDropdown.textContent = `Page ${this.currentPage} of ${this.totalPages}`;

        this.pageList.innerHTML = '';
        for (let i = 1; i <= this.totalPages; i++) {
            const li = document.createElement('li');
            const a = document.createElement('a');
            a.href = '#';
            a.textContent = i;
            a.className = i === this.currentPage ? 'active-page' : '';
            a.addEventListener('click', (e) => {
                e.preventDefault();
                this.currentPage = i;
                this.pageList.style.display = 'none';
                this.loadUsers();
            });
            li.appendChild(a);
            this.pageList.appendChild(li);
        }
    }

    updateStats() {
        this.totalUsersElement.textContent = this.totalUsers;
    }

    // ─── Utils ────────────────────────────────────────────────────────────────

    _getActiveContainer() {
        return this.currentView === 'cards' ? this.usersGrid : this.usersList;
    }

    _formatDate(dateString) {
        if (!dateString) return 'Unknown';
        return new Date(dateString).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new UsersPageManager();
});
