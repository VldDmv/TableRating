import { jest } from '@jest/globals';

jest.unstable_mockModule('@/tableScripts/core/utils.js', () => ({
    htmlUtils: { escape: (s) => s.replace(/</g, '&lt;').replace(/>/g, '&gt;') },
}));

const { ToastService } = await import('@/shared/toast.js');

// ─── Setup ────────────────────────────────────────────────────────────────────

function setupContainer() {
    document.body.innerHTML = '<div id="toastContainer"></div>';
    return document.getElementById('toastContainer');
}

// ─── Constructor ─────────────────────────────────────────────────────────────

describe('ToastService constructor', () => {
    test('finds container by id', () => {
        setupContainer();
        const toast = new ToastService('toastContainer');
        expect(toast.container).not.toBeNull();
    });

    test('warns when container not found', () => {
        document.body.innerHTML = '';
        const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
        const toast = new ToastService('missingContainer');
        expect(toast.container).toBeNull();
        expect(warnSpy).toHaveBeenCalled();
        warnSpy.mockRestore();
    });
});

// ─── show() ──────────────────────────────────────────────────────────────────

describe('ToastService.show', () => {
    let container;
    let service;

    beforeEach(() => {
        container = setupContainer();
        service = new ToastService('toastContainer');
        jest.useFakeTimers();
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    test('appends a toast element to container', () => {
        service.show('Hello!', 'success');
        expect(container.querySelectorAll('.toast').length).toBe(1);
    });

    test('toast has correct type class', () => {
        service.show('Error occurred', 'error');
        const toast = container.querySelector('.toast');
        expect(toast.classList.contains('toast-error')).toBe(true);
    });

    test('toast contains the message text', () => {
        service.show('Saved successfully', 'success');
        const msg = container.querySelector('.toast-message');
        expect(msg.textContent).toBe('Saved successfully');
    });

    test('uses default icon for success type', () => {
        service.show('OK', 'success');
        const icon = container.querySelector('.toast-icon');
        expect(icon.textContent).toBe('✅');
    });

    test('uses default icon for error type', () => {
        service.show('Fail', 'error');
        const icon = container.querySelector('.toast-icon');
        expect(icon.textContent).toBe('❌');
    });

    test('uses custom icon when provided', () => {
        service.show('Loading...', 'info', '⏳');
        const icon = container.querySelector('.toast-icon');
        expect(icon.textContent).toBe('⏳');
    });

    test('clicking close button removes the toast', () => {
        service.show('Test', 'info');
        jest.useFakeTimers();

        const closeBtn = container.querySelector('.toast-close');
        closeBtn.click();

        jest.advanceTimersByTime(400);
        expect(container.querySelectorAll('.toast').length).toBe(0);
    });

    test('toast auto-dismisses after 5 seconds', () => {
        service.show('Auto dismiss', 'info');
        expect(container.querySelectorAll('.toast').length).toBe(1);

        jest.advanceTimersByTime(5000 + 400);
        expect(container.querySelectorAll('.toast').length).toBe(0);
    });

    test('does nothing when container is null', () => {
        document.body.innerHTML = '';
        const s = new ToastService('missing');
        expect(() => s.show('msg', 'info')).not.toThrow();
    });

    test('multiple toasts stack', () => {
        service.show('First', 'info');
        service.show('Second', 'success');
        service.show('Third', 'error');
        expect(container.querySelectorAll('.toast').length).toBe(3);
    });
});

// ─── _decodeParam ─────────────────────────────────────────────────────────────

describe('ToastService._decodeParam (via checkUrlMessages)', () => {
    // We test the private method indirectly through its effect on shown toasts
    let container;
    let service;

    beforeEach(() => {
        container = setupContainer();
        service = new ToastService('toastContainer');
        jest.useFakeTimers();
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    test('decodes + as space in URL params', () => {
        Object.defineProperty(window, 'location', {
            value: {
                search: '?success=Item+saved+successfully',
                href: 'http://localhost/?success=Item+saved+successfully',
            },
            writable: true,
        });

        service.checkUrlMessages();

        const msg = container.querySelector('.toast-message');
        expect(msg.textContent).toBe('Item saved successfully');
    });

    test('decodes %20 in URL params', () => {
        Object.defineProperty(window, 'location', {
            value: {
                search: '?error=Not%20found',
                href: 'http://localhost/?error=Not%20found',
            },
            writable: true,
        });

        service.checkUrlMessages();

        const msg = container.querySelector('.toast-message');
        expect(msg.textContent).toBe('Not found');
    });

    test('shows success toast from URL', () => {
        Object.defineProperty(window, 'location', {
            value: {
                search: '?success=Saved',
                href: 'http://localhost/?success=Saved',
            },
            writable: true,
        });

        service.checkUrlMessages();

        const toast = container.querySelector('.toast-success');
        expect(toast).not.toBeNull();
    });

    test('shows error toast from URL', () => {
        Object.defineProperty(window, 'location', {
            value: {
                search: '?error=Failed',
                href: 'http://localhost/?error=Failed',
            },
            writable: true,
        });

        service.checkUrlMessages();

        const toast = container.querySelector('.toast-error');
        expect(toast).not.toBeNull();
    });

    test('shows both success and error when both params present', () => {
        Object.defineProperty(window, 'location', {
            value: {
                search: '?success=OK&error=Also+bad',
                href: 'http://localhost/?success=OK&error=Also+bad',
            },
            writable: true,
        });

        service.checkUrlMessages();

        expect(container.querySelectorAll('.toast').length).toBe(2);
    });

    test('does nothing when no params present', () => {
        Object.defineProperty(window, 'location', {
            value: {
                search: '',
                href: 'http://localhost/',
            },
            writable: true,
        });

        service.checkUrlMessages();

        expect(container.querySelectorAll('.toast').length).toBe(0);
    });
});

// ─── XSS protection ──────────────────────────────────────────────────────────

describe('ToastService XSS protection', () => {
    let container;
    let service;

    beforeEach(() => {
        container = setupContainer();
        service = new ToastService('toastContainer');
        jest.useFakeTimers();
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    test('escapes HTML in message', () => {
        service.show('<script>alert(1)</script>', 'error');
        const html = container.querySelector('.toast-message').innerHTML;
        expect(html).not.toContain('<script>');
    });
});
