/**
 * Collapsible Form Module
 */

export class CollapsibleForm {
    /**
     * Creates a new CollapsibleForm instance.
     * @param {HTMLFormElement} formElement - The form element to make collapsible
     * @param {Object} options - Configuration options
     */
    constructor(formElement, options = {}) {
        this.form = formElement;
        this.options = {
            startCollapsed: options.startCollapsed !== false,
            buttonText: options.buttonText || 'Add Item',
            buttonIcon: options.buttonIcon || '+',
            animationDuration: options.animationDuration || 300,
            collapseAfterSubmit: options.collapseAfterSubmit || false,
            ...options,
        };

        this.isCollapsed = this.options.startCollapsed;
        this.toggleButton = null;
        this.formWrapper = null;

        this.init();
    }

    /**
     * Initializes the collapsible form
     */
    init() {
        if (!this.form) {
            console.error('CollapsibleForm: Form element not found');
            return;
        }

        this.wrapForm();
        this.createToggleButton();

        if (this.isCollapsed) {
            this.collapse(false);
        }

        this.form.addEventListener('submit', () => this.handleSubmit());
    }

    /**
     * Handle form submission
     */
    handleSubmit() {
        if (this.options.collapseAfterSubmit) {
            // Only collapse if explicitly requested
            setTimeout(() => {
                this.collapse();
            }, 100);
        }
        // Otherwise, form stays open for easy multiple additions
    }

    /**
     * Wraps the form in a collapsible container
     */
    wrapForm() {
        // Create wrapper
        this.formWrapper = document.createElement('div');
        this.formWrapper.className = 'collapsible-form-wrapper';

        // Create container for form content
        const formContent = document.createElement('div');
        formContent.className = 'collapsible-form-content';

        // Move form into content
        this.form.parentNode.insertBefore(this.formWrapper, this.form);
        formContent.appendChild(this.form);
        this.formWrapper.appendChild(formContent);

        this.formContent = formContent;
    }

    /**
     * Creates the toggle button
     */
    createToggleButton() {
        const buttonContainer = document.createElement('div');
        buttonContainer.className = 'collapsible-form-toggle-container';

        this.toggleButton = document.createElement('button');
        this.toggleButton.type = 'button';
        this.toggleButton.className = 'collapsible-form-toggle-btn';
        this.toggleButton.innerHTML = `
            <span class="toggle-icon">${this.options.buttonIcon}</span>
            <span class="toggle-text">${this.options.buttonText}</span>
        `;

        this.toggleButton.addEventListener('click', () => this.toggle());

        buttonContainer.appendChild(this.toggleButton);
        this.formWrapper.insertBefore(buttonContainer, this.formContent);
    }

    /**
     * Toggles form visibility
     */
    toggle() {
        if (this.isCollapsed) {
            this.expand();
        } else {
            this.collapse();
        }
    }

    /**
     * Expands the form
     * @param {boolean} animated - Whether to animate the expansion
     */
    expand(animated = true) {
        this.isCollapsed = false;

        // Update button
        this.toggleButton.classList.add('expanded');
        const icon = this.toggleButton.querySelector('.toggle-icon');
        if (icon) {
            icon.textContent = '−'; // Minus sign
        }

        if (animated) {
            // Measure the height
            this.formContent.style.display = 'block';
            const height = this.formContent.scrollHeight;
            this.formContent.style.height = '0px';
            this.formContent.style.overflow = 'hidden';

            // Force reflow
            this.formContent.offsetHeight;

            // Animate
            this.formContent.style.transition = `height ${this.options.animationDuration}ms ease-out`;
            this.formContent.style.height = height + 'px';

            // Clean up after animation
            setTimeout(() => {
                this.formContent.style.height = 'auto';
                this.formContent.style.overflow = 'visible';
                this.formContent.style.transition = '';

                // Focus first input
                const firstInput = this.form.querySelector('input:not([type="hidden"])');
                if (firstInput) {
                    firstInput.focus();
                }
            }, this.options.animationDuration);
        } else {
            this.formContent.style.display = 'block';
            this.formContent.style.height = 'auto';
        }

        this.formWrapper.classList.add('expanded');
    }

    /**
     * Collapses the form
     * @param {boolean} animated - Whether to animate the collapse
     */
    collapse(animated = true) {
        this.isCollapsed = true;

        // Update button
        this.toggleButton.classList.remove('expanded');
        const icon = this.toggleButton.querySelector('.toggle-icon');
        if (icon) {
            icon.textContent = this.options.buttonIcon;
        }

        if (animated) {
            // Set current height
            const height = this.formContent.scrollHeight;
            this.formContent.style.height = height + 'px';
            this.formContent.style.overflow = 'hidden';

            // Force reflow
            this.formContent.offsetHeight;

            // Animate
            this.formContent.style.transition = `height ${this.options.animationDuration}ms ease-out`;
            this.formContent.style.height = '0px';

            // Clean up after animation
            setTimeout(() => {
                this.formContent.style.display = 'none';
                this.formContent.style.transition = '';
            }, this.options.animationDuration);
        } else {
            this.formContent.style.display = 'none';
            this.formContent.style.height = '0px';
        }

        this.formWrapper.classList.remove('expanded');
    }

    /**
     * Expands the form (public method)
     */
    show() {
        this.expand();
    }

    /**
     * Collapses the form (public method)
     */
    hide() {
        this.collapse();
    }

    /**
     * Checks if form is collapsed
     */
    isHidden() {
        return this.isCollapsed;
    }

    /**
     * Destroys the collapsible form
     */
    destroy() {
        if (this.toggleButton) {
            this.toggleButton.removeEventListener('click', this.toggle);
        }

        // Unwrap form
        if (this.formWrapper && this.formWrapper.parentNode) {
            this.formWrapper.parentNode.insertBefore(this.form, this.formWrapper);
            this.formWrapper.parentNode.removeChild(this.formWrapper);
        }
    }
}
