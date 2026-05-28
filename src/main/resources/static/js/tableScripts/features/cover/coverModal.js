/**
 * Cover Image Modal
 */
export class CoverModal {
    constructor() {
        this.modal = null;
        this.currentItemName = null; // Track current item
        this.currentCoverUrl = null;
        this.init();
    }

    init() {
        // Create modal HTML with edit button
        this.modal = document.createElement('div');
        this.modal.className = 'cover-modal';
        this.modal.innerHTML = `
            <span class="cover-modal-close">&times;</span>
            <div class="cover-modal-content">
                <img class="cover-modal-image" src="" alt="Cover">
                <div class="cover-modal-title"></div>
                <button class="cover-modal-edit-btn">✏️ Edit Cover</button>
            </div>
        `;
        document.body.appendChild(this.modal);

        // Event listeners
        const closeBtn = this.modal.querySelector('.cover-modal-close');
        closeBtn.addEventListener('click', () => this.close());

        // Edit button
        const editBtn = this.modal.querySelector('.cover-modal-edit-btn');
        editBtn.addEventListener('click', () => this.handleEdit());

        this.modal.addEventListener('click', (e) => {
            if (e.target === this.modal) {
                this.close();
            }
        });

        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.modal.style.display === 'block') {
                this.close();
            }
        });
    }

    /**
     * Show modal with cover image
     * @param {string} imageUrl - URL of the cover image
     * @param {string} title - Title to display below image
     */
    show(imageUrl, title = '') {
        const img = this.modal.querySelector('.cover-modal-image');
        const titleEl = this.modal.querySelector('.cover-modal-title');

        img.src = imageUrl;
        img.alt = title;
        titleEl.textContent = title;

        // Store for edit handler
        this.currentItemName = title;
        this.currentCoverUrl = imageUrl;

        this.modal.style.display = 'block';
        document.body.style.overflow = 'hidden'; // Prevent body scroll
    }

    /**
     * Handle edit button click
     */
    handleEdit() {
        // Close modal first
        this.close();

        // Trigger edit via custom event that coverClickHandler will listen to
        const event = new CustomEvent('coverEditRequested', {
            detail: {
                itemName: this.currentItemName,
                currentUrl: this.currentCoverUrl,
            },
        });
        document.dispatchEvent(event);
    }

    /**
     * Close modal
     */
    close() {
        this.modal.style.display = 'none';
        document.body.style.overflow = ''; // Restore body scroll
    }
}
