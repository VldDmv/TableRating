import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';
import { CoverClickHandler } from '@/tableScripts/features/cover/coverClickHandler.js';

describe('CoverClickHandler', () => {
    let handler;
    let tableBody;
    let mockConfig;
    let onCoverUpdated;

    beforeEach(() => {
        const meta = document.createElement('meta');
        meta.name = '_csrf_token';
        meta.content = 'test-token';
        document.head.appendChild(meta);

        tableBody = document.createElement('tbody');
        const row = document.createElement('tr');
        row.dataset.originalName = 'Test Game';
        row.dataset.originalCoverUrl = 'http://example.com/old.jpg';
        row.innerHTML = `
            <td><img class="cover-thumbnail" src="http://example.com/old.jpg" alt="Test Game"/></td>
            <td>Test Game</td>
        `;
        tableBody.appendChild(row);
        document.body.appendChild(tableBody);

        mockConfig = { entityType: 'games' };
        onCoverUpdated = jest.fn().mockResolvedValue(undefined);

        handler = new CoverClickHandler(tableBody, mockConfig, onCoverUpdated);
        global.fetch = jest.fn();
        global.prompt = jest.fn();
        global.alert = jest.fn();
    });

    afterEach(() => {
        document.body.innerHTML = '';
        document.head.innerHTML = '';
    });

    describe('Constructor', () => {
        test('should store tableBody, config, and callback', () => {
            expect(handler.tableBody).toBe(tableBody);
            expect(handler.config).toBe(mockConfig);
            expect(handler.onCoverUpdated).toBe(onCoverUpdated);
        });
    });

    describe('_isValidUrl', () => {
        test('should return true for valid URL', () => {
            expect(handler._isValidUrl('https://example.com/img.jpg')).toBe(true);
        });

        test('should return false for invalid URL', () => {
            expect(handler._isValidUrl('not-a-url')).toBe(false);
        });

        test('should return false for empty string', () => {
            expect(handler._isValidUrl('')).toBe(false);
        });
    });

    describe('init — click handling', () => {
        test('should not trigger edit on plain thumbnail click (no ctrl)', () => {
            handler.init();
            const img = tableBody.querySelector('.cover-thumbnail');
            img.dispatchEvent(new MouseEvent('click', { bubbles: true }));
            expect(global.prompt).not.toHaveBeenCalled();
        });

        test('should trigger edit on Ctrl+click of thumbnail', async () => {
            global.prompt.mockReturnValue(null); // user cancels
            handler.init();
            const img = tableBody.querySelector('.cover-thumbnail');
            img.dispatchEvent(new MouseEvent('click', { bubbles: true, ctrlKey: true }));
            await new Promise((r) => setTimeout(r, 0));
            expect(global.prompt).toHaveBeenCalled();
        });

        test('should trigger edit on click of placeholder', async () => {
            global.prompt.mockReturnValue(null);
            tableBody.innerHTML = `
                <tr data-original-name="Test" data-original-cover-url="">
                    <td><div class="cover-placeholder">📦</div></td>
                </tr>`;
            handler.init();
            const placeholder = tableBody.querySelector('.cover-placeholder');
            placeholder.dispatchEvent(new MouseEvent('click', { bubbles: true }));
            await new Promise((r) => setTimeout(r, 0));
            expect(global.prompt).toHaveBeenCalled();
        });
    });

    describe('_showEditPrompt', () => {
        test('should do nothing when user cancels prompt (returns null)', async () => {
            global.prompt.mockReturnValue(null);
            await handler._showEditPrompt('Test Game', 'http://example.com/old.jpg');
            expect(global.fetch).not.toHaveBeenCalled();
        });

        test('should show alert for invalid URL', async () => {
            global.prompt.mockReturnValue('not-a-url');
            await handler._showEditPrompt('Test Game', '');
            expect(global.alert).toHaveBeenCalledWith(expect.stringContaining('Invalid URL'));
        });

        test('should call _updateCover with valid URL', async () => {
            global.prompt.mockReturnValue('https://example.com/new.jpg');
            global.fetch.mockResolvedValue({ ok: true });
            const spy = jest.spyOn(handler, '_updateCover');
            await handler._showEditPrompt('Test Game', '');
            expect(spy).toHaveBeenCalledWith('Test Game', 'https://example.com/new.jpg');
        });

        test('should allow empty string (remove cover)', async () => {
            global.prompt.mockReturnValue('');
            global.fetch.mockResolvedValue({ ok: true });
            const spy = jest.spyOn(handler, '_updateCover');
            await handler._showEditPrompt('Test Game', 'http://old.com');
            expect(spy).toHaveBeenCalledWith('Test Game', '');
        });
    });

    describe('_updateCover', () => {
        test('should call fetch with PATCH method', async () => {
            global.fetch.mockResolvedValue({ ok: true });
            await handler._updateCover('Test Game', 'https://example.com/new.jpg');
            expect(global.fetch).toHaveBeenCalledWith(
                expect.stringContaining('/games/'),
                expect.objectContaining({ method: 'PATCH' })
            );
        });

        test('should include XSRF token header', async () => {
            global.fetch.mockResolvedValue({ ok: true });
            await handler._updateCover('Test Game', 'https://example.com/new.jpg');
            const [, opts] = global.fetch.mock.calls[0];
            expect(opts.headers['X-XSRF-TOKEN']).toBe('test-token');
        });

        test('should call onCoverUpdated on success', async () => {
            global.fetch.mockResolvedValue({ ok: true });
            await handler._updateCover('Test Game', 'https://example.com/new.jpg');
            expect(onCoverUpdated).toHaveBeenCalled();
        });

        test('should show alert on missing CSRF token', async () => {
            document.head.innerHTML = '';
            await handler._updateCover('Test Game', 'https://example.com/new.jpg');
            expect(global.alert).toHaveBeenCalledWith(expect.stringContaining('Security token'));
            expect(global.fetch).not.toHaveBeenCalled();
        });

        test('should show alert on fetch failure', async () => {
            global.fetch.mockResolvedValue({ ok: false, text: async () => 'Server Error' });
            await handler._updateCover('Test Game', 'https://example.com/new.jpg');
            expect(global.alert).toHaveBeenCalled();
        });
    });

    describe('coverEditRequested event', () => {
        test('should handle coverEditRequested custom event', async () => {
            global.prompt.mockReturnValue(null);
            handler.init();
            document.dispatchEvent(
                new CustomEvent('coverEditRequested', {
                    detail: { itemName: 'Test Game', currentUrl: 'http://old.com/img.jpg' },
                })
            );
            await new Promise((r) => setTimeout(r, 0));
            expect(global.prompt).toHaveBeenCalled();
        });
    });
});
