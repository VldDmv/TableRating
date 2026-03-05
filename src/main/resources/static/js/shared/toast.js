/**
 * ToastService - centralized notification service.

 */

import { htmlUtils } from '../tableScripts/core/utils.js';

const DEFAULT_ICONS = {
    success: '✅',
    error:   '❌',
    info:    'ℹ️',
    warning: '⚠️'
};

const AUTO_DISMISS_MS  = 5000;
const SLIDE_OUT_MS     = 300;

export class ToastService {
  /**
  * @param {string} containerId — toast container ID (default: 'toastContainer')
  */
    constructor(containerId = 'toastContainer') {
        this.container = document.getElementById(containerId);

        if (!this.container) {
            console.warn(`[ToastService] not found`);
        }
    }

    // ─── Public API ────────────────────────────────────────────────────────

    /**
    * Shows a toast.
    * @param {string} message — message text
    * @param {'success'|'error'|'info'|'warning'} type
    * @param {string|null} iconOverride — custom icon (optional)
    */
    show(message, type = 'info', iconOverride = null) {
        if (!this.container) return;

        const icon  = iconOverride ?? DEFAULT_ICONS[type] ?? DEFAULT_ICONS.info;
        const toast = this._createToastElement(message, type, icon);

        this.container.appendChild(toast);
        this._scheduleAutoDismiss(toast);
    }

   /**
   * Reads the ?success=...&error=... parameters from the URL,
   * displays the corresponding toasts, and clears the URL.
   */
    checkUrlMessages() {
        const params  = new URLSearchParams(window.location.search);
        const success = params.get('success');
        const error   = params.get('error');

        if (success) this.show(this._decodeParam(success), 'success');
        if (error)   this.show(this._decodeParam(error),   'error');

        if (success || error) {
            this._cleanUrl(['success', 'error']);
        }
    }

    // ─── Private methods ─────────────────────────────────────────────────────

    /**
     * a toast DOM element.
     */
    _createToastElement(message, type, icon) {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;


        toast.innerHTML = `
            <div class="toast-icon">${icon}</div>
            <div class="toast-content">
                <div class="toast-message">${htmlUtils.escape(message)}</div>
            </div>
            <button class="toast-close" type="button" aria-label="Закрыть">×</button>
        `;


        toast.querySelector('.toast-close')
             .addEventListener('click', () => this._dismiss(toast));

        return toast;
    }
/**
* Starts the toast auto-hide timer.
*/
    _scheduleAutoDismiss(toast) {
        setTimeout(() => this._dismiss(toast), AUTO_DISMISS_MS);
    }

    /**
    * Animates and removes the toast.
    */
    _dismiss(toast) {

        if (!toast.isConnected) return;

        toast.style.animation = `slideOut ${SLIDE_OUT_MS}ms ease-out`;
        setTimeout(() => toast.remove(), SLIDE_OUT_MS);
    }

    /**
    * Decodes a URL parameter (Spring passes + instead of a space).
    */
    _decodeParam(value) {
        return decodeURIComponent(value.replace(/\+/g, ' '));
    }

 /**
 * Removes the specified parameters from the URL without reloading the page.
 * @param {string[]} paramNames
 */
    _cleanUrl(paramNames) {
        const url = new URL(window.location.href);
        paramNames.forEach(p => url.searchParams.delete(p));
        window.history.replaceState({}, '', url);
    }
}