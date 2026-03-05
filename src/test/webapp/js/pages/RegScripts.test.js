import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';

const { FormSwitcher } = await import('@/pages/regScripts.js');

// ─── Helper ───────────────────────────────────────────────────────────────────

function setupFullDOM() {
    document.body.innerHTML = `
        <div id="login-form">
            <a href="#" id="show-register">Register</a>
        </div>
        <div id="register-form" class="hidden">
            <input type="text" id="register-name">
            <span id="username-indicator"></span>
            <div id="username-message"></div>
            <button id="register-button">Register</button>
            <a href="#" id="show-login">Login</a>
        </div>
    `;
}

function makeSwitcher(search = '') {
    delete window.location;
    window.location = { search };
    return new FormSwitcher();
}

// ─── Form switching ───────────────────────────────────────────────────────────

describe('FormSwitcher — form switching', () => {
    beforeEach(() => { setupFullDOM(); });

    afterEach(() => {
        document.body.innerHTML = '';
        jest.clearAllMocks();
    });

    test('show-register click hides login and shows register', () => {
        makeSwitcher();
        document.getElementById('show-register').click();
        expect(document.getElementById('login-form').classList.contains('hidden')).toBe(true);
        expect(document.getElementById('register-form').classList.contains('hidden')).toBe(false);
    });

    test('show-login click shows login and hides register', () => {
        makeSwitcher();
        document.getElementById('show-login').click();
        expect(document.getElementById('login-form').classList.contains('hidden')).toBe(false);
        expect(document.getElementById('register-form').classList.contains('hidden')).toBe(true);
    });

    test('multiple switches work correctly', () => {
        makeSwitcher();
        const showReg = document.getElementById('show-register');
        const showLog = document.getElementById('show-login');

        showLog.click();
        expect(document.getElementById('login-form').classList.contains('hidden')).toBe(false);

        showReg.click();
        expect(document.getElementById('register-form').classList.contains('hidden')).toBe(false);

        showLog.click();
        expect(document.getElementById('login-form').classList.contains('hidden')).toBe(false);
    });
});

// ─── Username availability check ──────────────────────────────────────────────

describe('FormSwitcher — username availability check', () => {
   beforeEach(() => {
       setupFullDOM();
       jest.useFakeTimers();
       global.fetch = jest.fn().mockResolvedValue({
           ok: true,
           json: async () => ({ available: true })
       });
   });

    afterEach(() => {
        document.body.innerHTML = '';
        jest.useRealTimers();
        jest.clearAllMocks();
        delete global.fetch;
    });

    function simulateInput(value) {
        const input = document.getElementById('register-name');
        input.value = value;
        input.dispatchEvent(new Event('input'));
    }

    test('empty input resets indicator', () => {
        makeSwitcher();
        simulateInput('test');
        expect(document.getElementById('username-indicator').textContent).toBe('⏳');

        simulateInput('');
        expect(document.getElementById('username-indicator').textContent).toBe('');
        expect(document.getElementById('register-button').disabled).toBe(false);
    });

    test('shows loading state while debounce pending', () => {
        makeSwitcher();
        simulateInput('newuser');
        expect(document.getElementById('username-indicator').textContent).toBe('⏳');
        expect(document.getElementById('username-message').textContent).toBe('Checking...');
        expect(document.getElementById('register-button').disabled).toBe(true);
    });

    test('debounce: does not call fetch until 500ms after last input', async () => {
        makeSwitcher();
        simulateInput('a');
        jest.advanceTimersByTime(200);
        simulateInput('ab');
        jest.advanceTimersByTime(200);
        simulateInput('abc');

        expect(global.fetch).not.toHaveBeenCalled();

        jest.advanceTimersByTime(500);
        expect(global.fetch).toHaveBeenCalledTimes(1);
        expect(global.fetch).toHaveBeenCalledWith(expect.stringContaining('abc'));
    });

    test('shows available when API returns available: true', async () => {
        global.fetch.mockResolvedValue({
            ok: true,
            json: async () => ({ available: true })
        });

        makeSwitcher();
        simulateInput('freeuser');
        jest.advanceTimersByTime(500);
        await Promise.resolve();
        await Promise.resolve();

        expect(document.getElementById('username-indicator').textContent).toBe('✓');
        expect(document.getElementById('register-button').disabled).toBe(false);
    });

    test('shows taken when API returns available: false', async () => {
        global.fetch.mockResolvedValue({
            ok: true,
            json: async () => ({ available: false })
        });

        makeSwitcher();
        simulateInput('takenuser');
        jest.advanceTimersByTime(500);
        await Promise.resolve();
        await Promise.resolve();

        expect(document.getElementById('username-indicator').textContent).toBe('✗');
        expect(document.getElementById('register-button').disabled).toBe(true);
    });

    test('shows error state when fetch throws', async () => {
        global.fetch.mockRejectedValue(new Error('Network error'));

        makeSwitcher();
        simulateInput('someuser');
        jest.advanceTimersByTime(500);
        await Promise.resolve();
        await Promise.resolve();

        expect(document.getElementById('username-indicator').textContent).toBe('!');
        expect(document.getElementById('register-button').disabled).toBe(true);
    });
});

// ─── URL parameter handling ───────────────────────────────────────────────────

describe('FormSwitcher — URL parameters', () => {
    beforeEach(() => { setupFullDOM(); });
    afterEach(() => { document.body.innerHTML = ''; });

    test('showRegister param shows register form', () => {
        makeSwitcher('?showRegister=true');
        expect(document.getElementById('register-form').classList.contains('hidden')).toBe(false);
        expect(document.getElementById('login-form').classList.contains('hidden')).toBe(true);
    });

    test('showLogin param shows login form', () => {
        makeSwitcher('?showLogin=true');
        expect(document.getElementById('login-form').classList.contains('hidden')).toBe(false);
        expect(document.getElementById('register-form').classList.contains('hidden')).toBe(true);
    });

    test('no params leaves initial state unchanged', () => {
        makeSwitcher('');
        expect(document.getElementById('login-form').classList.contains('hidden')).toBe(false);
        expect(document.getElementById('register-form').classList.contains('hidden')).toBe(true);
    });

    test('showRegister takes priority over showLogin', () => {
        makeSwitcher('?showRegister=true&showLogin=true');
        expect(document.getElementById('register-form').classList.contains('hidden')).toBe(false);
    });
});

// ─── Null safety ──────────────────────────────────────────────────────────────

describe('FormSwitcher — null safety', () => {
    afterEach(() => { document.body.innerHTML = ''; });

    test('does not throw when optional UI elements are missing', () => {
        document.body.innerHTML = `
            <div id="login-form"></div>
            <div id="register-form" class="hidden"></div>
        `;
        delete window.location;
        window.location = { search: '' };
        expect(() => new FormSwitcher()).not.toThrow();
    });

    test('does not throw when all form elements are missing', () => {
        document.body.innerHTML = '';
        delete window.location;
        window.location = { search: '' };
        expect(() => new FormSwitcher()).not.toThrow();
    });
});