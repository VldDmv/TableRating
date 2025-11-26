/**
 * Modal management for admin tags/genres editing
 */
export class ManagementModal {
    constructor() {
        this.modal = document.getElementById('editModal');
        this.closeBtn = document.querySelector('.close-btn');
        this.editIdInput = document.getElementById('edit-id');
        this.editNameInput = document.getElementById('edit-name');
        this.type = this.modal?.dataset.type || 'tags';
    }

    init() {
        if (!this.modal) return;

        this.attachEventListeners();
    }

    attachEventListeners() {
        // Edit buttons
        document.querySelectorAll('.edit-btn').forEach(button => {
            button.addEventListener('click', () => this.openModal(button));
        });

        // Close button
        this.closeBtn?.addEventListener('click', () => this.closeModal());

        // Click outside modal
        window.addEventListener('click', (event) => {
            if (event.target === this.modal) {
                this.closeModal();
            }
        });

        // Escape key
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && this.modal.style.display === 'block') {
                this.closeModal();
            }
        });
    }

    openModal(button) {
        const id = button.dataset.id;
        const name = button.dataset.name;

        this.editIdInput.value = id;
        this.editNameInput.value = name;

        // Handle genres-specific media types
        if (this.type === 'genres' && button.dataset.mediaTypes) {
            this.updateMediaTypeCheckboxes(button.dataset.mediaTypes);
        }

        this.modal.style.display = 'block';
        this.editNameInput.focus();
    }

    updateMediaTypeCheckboxes(mediaTypesString) {
        const mediaTypes = mediaTypesString.split(',');
        const checkboxes = document.querySelectorAll('#edit-media-types input[type="checkbox"]');

        checkboxes.forEach(cb => {
            cb.checked = mediaTypes.includes(cb.value);
        });
    }

    closeModal() {
        this.modal.style.display = 'none';
    }
}

// Initialize on DOMContentLoaded
document.addEventListener('DOMContentLoaded', () => {
    const modal = new ManagementModal();
    modal.init();
});