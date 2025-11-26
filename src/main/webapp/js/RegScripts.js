/**
 * Initializes registration and login form functionality with form switching.
 */

class FormSwitcher {
    constructor() {
        this.loginForm = document.getElementById('login-form');
        this.registerForm = document.getElementById('register-form');
        this.init();
    }

    /**
     * Initializes form switching functionality.
     */
    init() {
        // Check URL parameter for initial form display
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.get("showLogin")) {
            this.showLogin();
        }

        // Setup event listeners
        this.setupEventListeners();
    }

    /**
     * Sets up click event listeners for form switching.
     */
    setupEventListeners() {
        const showRegisterLink = document.getElementById('show-register');
        const showLoginLink = document.getElementById('show-login');

        if (showRegisterLink) {
            showRegisterLink.addEventListener('click', (e) => {
                e.preventDefault();
                this.showRegister();
            });
        }

        if (showLoginLink) {
            showLoginLink.addEventListener('click', (e) => {
                e.preventDefault();
                this.showLogin();
            });
        }
    }

    /**
     * Shows the login form and hides the registration form.
     */
    showLogin() {
        this.loginForm?.classList.remove('hidden');
        this.registerForm?.classList.add('hidden');
    }

    /**
     * Shows the registration form and hides the login form.
     */
    showRegister() {
        this.loginForm?.classList.add('hidden');
        this.registerForm?.classList.remove('hidden');
    }
}

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', () => {
    new FormSwitcher();
});