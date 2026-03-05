import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';
import {
    htmlUtils,
    stringUtils,
    domUtils,
    securityUtils,
    entityUtils,
    ICONS,
    CONSTANTS
} from '@/tableScripts/core/utils.js';

describe('htmlUtils', () => {
    describe('escape', () => {
        test('should escape HTML special characters', () => {
            expect(htmlUtils.escape('<script>alert("xss")</script>'))
             .toBe('&lt;script&gt;alert(&quot;xss&quot;)&lt;/script&gt;');
        });

        test('should escape ampersands', () => {
            expect(htmlUtils.escape('Tom & Jerry')).toContain('&amp;');
        });

        test('should escape quotes', () => {
     const result = htmlUtils.escape('"quotes"');
                expect(result).toBe('&quot;quotes&quot;');
        });

        test('should handle empty strings', () => {
            expect(htmlUtils.escape('')).toBe('');
        });

        test('should handle null', () => {
            expect(htmlUtils.escape(null)).toBe('');
        });

        test('should handle undefined', () => {
            expect(htmlUtils.escape(undefined)).toBe('');
        });

        test('should handle strings without special characters', () => {
            expect(htmlUtils.escape('Hello World')).toBe('Hello World');
        });

        test('should handle multiple special characters', () => {
            const result = htmlUtils.escape('<div class="test">A & B</div>');
            expect(result).toContain('&lt;');
            expect(result).toContain('&gt;');
            expect(result).toContain('&quot;');
            expect(result).toContain('&amp;');
        });
    });

    describe('decode', () => {
        test('should decode HTML entities', () => {
            expect(htmlUtils.decode('&lt;div&gt;')).toBe('<div>');
        });

        test('should decode ampersands', () => {
            expect(htmlUtils.decode('Tom &amp; Jerry')).toBe('Tom & Jerry');
        });

        test('should decode quotes', () => {
            expect(htmlUtils.decode('&quot;test&quot;')).toBe('"test"');
        });

        test('should handle empty strings', () => {
            expect(htmlUtils.decode('')).toBe('');
        });

        test('should handle null', () => {
            expect(htmlUtils.decode(null)).toBe('');
        });

        test('should handle undefined', () => {
            expect(htmlUtils.decode(undefined)).toBe('');
        });

        test('should handle mixed content', () => {
            const result = htmlUtils.decode('&lt;p&gt;Hello &amp; Goodbye&lt;/p&gt;');
            expect(result).toBe('<p>Hello & Goodbye</p>');
        });
    });
});

describe('stringUtils', () => {
    describe('capitalize', () => {
        test('should capitalize first letter', () => {
            expect(stringUtils.capitalize('hello')).toBe('Hello');
        });

        test('should not change already capitalized strings', () => {
            expect(stringUtils.capitalize('Hello')).toBe('Hello');
        });

        test('should handle empty strings', () => {
            expect(stringUtils.capitalize('')).toBe('');
        });

        test('should handle null', () => {
            expect(stringUtils.capitalize(null)).toBe('');
        });

        test('should handle undefined', () => {
            expect(stringUtils.capitalize(undefined)).toBe('');
        });

        test('should only capitalize first character', () => {
            expect(stringUtils.capitalize('hELLO')).toBe('HELLO');
        });

        test('should handle single character strings', () => {
            expect(stringUtils.capitalize('a')).toBe('A');
        });

        test('should handle strings starting with numbers', () => {
            expect(stringUtils.capitalize('123abc')).toBe('123abc');
        });
    });
});

