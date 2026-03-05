/**
 * Tag Chips Manager
 * Handles chip-based tag selection
 */

export class TagChipsManager {
    constructor(containerId, dropdownId, addBtnId, searchInputId) {
        this.chipsDisplay = document.getElementById(containerId);
        this.dropdown = document.getElementById(dropdownId);
        this.addBtn = document.getElementById(addBtnId);
        this.searchInput = document.getElementById(searchInputId);
        this.dropdownItems = [];

        if (!this.chipsDisplay || !this.dropdown || !this.addBtn) {
            console.warn('[TagChipsManager] Required elements not found');
            return;
        }

        this.init();
    }

    init() {
        // Get all dropdown items
        this.dropdownItems = Array.from(
            this.dropdown.querySelectorAll('.tag-dropdown-item')
        );

        // Add button click
        this.addBtn.addEventListener('click', () => this.toggleDropdown());

        // Search input
        if (this.searchInput) {
            this.searchInput.addEventListener('input', (e) => {
                this.filterDropdown(e.target.value);
            });
        }

        // Dropdown item click
        this.dropdownItems.forEach(item => {
            item.addEventListener('click', () => this.toggleTag(item));
        });

        // Close dropdown when clicking outside
        document.addEventListener('click', (e) => {
            if (!this.dropdown.contains(e.target) &&
                !this.addBtn.contains(e.target)) {
                this.closeDropdown();
            }
        });

    }

    /**
     * Toggle dropdown visibility
     */
    toggleDropdown() {
        const isVisible = this.dropdown.style.display !== 'none';

        if (isVisible) {
            this.closeDropdown();
        } else {
            this.openDropdown();
        }
    }

    /**
     * Open dropdown
     */
    openDropdown() {
        this.dropdown.style.display = 'block';
        if (this.searchInput) {
            this.searchInput.focus();
        }
    }

    /**
     * Close dropdown
     */
    closeDropdown() {
        this.dropdown.style.display = 'none';
        if (this.searchInput) {
            this.searchInput.value = '';
            this.filterDropdown(''); // Reset filter
        }
    }

    /**
     * Filter dropdown items by search term
     * @param {string} searchTerm - Search query
     */
    filterDropdown(searchTerm) {
        const query = searchTerm.toLowerCase().trim();

        this.dropdownItems.forEach(item => {
            const tagName = item.dataset.tagName.toLowerCase();
            const matches = tagName.includes(query);
            item.style.display = matches ? 'block' : 'none';
        });
    }

    /**
     * Toggle tag selection
     * @param {HTMLElement} item - Dropdown item
     */
    toggleTag(item) {
        const checkbox = item.querySelector('input[type="checkbox"]');
        const tagId = item.dataset.tagId;
        const tagName = item.dataset.tagName;

        if (checkbox.checked) {
            // Deselect
            checkbox.checked = false;
            item.classList.remove('selected');
            this.removeChip(tagId);
        } else {
            // Select
            checkbox.checked = true;
            item.classList.add('selected');
            this.addChip(tagId, tagName);
        }
    }

    /**
     * Add chip to display
     * @param {string} tagId - Tag ID
     * @param {string} tagName - Tag name
     */
    addChip(tagId, tagName) {
        // Check if already exists
        if (this.chipsDisplay.querySelector(`[data-chip-id="${tagId}"]`)) {
            return;
        }

        const chip = document.createElement('div');
        chip.className = 'tag-chip';
        chip.dataset.chipId = tagId;

        const chipText = document.createElement('span');
        chipText.textContent = tagName;

        const removeBtn = document.createElement('button');
        removeBtn.className = 'tag-chip-remove';
        removeBtn.type = 'button';
        removeBtn.innerHTML = '×';
        removeBtn.addEventListener('click', () => {
            this.removeChip(tagId);
            // Uncheck checkbox in dropdown
            const item = this.dropdown.querySelector(`[data-tag-id="${tagId}"]`);
            if (item) {
                const checkbox = item.querySelector('input[type="checkbox"]');
                if (checkbox) checkbox.checked = false;
                item.classList.remove('selected');
            }
        });

        chip.appendChild(chipText);
        chip.appendChild(removeBtn);
        this.chipsDisplay.appendChild(chip);
    }

    /**
     * Remove chip from display
     * @param {string} tagId - Tag ID
     */
    removeChip(tagId) {
        const chip = this.chipsDisplay.querySelector(`[data-chip-id="${tagId}"]`);
        if (chip) {
            chip.style.animation = 'chipDisappear 0.2s ease-out';
            setTimeout(() => chip.remove(), 200);
        }
    }

    /**
     * Get selected tag IDs
     * @returns {Array<string>} Selected tag IDs
     */
    getSelectedIds() {
        const chips = this.chipsDisplay.querySelectorAll('.tag-chip');
        return Array.from(chips).map(chip => chip.dataset.chipId);
    }

    /**
     * Clear all chips
     */
    clearAll() {
        this.chipsDisplay.innerHTML = '';
        this.dropdownItems.forEach(item => {
            const checkbox = item.querySelector('input[type="checkbox"]');
            if (checkbox) checkbox.checked = false;
            item.classList.remove('selected');
        });
    }

    /**
     * Set selected tags
     * @param {Array<{id: string, name: string}>} tags - Tags to select
     */
    setSelectedTags(tags) {
        this.clearAll();
        tags.forEach(tag => {
            this.addChip(tag.id, tag.name);
            const item = this.dropdown.querySelector(`[data-tag-id="${tag.id}"]`);
            if (item) {
                const checkbox = item.querySelector('input[type="checkbox"]');
                if (checkbox) checkbox.checked = true;
                item.classList.add('selected');
            }
        });
    }
    /**
     * Clear all chips and reset form
     * Call this after successful form submission
     */
    clearForm() {
        this.clearAll(); // Existing method

        // Also clear search input
        if (this.searchInput) {
            this.searchInput.value = '';
        }

        // Close dropdown
        this.closeDropdown();

    }
}

// Add chip disappear animation to CSS
const style = document.createElement('style');
style.textContent = `
@keyframes chipDisappear {
  to {
    opacity: 0;
    transform: scale(0.8);
  }
}
`;
document.head.appendChild(style);