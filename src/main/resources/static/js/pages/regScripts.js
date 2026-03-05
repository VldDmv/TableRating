/**
 * Initializes registration and login form functionality with form switching.
 */

export { FormSwitcher }

class FormSwitcher {
    constructor() {
        this.loginForm = document.getElementById('login-form');
        this.registerForm = document.getElementById('register-form');
        this.usernameInput = document.getElementById('register-name');
        this.usernameIndicator = document.getElementById('username-indicator');
        this.usernameMessage = document.getElementById('username-message');
        this.registerButton = document.getElementById('register-button');

        // Debounce timer for username check
        this.checkUsernameTimer = null;
        this.isUsernameAvailable = false;

        this.init();
    }

    /**
     * Initializes form switching functionality.
     */
    init() {
        // Check URL parameter for initial form display
        const urlParams = new URLSearchParams(window.location.search);

        //  Handle both showRegister and showLogin parameters
        if (urlParams.get("showRegister")) {
            this.showRegister();
        } else if (urlParams.get("showLogin")) {
            this.showLogin();
        }

        // Setup event listeners
        this.setupEventListeners();
        this.setupUsernameCheck();
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
     *  Setup realtime username availability check
     */
    setupUsernameCheck() {

        if (!this.usernameInput
            || !this.usernameIndicator
            || !this.usernameMessage
            || !this.registerButton) return;

        // Check on input (with debounce to avoid too many requests)
        this.usernameInput.addEventListener('input', () => {
            clearTimeout(this.checkUsernameTimer);

            const username = this.usernameInput.value.trim();

            // Reset if empty
            if (!username) {
                this.resetUsernameIndicator();
                return;
            }

            // Show loading state
            this.showLoadingState();

            // Debounce: wait 500ms after user stops typing
            this.checkUsernameTimer = setTimeout(() => {
                this.checkUsernameAvailability(username);
            }, 500);
        });

        // Prevent form submission if username is taken
        this.registerForm?.addEventListener('submit', (e) => {
            if (!this.isUsernameAvailable && this.usernameInput.value.trim()) {
                e.preventDefault();
                this.showError('Username is already taken. Please choose another.');
            }
        });
    }

    /**
     *  Check username availability via AJAX
     */
    async checkUsernameAvailability(username) {
        try {
            const response = await fetch(`/auth/check-username?username=${encodeURIComponent(username)}`);

            if (!response.ok) {
                throw new Error('Failed to check username');
            }

            const data = await response.json();
            this.isUsernameAvailable = data.available;

            if (data.available) {
                this.showAvailable();
            } else {
                this.showTaken();
            }

        } catch (error) {
            console.error('Error checking username:', error);
            this.showError('Could not verify username availability');
        }
    }

    /**
     * Show loading state while checking
     */
    showLoadingState() {
        this.usernameIndicator.textContent = '⏳';
        this.usernameIndicator.className = 'username-indicator checking';
        this.usernameMessage.textContent = 'Checking...';
        this.usernameMessage.className = 'username-message checking';
        this.registerButton.disabled = true;
    }

    /**
     Show username is available
     */
    showAvailable() {
        this.usernameIndicator.textContent = '✓';
        this.usernameIndicator.className = 'username-indicator available';
        this.usernameMessage.textContent = 'Username available';
        this.usernameMessage.className = 'username-message available';
        this.registerButton.disabled = false;
    }

    /**
     * Show username is taken
     */
    showTaken() {
        this.usernameIndicator.textContent = '✗';
        this.usernameIndicator.className = 'username-indicator taken';
        this.usernameMessage.textContent = 'Username already taken';
        this.usernameMessage.className = 'username-message taken';
        this.registerButton.disabled = true;
    }

    /**
    * Show error message
     */
    showError(message) {
        this.usernameIndicator.textContent = '!';
        this.usernameIndicator.className = 'username-indicator error';
        this.usernameMessage.textContent = message;
        this.usernameMessage.className = 'username-message error';
        this.registerButton.disabled = true;
    }

    /**
     * Reset indicator to initial state
     */
    resetUsernameIndicator() {
        this.usernameIndicator.textContent = '';
        this.usernameIndicator.className = 'username-indicator';
        this.usernameMessage.textContent = '';
        this.usernameMessage.className = 'username-message';
        this.registerButton.disabled = false;
        this.isUsernameAvailable = false;
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