describe('domUtils', () => {
    describe('createElement', () => {
        test('should create element with tag name', () => {
            const el = domUtils.createElement('div');
            expect(el.tagName).toBe('DIV');
        });

        test('should set className', () => {
            const el = domUtils.createElement('div', { className: 'test-class' });
            expect(el.className).toBe('test-class');
        });

        test('should set attributes', () => {
            const el = domUtils.createElement('input', {
                type: 'text',
                placeholder: 'Enter text'
            });
            expect(el.getAttribute('type')).toBe('text');
            expect(el.getAttribute('placeholder')).toBe('Enter text');
        });

        test('should set dataset', () => {
            const el = domUtils.createElement('div', {
                dataset: { id: '123', name: 'test' }
            });
            expect(el.dataset.id).toBe('123');
            expect(el.dataset.name).toBe('test');
        });

        test('should add event listeners', () => {
            const onClick = jest.fn();
            const el = domUtils.createElement('button', { onClick });

            el.click();
            expect(onClick).toHaveBeenCalled();
        });

        test('should handle string children', () => {
            const el = domUtils.createElement('div', {}, 'Hello World');
            expect(el.textContent).toBe('Hello World');
        });

        test('should handle array of string children', () => {
            const el = domUtils.createElement('div', {}, ['Hello', ' ', 'World']);
            expect(el.textContent).toBe('Hello World');
        });

        test('should handle array of element children', () => {
            const child1 = document.createElement('span');
            child1.textContent = 'Child 1';
            const child2 = document.createElement('span');
            child2.textContent = 'Child 2';

            const el = domUtils.createElement('div', {}, [child1, child2]);
            expect(el.children.length).toBe(2);
        });

        test('should handle mixed children', () => {
            const span = document.createElement('span');
            span.textContent = 'Span';

            const el = domUtils.createElement('div', {}, ['Text', span]);
            expect(el.childNodes.length).toBe(2);
        });

        test('should handle empty attributes', () => {
            const el = domUtils.createElement('div', {});
            expect(el.tagName).toBe('DIV');
        });

        test('should handle empty children', () => {
            const el = domUtils.createElement('div', {}, []);
            expect(el.childNodes.length).toBe(0);
        });
    });
});

describe('securityUtils', () => {
    beforeEach(() => {
        document.head.innerHTML = '';
    });

    describe('getCsrfToken', () => {
        test('should get CSRF token from meta tag', () => {
            const meta = document.createElement('meta');
            meta.name = '_csrf_token';
            meta.content = 'test-token-123';
            document.head.appendChild(meta);

            expect(securityUtils.getCsrfToken()).toBe('test-token-123');
        });

        test('should return null when meta tag not found', () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

            expect(securityUtils.getCsrfToken()).toBeNull();
            expect(consoleErrorSpy).toHaveBeenCalled();

            consoleErrorSpy.mockRestore();
        });

        test('should handle empty token', () => {
            const meta = document.createElement('meta');
            meta.name = '_csrf_token';
            meta.content = '';
            document.head.appendChild(meta);

            expect(securityUtils.getCsrfToken()).toBe('');
        });
    });

    describe('createCsrfInput', () => {
        test('should create hidden input with CSRF token', () => {
            const meta = document.createElement('meta');
            meta.name = '_csrf_token';
            meta.content = 'test-token';
            document.head.appendChild(meta);

            const input = securityUtils.createCsrfInput();

            expect(input.type).toBe('hidden');
            expect(input.name).toBe('_csrf');
            expect(input.value).toBe('test-token');
        });

        test('should create input with empty value when token not found', () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

            const input = securityUtils.createCsrfInput();

            expect(input.value).toBe('');
            consoleErrorSpy.mockRestore();
        });
    });

    describe('addCsrfToFormData', () => {
        test('should add CSRF token to FormData', () => {
            const meta = document.createElement('meta');
            meta.name = '_csrf_token';
            meta.content = 'test-token';
            document.head.appendChild(meta);

            const formData = new FormData();
            securityUtils.addCsrfToFormData(formData);

            expect(formData.get('_csrf')).toBe('test-token');
        });

        test('should not add token when not found', () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

            const formData = new FormData();
            securityUtils.addCsrfToFormData(formData);

            expect(formData.get('_csrf')).toBeNull();
            consoleErrorSpy.mockRestore();
        });

        test('should return FormData for chaining', () => {
            const meta = document.createElement('meta');
            meta.name = '_csrf_token';
            meta.content = 'test-token';
            document.head.appendChild(meta);

            const formData = new FormData();
            const result = securityUtils.addCsrfToFormData(formData);

            expect(result).toBe(formData);
        });
    });
});

