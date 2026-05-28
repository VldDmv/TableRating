/**
 * Manages pagination controls for category tables.
 */

export class PaginationManager {
    /**
     * Creates a new PaginationManager instance.
     * @param {Object} elements - DOM elements for pagination.
     */
    constructor(elements) {
        this.prevButton = elements.prevButton;
        this.nextButton = elements.nextButton;
        this.rowsPerPageSelect = elements.rowsPerPageSelect;
        this.pageDropdown = elements.pageDropdown;
        this.pageList = elements.pageList;

        this.paginationContainer = this.prevButton?.closest('.pagination-controls-container');

        this.currentPage = 1;
        this.totalPages = 1;

        this.pageChangeListeners = [];
        this.rowsChangeListeners = [];
    }

    /**
     * Subscribes to page change events.
     * @param {Function} callback - Callback(newPage) => void.
     * @returns {PaginationManager} This instance for chaining.
     */
    onPageChange(callback) {
        this.pageChangeListeners.push(callback);
        return this;
    }

    /**
     * Subscribes to rows per page change events.
     * @param {Function} callback - Callback(newRowsPerPage) => void.
     * @returns {PaginationManager} This instance for chaining.
     */
    onRowsPerPageChange(callback) {
        this.rowsChangeListeners.push(callback);
        return this;
    }

    /**
     * Initializes pagination controls.
     */
    init() {
        this.initPrevButton();
        this.initNextButton();
        this.initRowsPerPageSelect();
        this.initPageDropdown();
    }

    /**
     * Initializes previous page button.
     */
    initPrevButton() {
        if (!this.prevButton) return;

        this.prevButton.addEventListener('click', () => {
            if (this.currentPage > 1) {
                this.pageChangeListeners.forEach((callback) => callback(this.currentPage - 1));
            }
        });
    }

    /**
     * Initializes next page button.
     */
    initNextButton() {
        if (!this.nextButton) return;

        this.nextButton.addEventListener('click', () => {
            if (this.currentPage < this.totalPages) {
                this.pageChangeListeners.forEach((callback) => callback(this.currentPage + 1));
            }
        });
    }

    /**
     * Initializes rows per page select.
     */
    initRowsPerPageSelect() {
        if (!this.rowsPerPageSelect) return;

        this.rowsPerPageSelect.addEventListener('change', () => {
            const newRows = parseInt(this.rowsPerPageSelect.value, 10);
            this.rowsChangeListeners.forEach((callback) => callback(newRows));
        });
    }

    /**
     * Initializes page dropdown and list.
     */
    initPageDropdown() {
        if (!this.pageDropdown || !this.pageList) return;

        this.pageDropdown.addEventListener('click', (e) => {
            e.stopPropagation();
            const isVisible = this.pageList.style.display === 'block';
            this.pageList.style.display = isVisible ? 'none' : 'block';
        });

        this.pageList.addEventListener('click', (e) => {
            if (e.target.tagName === 'A') {
                e.preventDefault();
                this.pageList.style.display = 'none';
                const page = parseInt(e.target.dataset.page, 10);
                this.pageChangeListeners.forEach((callback) => callback(page));
            }
        });

        // Close dropdown when clicking outside
        document.addEventListener('click', (e) => {
            if (
                this.pageList.style.display === 'block' &&
                !this.pageDropdown.contains(e.target) &&
                !this.pageList.contains(e.target)
            ) {
                this.pageList.style.display = 'none';
            }
        });
    }

    /**
     * Updates pagination display with new data.
     * @param {Object} pageResult - Page result with currentPage and totalPages.
     */
    update(pageResult) {
        this.currentPage = pageResult.currentPage;
        this.totalPages = pageResult.totalPages;

        this.updateButtons();
        this.updateDropdown();
        this.updatePageList();

        if (this.paginationContainer) {
            if (this.totalPages <= 1) {
                this.paginationContainer.style.display = 'none';
            } else {
                this.paginationContainer.style.display = 'flex';
            }
        }
    }

    /**
     * Updates button states.
     */
    updateButtons() {
        if (this.prevButton) {
            this.prevButton.disabled = this.currentPage <= 1;
        }
        if (this.nextButton) {
            this.nextButton.disabled = this.currentPage >= this.totalPages;
        }
    }

    /**
     * Updates dropdown text.
     */
    updateDropdown() {
        if (this.pageDropdown) {
            this.pageDropdown.textContent = `Page ${this.currentPage} of ${this.totalPages}`;
        }
    }

    /**
     * Updates page list dropdown.
     */
    updatePageList() {
        if (!this.pageList) return;

        this.pageList.innerHTML = '';

        for (let i = 1; i <= this.totalPages; i++) {
            const li = document.createElement('li');
            const a = document.createElement('a');
            a.href = '#';
            a.textContent = i;
            a.dataset.page = i;

            if (i === this.currentPage) {
                a.classList.add('active-page');
            }

            li.appendChild(a);
            this.pageList.appendChild(li);
        }
    }

    /**
     * Gets current page number.
     * @returns {number} Current page.
     */
    getCurrentPage() {
        return this.currentPage;
    }

    /**
     * Gets total pages count.
     * @returns {number} Total pages.
     */
    getTotalPages() {
        return this.totalPages;
    }
}
