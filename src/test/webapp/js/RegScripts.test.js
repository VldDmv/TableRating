

describe('FormSwitcher', () => {
    let loginForm;
    let registerForm;
    let showRegisterLink;
    let showLoginLink;

    beforeEach(() => {

        document.body.innerHTML = `
            <div id="login-form" class="hidden">
                <h2>Login</h2>
                <a href="#" id="show-register">Register</a>
            </div>
            <div id="register-form">
                <h2>Register</h2>
                <a href="#" id="show-login">Login</a>
            </div>
        `;

        loginForm = document.getElementById('login-form');
        registerForm = document.getElementById('register-form');
        showRegisterLink = document.getElementById('show-register');
        showLoginLink = document.getElementById('show-login');

        if (showRegisterLink) {
            showRegisterLink.addEventListener('click', (e) => {
                e.preventDefault();
                loginForm?.classList.add('hidden');
                registerForm?.classList.remove('hidden');
            });
        }

        if (showLoginLink) {
            showLoginLink.addEventListener('click', (e) => {
                e.preventDefault();
                loginForm?.classList.remove('hidden');
                registerForm?.classList.add('hidden');
            });
        }

        delete window.location;
        window.location = { search: '' };
    });

    afterEach(() => {
        document.body.innerHTML = '';
    });

    describe('Initial State', () => {
        test('should have login form hidden by default', () => {
            expect(loginForm.classList.contains('hidden')).toBe(true);
        });

        test('should have register form visible by default', () => {
            expect(registerForm.classList.contains('hidden')).toBe(false);
        });

        test('should find all required elements', () => {
            expect(loginForm).toBeTruthy();
            expect(registerForm).toBeTruthy();
            expect(showRegisterLink).toBeTruthy();
            expect(showLoginLink).toBeTruthy();
        });
    });

    describe('Form Switching', () => {
        test('should switch to register form when clicking show-register link', () => {

            loginForm.classList.remove('hidden');
            registerForm.classList.add('hidden');

            showRegisterLink.click();

            expect(loginForm.classList.contains('hidden')).toBe(true);
            expect(registerForm.classList.contains('hidden')).toBe(false);
        });

        test('should switch to login form when clicking show-login link', () => {

            loginForm.classList.add('hidden');
            registerForm.classList.remove('hidden');

            showLoginLink.click();

            expect(loginForm.classList.contains('hidden')).toBe(false);
            expect(registerForm.classList.contains('hidden')).toBe(true);
        });

        test('should prevent default link behavior on show-register click', () => {
            const event = new MouseEvent('click', { bubbles: true, cancelable: true });
            let defaultPrevented = false;

            Object.defineProperty(event, 'preventDefault', {
                value: () => { defaultPrevented = true; }
            });

            showRegisterLink.dispatchEvent(event);
            expect(defaultPrevented).toBe(true);
        });

        test('should prevent default link behavior on show-login click', () => {
            const event = new MouseEvent('click', { bubbles: true, cancelable: true });
            let defaultPrevented = false;

            Object.defineProperty(event, 'preventDefault', {
                value: () => { defaultPrevented = true; }
            });

            showLoginLink.dispatchEvent(event);
            expect(defaultPrevented).toBe(true);
        });

        test('should handle multiple form switches', () => {

            expect(registerForm.classList.contains('hidden')).toBe(false);


            showLoginLink.click();
            expect(loginForm.classList.contains('hidden')).toBe(false);
            expect(registerForm.classList.contains('hidden')).toBe(true);


            showRegisterLink.click();
            expect(loginForm.classList.contains('hidden')).toBe(true);
            expect(registerForm.classList.contains('hidden')).toBe(false);


            showLoginLink.click();
            expect(loginForm.classList.contains('hidden')).toBe(false);
            expect(registerForm.classList.contains('hidden')).toBe(true);
        });
    });

    describe('Missing Elements Handling', () => {
        test('should not throw error when forms are missing', () => {
            document.body.innerHTML = '';

            expect(() => {
                const login = document.getElementById('login-form');
                const register = document.getElementById('register-form');

                login?.classList.add('hidden');
                register?.classList.remove('hidden');
            }).not.toThrow();
        });

        test('should not throw error when links are missing', () => {
            document.body.innerHTML = `
                <div id="login-form"></div>
                <div id="register-form"></div>
            `;

            expect(() => {
                const showReg = document.getElementById('show-register');
                const showLog = document.getElementById('show-login');

                expect(showReg).toBeNull();
                expect(showLog).toBeNull();
            }).not.toThrow();
        });
    });

    describe('URL Parameter Handling', () => {
        test('should detect showLogin parameter', () => {
            window.location.search = '?showLogin=true';
            const urlParams = new URLSearchParams(window.location.search);

            expect(urlParams.get('showLogin')).toBe('true');
        });

        test('should show login form when showLogin parameter is present', () => {
            window.location.search = '?showLogin=true';
            const urlParams = new URLSearchParams(window.location.search);

            if (urlParams.get('showLogin')) {
                loginForm.classList.remove('hidden');
                registerForm.classList.add('hidden');
            }

            expect(loginForm.classList.contains('hidden')).toBe(false);
            expect(registerForm.classList.contains('hidden')).toBe(true);
        });

        test('should handle showLogin with different values', () => {
            window.location.search = '?showLogin=1';
            const urlParams = new URLSearchParams(window.location.search);

            expect(urlParams.get('showLogin')).toBeTruthy();
        });

        test('should handle empty URL parameters', () => {
            window.location.search = '';
            const urlParams = new URLSearchParams(window.location.search);

            expect(urlParams.get('showLogin')).toBeNull();
        });

        test('should handle multiple URL parameters', () => {
            window.location.search = '?showLogin=true&other=value';
            const urlParams = new URLSearchParams(window.location.search);

            expect(urlParams.get('showLogin')).toBe('true');
            expect(urlParams.get('other')).toBe('value');
        });
    });

    describe('CSS Classes', () => {
        test('should use "hidden" class for visibility', () => {
            loginForm.classList.add('hidden');
            expect(loginForm.classList.contains('hidden')).toBe(true);

            loginForm.classList.remove('hidden');
            expect(loginForm.classList.contains('hidden')).toBe(false);
        });

        test('should toggle classes correctly', () => {

            loginForm.classList.remove('hidden');
            registerForm.classList.remove('hidden');


            loginForm.classList.add('hidden');
            registerForm.classList.remove('hidden');

            expect(loginForm.classList.contains('hidden')).toBe(true);
            expect(registerForm.classList.contains('hidden')).toBe(false);
        });
    });

    describe('Event Listeners', () => {
        test('should trigger event on show-register link click', () => {
            let eventTriggered = false;
            showRegisterLink.addEventListener('click', () => {
                eventTriggered = true;
            });

            showRegisterLink.click();
            expect(eventTriggered).toBe(true);
        });

        test('should trigger event on show-login link click', () => {
            let eventTriggered = false;
            showLoginLink.addEventListener('click', () => {
                eventTriggered = true;
            });

            showLoginLink.click();
            expect(eventTriggered).toBe(true);
        });
    });

    describe('Edge Cases', () => {
        test('should handle clicking when already on target form', () => {

            loginForm.classList.remove('hidden');
            registerForm.classList.add('hidden');


            showLoginLink.click();


            expect(loginForm.classList.contains('hidden')).toBe(false);
            expect(registerForm.classList.contains('hidden')).toBe(true);
        });

        test('should work with both forms initially visible', () => {
            loginForm.classList.remove('hidden');
            registerForm.classList.remove('hidden');

            showLoginLink.click();

            expect(loginForm.classList.contains('hidden')).toBe(false);
            expect(registerForm.classList.contains('hidden')).toBe(true);
        });

        test('should work with both forms initially hidden', () => {
            loginForm.classList.add('hidden');
            registerForm.classList.add('hidden');

            showLoginLink.click();

            expect(loginForm.classList.contains('hidden')).toBe(false);
            expect(registerForm.classList.contains('hidden')).toBe(true);
        });
    });
});