describe('entityUtils', () => {
    describe('getItemTypeName', () => {
        test('should return "Tags" for games', () => {
            expect(entityUtils.getItemTypeName('games')).toBe('Tags');
        });

        test('should return "Genres" for movies', () => {
            expect(entityUtils.getItemTypeName('movies')).toBe('Genres');
        });

        test('should return "Genres" for books', () => {
            expect(entityUtils.getItemTypeName('books')).toBe('Genres');
        });

        test('should return "Genres" for shows', () => {
            expect(entityUtils.getItemTypeName('shows')).toBe('Genres');
        });
    });

    describe('getAvailableItems', () => {
        beforeEach(() => {
            delete window.allAvailableTags;
            delete window.allAvailableGenres;
        });

        test('should return tags for games', () => {
            window.allAvailableTags = [{ id: 1, name: 'Action' }];

            expect(entityUtils.getAvailableItems('games')).toEqual([{ id: 1, name: 'Action' }]);
        });

        test('should return genres for movies', () => {
            window.allAvailableGenres = [{ id: 1, name: 'Drama' }];

            expect(entityUtils.getAvailableItems('movies')).toEqual([{ id: 1, name: 'Drama' }]);
        });

        test('should return empty array when not defined', () => {
            expect(entityUtils.getAvailableItems('games')).toEqual([]);
            expect(entityUtils.getAvailableItems('movies')).toEqual([]);
        });
    });

    describe('getSingularName', () => {
        test('should return singular names correctly', () => {
            expect(entityUtils.getSingularName('games')).toBe('Game');
            expect(entityUtils.getSingularName('movies')).toBe('Movie');
            expect(entityUtils.getSingularName('books')).toBe('Book');
            expect(entityUtils.getSingularName('shows')).toBe('Show');
        });

        test('should return original name for unknown type', () => {
            expect(entityUtils.getSingularName('unknown')).toBe('unknown');
        });
    });
});

describe('ICONS', () => {
    test('should have all required icons', () => {
        expect(ICONS.COMPLETED).toBeDefined();
        expect(ICONS.NOT_COMPLETED).toBeDefined();
        expect(ICONS.EDIT).toBeDefined();
        expect(ICONS.DELETE).toBeDefined();
        expect(ICONS.SAVE).toBeDefined();
        expect(ICONS.CANCEL).toBeDefined();
        expect(ICONS.ARROW_UP).toBeDefined();
        expect(ICONS.ARROW_DOWN).toBeDefined();
    });

    test('icons should be strings', () => {
        Object.values(ICONS).forEach(icon => {
            expect(typeof icon).toBe('string');
        });
    });

    test('icons should not be empty', () => {
        Object.values(ICONS).forEach(icon => {
            expect(icon.length).toBeGreaterThan(0);
        });
    });
});

describe('CONSTANTS', () => {
    test('should have SEARCH_DEBOUNCE_MS', () => {
        expect(CONSTANTS.SEARCH_DEBOUNCE_MS).toBe(500);
    });

    test('should have AJAX headers', () => {
        expect(CONSTANTS.AJAX_HEADER).toBe('X-Requested-With-AJAX');
        expect(CONSTANTS.AJAX_HEADER_VALUE).toBe('true');
    });

    test('should have DEFAULT_ROWS_PER_PAGE', () => {
        expect(CONSTANTS.DEFAULT_ROWS_PER_PAGE).toBe(10);
    });

    test('should have SCORE_THRESHOLDS for all entity types', () => {
        expect(CONSTANTS.SCORE_THRESHOLDS.GAMES).toEqual({ low: 49, medium: 74 });
        expect(CONSTANTS.SCORE_THRESHOLDS.MOVIES).toEqual({ low: 39, medium: 60 });
        expect(CONSTANTS.SCORE_THRESHOLDS.SHOWS).toEqual({ low: 39, medium: 60 });
        expect(CONSTANTS.SCORE_THRESHOLDS.BOOKS).toEqual({ low: 49, medium: 79 });
    });
});