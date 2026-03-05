import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';
import { ErrorHandler } from '@/tableScripts/core/errorHandler.js';

describe('ErrorHandler', () => {
    let consoleErrorSpy;

    beforeEach(() => {
        consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
        global.alert = jest.fn();
    });

    afterEach(() => {
        consoleErrorSpy.mockRestore();
    });

    describe('handle', () => {
        test('should log error to console', () => {
            const error = new Error('Test error');

            ErrorHandler.handle(error, 'User message');

            expect(consoleErrorSpy).toHaveBeenCalledWith('Error occurred:', error);
        });

        test('should show alert when no container provided', () => {
            const error = new Error('Test error');

            ErrorHandler.handle(error, 'User message');

            expect(global.alert).toHaveBeenCalledWith(
                expect.stringContaining('User message')
            );
            expect(global.alert).toHaveBeenCalledWith(
                expect.stringContaining('Test error')
            );
        });

        test('should display error in container when provided', () => {
            const container = document.createElement('div');
            const error = new Error('Test error');

            ErrorHandler.handle(error, 'User message', container);

            expect(container.innerHTML).toContain('User message');
            expect(container.innerHTML).toContain('Test error');
            expect(global.alert).not.toHaveBeenCalled();
        });

        test('should escape HTML in error messages', () => {
            const container = document.createElement('div');
            const error = new Error('<script>alert("xss")</script>');

            ErrorHandler.handle(error, 'User <b>message</b>', container);

            expect(container.innerHTML).not.toContain('<script>');
            expect(container.innerHTML).toContain('&lt;script&gt;');
            expect(container.innerHTML).toContain('&lt;b&gt;message&lt;/b&gt;');
        });

        test('should handle errors with empty messages', () => {
            const error = new Error('');
            const container = document.createElement('div');

            ErrorHandler.handle(error, 'User message', container);

            expect(container.innerHTML).toContain('User message');
        });


        test('should handle null error objects gracefully', () => {
            const container = document.createElement('div');

            expect(() => {
                ErrorHandler.handle(null, 'User message', container);
            }).not.toThrow();


            expect(container.innerHTML).toContain('User message');
            expect(consoleErrorSpy).toHaveBeenCalledWith('Error occurred:', null);
        });
    });

    describe('showError', () => {
        test('should display error message in container', () => {
            const container = document.createElement('div');

            ErrorHandler.showError(container, 'Error Title', 'Error Details');

            expect(container.querySelector('.error-message')).toBeTruthy();
            expect(container.textContent).toContain('Error Title');
            expect(container.textContent).toContain('Error Details');
        });

        test('should apply error styling', () => {
            const container = document.createElement('div');

            ErrorHandler.showError(container, 'Title', 'Details');

            const errorDiv = container.querySelector('.error-message');
            expect(errorDiv.style.backgroundColor).toBe('rgb(255, 238, 238)');
            expect(errorDiv.style.border).toContain('#fcc');
        });

        test('should escape HTML in title and details', () => {
            const container = document.createElement('div');

            ErrorHandler.showError(
                container,
                '<script>alert(1)</script>',
                '<img src=x onerror=alert(1)>'
            );

            expect(container.innerHTML).not.toContain('<script>');
            expect(container.innerHTML).not.toContain('<img');
            expect(container.innerHTML).toContain('&lt;script&gt;');
            expect(container.innerHTML).toContain('&lt;img');
        });

        test('should handle empty strings', () => {
            const container = document.createElement('div');

            ErrorHandler.showError(container, '', '');

            expect(container.querySelector('.error-message')).toBeTruthy();
        });

        test('should replace existing content in container', () => {
            const container = document.createElement('div');
            container.innerHTML = '<p>Old content</p>';

            ErrorHandler.showError(container, 'Title', 'Details');

            expect(container.textContent).not.toContain('Old content');
            expect(container.textContent).toContain('Title');
        });
    });

    describe('showLoading', () => {
        test('should display loading message in container', () => {
            const container = document.createElement('div');

            ErrorHandler.showLoading(container);

            expect(container.querySelector('.loading-indicator')).toBeTruthy();
            expect(container.textContent).toContain('Loading...');
        });

        test('should display custom loading message', () => {
            const container = document.createElement('div');

            ErrorHandler.showLoading(container, 'Please wait...');

            expect(container.textContent).toContain('Please wait...');
        });

        test('should escape HTML in loading message', () => {
            const container = document.createElement('div');

            ErrorHandler.showLoading(container, '<script>alert(1)</script>');

            expect(container.innerHTML).not.toContain('<script>');
            expect(container.innerHTML).toContain('&lt;script&gt;');
        });

        test('should apply loading styling', () => {
            const container = document.createElement('div');

            ErrorHandler.showLoading(container);

            const loadingDiv = container.querySelector('.loading-indicator');
            expect(loadingDiv.style.padding).toBe('20px');
            expect(loadingDiv.style.textAlign).toBe('center');
        });

        test('should replace existing content', () => {
            const container = document.createElement('div');
            container.innerHTML = '<p>Old content</p>';

            ErrorHandler.showLoading(container);

            expect(container.textContent).not.toContain('Old content');
        });

        test('should handle empty message string', () => {
            const container = document.createElement('div');

            ErrorHandler.showLoading(container, '');

            expect(container.querySelector('.loading-indicator')).toBeTruthy();
        });
    });

    describe('parseErrorResponse', () => {
        test('should parse JSON error response', async () => {
            const response = {
                ok: false,
                status: 400,
                text: async () => JSON.stringify({ message: 'Bad Request' })
            };

            const message = await ErrorHandler.parseErrorResponse(response);

            expect(message).toBe('Bad Request');
        });

        test('should return default message when no message in JSON', async () => {
            const response = {
                ok: false,
                status: 404,
                text: async () => JSON.stringify({})
            };

            const message = await ErrorHandler.parseErrorResponse(response);

            expect(message).toBe('HTTP error 404');
        });

        test('should handle plain text error response', async () => {
            const response = {
                ok: false,
                status: 500,
                text: async () => 'Internal Server Error'
            };

            const message = await ErrorHandler.parseErrorResponse(response);

            expect(message).toBe('Internal Server Error');
        });

        test('should handle empty text response', async () => {
            const response = {
                ok: false,
                status: 503,
                text: async () => ''
            };

            const message = await ErrorHandler.parseErrorResponse(response);

            expect(message).toBe('HTTP error 503');
        });

        test('should handle text() method throwing error', async () => {
            const response = {
                ok: false,
                status: 500,
                text: async () => {
                    throw new Error('Cannot read response');
                }
            };

            const message = await ErrorHandler.parseErrorResponse(response);

            expect(message).toBe('HTTP error 500');
        });

        test('should handle invalid JSON gracefully', async () => {
            const response = {
                ok: false,
                status: 400,
                text: async () => 'Not valid JSON {'
            };

            const message = await ErrorHandler.parseErrorResponse(response);

            expect(message).toBe('Not valid JSON {');
        });

        test('should handle various HTTP status codes', async () => {
            const statuses = [400, 401, 403, 404, 500, 502, 503];

            for (const status of statuses) {
                const response = {
                    ok: false,
                    status,
                    text: async () => ''
                };

                const message = await ErrorHandler.parseErrorResponse(response);
                expect(message).toBe(`HTTP error ${status}`);
            }
        });

        test('should prioritize message from JSON over status code', async () => {
            const response = {
                ok: false,
                status: 400,
                text: async () => JSON.stringify({ message: 'Custom error message' })
            };

            const message = await ErrorHandler.parseErrorResponse(response);

            expect(message).toBe('Custom error message');
            expect(message).not.toContain('400');
        });
    });

    describe('Integration Tests', () => {
        test('should handle complete error flow with container', () => {
            const container = document.createElement('div');
            const error = new Error('Database connection failed');

            ErrorHandler.handle(error, 'Failed to load data', container);

            expect(consoleErrorSpy).toHaveBeenCalled();
            expect(container.querySelector('.error-message')).toBeTruthy();
            expect(container.textContent).toContain('Failed to load data');
            expect(container.textContent).toContain('Database connection failed');
        });

        test('should handle complete error flow without container', () => {
            const error = new Error('Network timeout');

            ErrorHandler.handle(error, 'Request failed');

            expect(consoleErrorSpy).toHaveBeenCalled();
            expect(global.alert).toHaveBeenCalled();
        });

        test('should handle switching from loading to error', () => {
            const container = document.createElement('div');

            ErrorHandler.showLoading(container, 'Loading data...');
            expect(container.textContent).toContain('Loading data...');

            ErrorHandler.showError(container, 'Error', 'Failed to load');
            expect(container.textContent).not.toContain('Loading data...');
            expect(container.textContent).toContain('Failed to load');
        });

        test('should handle multiple errors in same container', () => {
            const container = document.createElement('div');

            ErrorHandler.showError(container, 'Error 1', 'Details 1');
            expect(container.textContent).toContain('Error 1');

            ErrorHandler.showError(container, 'Error 2', 'Details 2');
            expect(container.textContent).not.toContain('Error 1');
            expect(container.textContent).toContain('Error 2');
        });
    });

    describe('Edge Cases', () => {
        test('should handle very long error messages', () => {
            const container = document.createElement('div');
            const longMessage = 'A'.repeat(10000);

            ErrorHandler.showError(container, 'Title', longMessage);

            expect(container.textContent).toContain('Title');
            expect(container.textContent.length).toBeGreaterThan(9000);
        });

        test('should handle special characters in error messages', () => {
            const container = document.createElement('div');

            ErrorHandler.showError(
                container,
                'Error: <>&"\'',
                'Details: <>&"\''
            );

            expect(container.innerHTML).toContain('&lt;');
            expect(container.innerHTML).toContain('&gt;');
            expect(container.innerHTML).toContain('&amp;');
        });

        test('should handle unicode characters', () => {
            const container = document.createElement('div');

            ErrorHandler.showError(
                container,
                'Error: 你好 🎮',
                'Details: テスト ⚠️'
            );

            expect(container.textContent).toContain('你好 🎮');
            expect(container.textContent).toContain('テスト ⚠️');
        });

        test('should handle null container gracefully in showError', () => {
            expect(() => {
                ErrorHandler.showError(null, 'Title', 'Details');
            }).toThrow();
        });

        test('should handle undefined values', () => {
            const container = document.createElement('div');

            ErrorHandler.showError(container, undefined, undefined);

            expect(container.querySelector('.error-message')).toBeTruthy();
        });
    });
});