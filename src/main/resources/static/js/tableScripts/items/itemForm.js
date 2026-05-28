/**
 * Manages form submission handling for adding items in category pages.
 */

export class ItemFormManager {
    /**
     * Creates a new ItemFormManager instance.
     * @param {HTMLFormElement} formElement - Form element.
     * @param {HTMLInputElement} scoreInputElement - Score input element.
     * @param {Function} validateScore - Score validation function.
     */
    constructor(formElement, scoreInputElement, validateScore) {
        this.formElement = formElement;
        this.scoreInputElement = scoreInputElement;
        this.validateScore = validateScore;
    }

    /**
     * Initializes form validation.
     */
    init() {
        if (!this.formElement || !this.scoreInputElement) {
            console.error('ItemFormManager: Missing required elements.');
            return;
        }

        this.formElement.addEventListener('submit', (event) => {
            if (!this.handleSubmit()) {
                event.preventDefault();
            }
        });
    }

    /**
     * Handles form submission.
     * @returns {boolean} True if validation passed.
     */
    handleSubmit() {
        const scoreValue = this.scoreInputElement.value.trim();

        const isValid = this.validateScore(scoreValue);

        if (!isValid) {
            this.scoreInputElement.focus();
            return false;
        }

        return true;
    }

    /**
     * Resets the form.
     */
    reset() {
        if (this.formElement) {
            this.formElement.reset();
        }
    }

    /**
     * Disables the form.
     * @param {boolean} disabled - Disabled state.
     */
    setDisabled(disabled) {
        if (this.formElement) {
            const elements = this.formElement.elements;
            for (let i = 0; i < elements.length; i++) {
                elements[i].disabled = disabled;
            }
        }
    }
}
