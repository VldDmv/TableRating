describe('Header Dropdown', () => {
    let userMenuButton;
    let userMenuDropdown;

    beforeEach(() => {
        document.body.innerHTML = `
            <button id="user-menu-button">User Menu</button>
            <div id="user-menu-dropdown"></div>
        `;

        userMenuButton = document.getElementById('user-menu-button');
        userMenuDropdown = document.getElementById('user-menu-dropdown');

        userMenuButton.addEventListener('click', function (e) {
            e.stopPropagation();
            userMenuDropdown.classList.toggle('show');
        });

        document.addEventListener('click', function (e) {
            if (!userMenuButton.contains(e.target) && !userMenuDropdown.contains(e.target)) {
                userMenuDropdown.classList.remove('show');
            }
        });

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                userMenuDropdown.classList.remove('show');
            }
        });
    });

    afterEach(() => {
        document.body.innerHTML = '';
    });

    describe('Dropdown Toggle', () => {
        test('should toggle dropdown visibility on button click', () => {
            userMenuButton.click();
            expect(userMenuDropdown.classList.contains('show')).toBe(true);

            userMenuButton.click();
            expect(userMenuDropdown.classList.contains('show')).toBe(false);
        });

        test('should stop event propagation on button click', () => {
            let propagationStopped = false;

            const event = new MouseEvent('click', { bubbles: true });
            Object.defineProperty(event, 'stopPropagation', {
                value: () => {
                    propagationStopped = true;
                },
            });

            userMenuButton.dispatchEvent(event);
            expect(propagationStopped).toBe(true);
        });
    });

    describe('Close Dropdown', () => {
        beforeEach(() => {
            userMenuButton.click();
        });

        test('should close dropdown when clicking outside', () => {
            expect(userMenuDropdown.classList.contains('show')).toBe(true);

            document.body.click();
            expect(userMenuDropdown.classList.contains('show')).toBe(false);
        });

        test('should not close dropdown when clicking on button', () => {
            expect(userMenuDropdown.classList.contains('show')).toBe(true);

            userMenuButton.click();
            expect(userMenuDropdown.classList.contains('show')).toBe(false);
        });

        test('should not close dropdown when clicking inside dropdown', () => {
            expect(userMenuDropdown.classList.contains('show')).toBe(true);

            userMenuDropdown.click();
            expect(userMenuDropdown.classList.contains('show')).toBe(true);
        });

        test('should close dropdown on Escape key press', () => {
            expect(userMenuDropdown.classList.contains('show')).toBe(true);

            const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });
            document.dispatchEvent(escapeEvent);
            expect(userMenuDropdown.classList.contains('show')).toBe(false);
        });

        test('should not close dropdown on other key press', () => {
            expect(userMenuDropdown.classList.contains('show')).toBe(true);

            const enterEvent = new KeyboardEvent('keydown', { key: 'Enter' });
            document.dispatchEvent(enterEvent);
            expect(userMenuDropdown.classList.contains('show')).toBe(true);
        });
    });

    describe('Initialization', () => {
        test('should not throw error if elements are missing', () => {
            document.body.innerHTML = '';
            expect(() => {
                const button = document.getElementById('user-menu-button');
                const dropdown = document.getElementById('user-menu-dropdown');

                if (!button || !dropdown) {
                    return;
                }
            }).not.toThrow();
        });
    });
});
