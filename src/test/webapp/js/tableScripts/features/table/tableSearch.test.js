
import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';
import { SearchManager } from '@/tableScripts/features/table/tableSearch.js';

describe('SearchManager', () => {
    let searchManager;
    let searchInput;

    beforeEach(() => {
        jest.useFakeTimers();
        searchInput = document.createElement('input');
        searchInput.type = 'text';
        searchManager = new SearchManager(searchInput, 500);
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    describe('Constructor', () => {
        test('should initialize with search input', () => {
            expect(searchManager.searchInput).toBe(searchInput);
        });

        test('should initialize with debounce delay', () => {
            expect(searchManager.debounceMs).toBe(500);
        });

        test('should use default debounce delay if not provided', () => {
            const manager = new SearchManager(searchInput);
            expect(manager.debounceMs).toBe(500);
        });

        test('should initialize with empty listeners array', () => {
            expect(searchManager.listeners).toEqual([]);
        });

        test('should initialize debounceTimeout as null', () => {
            expect(searchManager.debounceTimeout).toBeNull();
        });
    });

    describe('onSearch', () => {
        test('should add search listener', () => {
            const callback = jest.fn();

            searchManager.onSearch(callback);

            expect(searchManager.listeners).toContain(callback);
        });

        test('should return instance for chaining', () => {
            const callback = jest.fn();

            const result = searchManager.onSearch(callback);

            expect(result).toBe(searchManager);
        });

        test('should allow multiple listeners', () => {
            const callback1 = jest.fn();
            const callback2 = jest.fn();

            searchManager.onSearch(callback1);
            searchManager.onSearch(callback2);

            expect(searchManager.listeners.length).toBe(2);
        });
    });

    describe('init', () => {
        test('should attach input event listener', () => {
            searchManager.init();

            const spy = jest.spyOn(searchInput, 'addEventListener');
            const manager = new SearchManager(searchInput);
            manager.init();

            expect(spy).toHaveBeenCalledWith('input', expect.any(Function));
        });

        test('should not throw when search input is null', () => {
            const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();
            const manager = new SearchManager(null);

            expect(() => manager.init()).not.toThrow();
            expect(consoleWarnSpy).toHaveBeenCalled();

            consoleWarnSpy.mockRestore();
        });

        test('should not throw when search input is undefined', () => {
            const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();
            const manager = new SearchManager(undefined);

            expect(() => manager.init()).not.toThrow();

            consoleWarnSpy.mockRestore();
        });
    });

    describe('Debouncing', () => {
        test('should debounce search input', () => {
            const callback = jest.fn();
            searchManager.onSearch(callback);
            searchManager.init();

            searchInput.value = 'test';
            searchInput.dispatchEvent(new Event('input'));

            expect(callback).not.toHaveBeenCalled();

            jest.advanceTimersByTime(500);

            expect(callback).toHaveBeenCalledWith('test');
        });

        test('should clear previous timeout on new input', () => {
            const callback = jest.fn();
            searchManager.onSearch(callback);
            searchManager.init();

            searchInput.value = 'test1';
            searchInput.dispatchEvent(new Event('input'));

            jest.advanceTimersByTime(200);

            searchInput.value = 'test2';
            searchInput.dispatchEvent(new Event('input'));

            jest.advanceTimersByTime(500);

            expect(callback).toHaveBeenCalledTimes(1);
            expect(callback).toHaveBeenCalledWith('test2');
        });

        test('should wait full debounce time after last input', () => {
            const callback = jest.fn();
            searchManager.onSearch(callback);
            searchManager.init();

            searchInput.value = 't';
            searchInput.dispatchEvent(new Event('input'));
            jest.advanceTimersByTime(100);

            searchInput.value = 'te';
            searchInput.dispatchEvent(new Event('input'));
            jest.advanceTimersByTime(100);

            searchInput.value = 'tes';
            searchInput.dispatchEvent(new Event('input'));
            jest.advanceTimersByTime(100);

            searchInput.value = 'test';
            searchInput.dispatchEvent(new Event('input'));

            expect(callback).not.toHaveBeenCalled();

            jest.advanceTimersByTime(500);

            expect(callback).toHaveBeenCalledTimes(1);
            expect(callback).toHaveBeenCalledWith('test');
        });

        test('should use custom debounce delay', () => {
            const callback = jest.fn();
            const manager = new SearchManager(searchInput, 1000);
            manager.onSearch(callback);
            manager.init();

            searchInput.value = 'test';
            searchInput.dispatchEvent(new Event('input'));

            jest.advanceTimersByTime(500);
            expect(callback).not.toHaveBeenCalled();

            jest.advanceTimersByTime(500);
            expect(callback).toHaveBeenCalledWith('test');
        });

        test('should handle rapid typing', () => {
            const callback = jest.fn();
            searchManager.onSearch(callback);
            searchManager.init();

            for (let i = 0; i < 10; i++) {
                searchInput.value = `test${i}`;
                searchInput.dispatchEvent(new Event('input'));
                jest.advanceTimersByTime(50);
            }

            jest.advanceTimersByTime(500);

            expect(callback).toHaveBeenCalledTimes(1);
            expect(callback).toHaveBeenCalledWith('test9');
        });
    });

    describe('Search Term Processing', () => {
        test('should trim search term', () => {
            const callback = jest.fn();
            searchManager.onSearch(callback);
            searchManager.init();

            searchInput.value = '  test  ';
            searchInput.dispatchEvent(new Event('input'));

            jest.advanceTimersByTime(500);

            expect(callback).toHaveBeenCalledWith('test');
        });

        test('should handle empty search term', () => {
            const callback = jest.fn();
            searchManager.onSearch(callback);
            searchManager.init();

            searchInput.value = '';
            searchInput.dispatchEvent(new Event('input'));

            jest.advanceTimersByTime(500);

            expect(callback).toHaveBeenCalledWith('');
        });

        test('should handle whitespace-only search term', () => {
            const callback = jest.fn();
            searchManager.onSearch(callback);
            searchManager.init();

            searchInput.value = '   ';
            searchInput.dispatchEvent(new Event('input'));

            jest.advanceTimersByTime(500);

            expect(callback).toHaveBeenCalledWith('');
        });

        test('should handle special characters', () => {
            const callback = jest.fn();
            searchManager.onSearch(callback);
            searchManager.init();

            searchInput.value = '!@#$%^&*()';
            searchInput.dispatchEvent(new Event('input'));

            jest.advanceTimersByTime(500);

            expect(callback).toHaveBeenCalledWith('!@#$%^&*()');
        });

        test('should handle unicode characters', () => {
            const callback = jest.fn();
            searchManager.onSearch(callback);
            searchManager.init();

            searchInput.value = '测试 🎮';
            searchInput.dispatchEvent(new Event('input'));

            jest.advanceTimersByTime(500);

            expect(callback).toHaveBeenCalledWith('测试 🎮');
        });
    });

    describe('Multiple Listeners', () => {
        test('should call all registered listeners', () => {
            const callback1 = jest.fn();
            const callback2 = jest.fn();
            const callback3 = jest.fn();

            searchManager.onSearch(callback1);
            searchManager.onSearch(callback2);
            searchManager.onSearch(callback3);
            searchManager.init();

            searchInput.value = 'test';
            searchInput.dispatchEvent(new Event('input'));

            jest.advanceTimersByTime(500);

            expect(callback1).toHaveBeenCalledWith('test');
            expect(callback2).toHaveBeenCalledWith('test');
            expect(callback3).toHaveBeenCalledWith('test');
        });

        test('should call listeners with same value', () => {
            const callback1 = jest.fn();
            const callback2 = jest.fn();

            searchManager.onSearch(callback1);
            searchManager.onSearch(callback2);
            searchManager.init();

            searchInput.value = 'test';
            searchInput.dispatchEvent(new Event('input'));

            jest.advanceTimersByTime(500);

            const calls1 = callback1.mock.calls[0][0];
            const calls2 = callback2.mock.calls[0][0];

            expect(calls1).toBe(calls2);
        });
    });

    describe('clear', () => {
        test('should clear search input value', () => {
            searchInput.value = 'test';

            searchManager.clear();

            expect(searchInput.value).toBe('');
        });

        test('should handle null search input', () => {
            const manager = new SearchManager(null);

            expect(() => manager.clear()).not.toThrow();
        });

        test('should handle undefined search input', () => {
            const manager = new SearchManager(undefined);

            expect(() => manager.clear()).not.toThrow();
        });
    });

    describe('getValue', () => {
        test('should return current search value', () => {
            searchInput.value = 'test query';

            expect(searchManager.getValue()).toBe('test query');
        });

        test('should trim returned value', () => {
            searchInput.value = '  test  ';

            expect(searchManager.getValue()).toBe('test');
        });

        test('should return empty string when search input is null', () => {
            const manager = new SearchManager(null);

            expect(manager.getValue()).toBe('');
        });

        test('should return empty string when search input is undefined', () => {
            const manager = new SearchManager(undefined);

            expect(manager.getValue()).toBe('');
        });

        test('should return empty string for whitespace', () => {
            searchInput.value = '   ';

            expect(searchManager.getValue()).toBe('');
        });
    });

    describe('Integration', () => {
        test('should handle complete search flow', () => {
            const callback = jest.fn();
            searchManager.onSearch(callback).init();


            searchInput.value = 'mario';
            searchInput.dispatchEvent(new Event('input'));

            jest.advanceTimersByTime(500);

            expect(callback).toHaveBeenCalledWith('mario');

            searchManager.clear();
            expect(searchInput.value).toBe('');

            searchInput.value = 'zelda';
            searchInput.dispatchEvent(new Event('input'));

            jest.advanceTimersByTime(500);

            expect(callback).toHaveBeenCalledWith('zelda');
        });

        test('should handle search, clear, and re-search', () => {
            const callback = jest.fn();
            searchManager.onSearch(callback).init();

            searchInput.value = 'test1';
            searchInput.dispatchEvent(new Event('input'));
            jest.advanceTimersByTime(500);

            searchManager.clear();

            searchInput.value = 'test2';
            searchInput.dispatchEvent(new Event('input'));
            jest.advanceTimersByTime(500);

            expect(callback).toHaveBeenCalledTimes(2);
            expect(callback).toHaveBeenNthCalledWith(1, 'test1');
            expect(callback).toHaveBeenNthCalledWith(2, 'test2');
        });

        test('should handle getValue during debounce', () => {
            searchManager.init();

            searchInput.value = 'typing...';
            searchInput.dispatchEvent(new Event('input'));


            expect(searchManager.getValue()).toBe('typing...');

            jest.advanceTimersByTime(500);

            expect(searchManager.getValue()).toBe('typing...');
        });
    });

    describe('Edge Cases', () => {
        test('should handle very long search terms', () => {
            const callback = jest.fn();
            searchManager.onSearch(callback);
            searchManager.init();

            const longTerm = 'a'.repeat(10000);
            searchInput.value = longTerm;
            searchInput.dispatchEvent(new Event('input'));

            jest.advanceTimersByTime(500);

            expect(callback).toHaveBeenCalledWith(longTerm);
        });

    test('should handle multiple rapid clears and sets', () => {
               const callback = jest.fn();
               searchManager.onSearch(callback);
               searchManager.init();

               for (let i = 0; i < 5; i++) {
                   searchInput.value = `test${i}`;
                   searchInput.dispatchEvent(new Event('input'));

                   jest.advanceTimersByTime(50);
                   searchManager.clear();
               }

               jest.advanceTimersByTime(500);

               expect(callback).toHaveBeenCalledWith('');
           });

        test('should handle zero debounce delay', () => {
            const callback = jest.fn();
            const manager = new SearchManager(searchInput, 0);
            manager.onSearch(callback);
            manager.init();

            searchInput.value = 'test';
            searchInput.dispatchEvent(new Event('input'));

            jest.advanceTimersByTime(0);

            expect(callback).toHaveBeenCalledWith('test');
        });

        test('should handle negative debounce delay', () => {
            const callback = jest.fn();
            const manager = new SearchManager(searchInput, -100);
            manager.onSearch(callback);
            manager.init();

            searchInput.value = 'test';
            searchInput.dispatchEvent(new Event('input'));

            jest.advanceTimersByTime(0);


            expect(callback).toHaveBeenCalled();
        });

        test('should handle input event without value change', () => {
            const callback = jest.fn();
            searchManager.onSearch(callback);
            searchManager.init();

            searchInput.value = 'test';
            searchInput.dispatchEvent(new Event('input'));

            jest.advanceTimersByTime(500);

            expect(callback).toHaveBeenCalledTimes(1);

            searchInput.dispatchEvent(new Event('input'));

            jest.advanceTimersByTime(500);

            expect(callback).toHaveBeenCalledTimes(2);
            expect(callback).toHaveBeenLastCalledWith('test');
        });
    });
});