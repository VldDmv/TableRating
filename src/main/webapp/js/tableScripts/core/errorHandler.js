/**
 * Centralized error handling for the application.
 */

import { htmlUtils } from './utils.js';

export class ErrorHandler {
    /**
     * Handles errors with logging and user notification.
     * @param {Error} error - The error object.
     * @param {string} userMessage - User-friendly error message.
     * @param {HTMLElement} container - Container to display error (optional).
     */
    static handle(error, userMessage, container = null) {
        console.error('Error occurred:', error);

        const errorMessage = error ? error.message : 'An unknown error occurred.';

        if (container) {
            this.showError(container, userMessage, errorMessage);
        } else {
            alert(`${userMessage}\n\nDetails: ${errorMessage}`);
        }

    }

    /**
     * Displays an error message in a container.
     * @param {HTMLElement} container - The container element.
     * @param {string} title - Error title.
     * @param {string} details - Error details.
     */
    static showError(container, title, details) {

        const safeTitle = title || 'Error';
        const safeDetails = details || 'No details provided.';

        if (!container) {
            throw new Error("showError: container element is null or undefined.");
        }


        container.innerHTML = `
            <div class="error-message" style="padding: 20px; background-color: #fee; border: 1px solid #fcc; border-radius: 4px;">
                <strong style="color: #c00;">${htmlUtils.escape(safeTitle)}</strong>
                <p style="margin-top: 10px; color: #666;">${htmlUtils.escape(safeDetails)}</p>
            </div>
        `;
    }

    /**
     * Shows a loading indicator in a container.
     * @param {HTMLElement} container - The container element.
     * @param {string} message - Loading message.
     */
    static showLoading(container, message = 'Loading...') {
        if (!container) return;
        container.innerHTML = `
            <div class="loading-indicator" style="padding: 20px; text-align: center;">
                <p>${htmlUtils.escape(message)}</p>
            </div>
        `;
    }

    /**
     * Parses error response from fetch.
     * @param {Response} response - Fetch response object.
     * @returns {Promise<string>} Error message.
     */
    static async parseErrorResponse(response) {
        try {
            const text = await response.text();
            try {
                const json = JSON.parse(text);
                return json.message || `HTTP error ${response.status}`;
            } catch {
                return text || `HTTP error ${response.status}`;
            }
        } catch {
            return `HTTP error ${response.status}`;
        }
    }
}