import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';
import { FilterManager } from '@/tableScripts/features/table/tableFiltering.js';

describe('FilterManager', () => {
    let filterManager;
    let filterSelect;

    beforeEach(() => {
        filterSelect = document.createElement('select');

        const options = [
            { value: 'all', text: 'All' },
            { value: '1', text: 'Action' },
            { value: '2', text: 'RPG' },
            { value: '3', text: 'Strategy' },
        ];

        options.forEach((opt) => {
            const option = document.createElement('option');
            option.value = opt.value;
            option.textContent = opt.text;
            filterSelect.appendChild(option);
        });

        filterManager = new FilterManager(filterSelect);
    });

    describe('Constructor', () => {
        test('should initialize with filter select', () => {
            expect(filterManager.filterSelect).toBe(filterSelect);
        });

        test('should initialize with empty listeners array', () => {
            expect(filterManager.listeners).toEqual([]);
        });
    });

    describe('onChange', () => {
        test('should add change listener', () => {
            const callback = jest.fn();

            filterManager.onChange(callback);

            expect(filterManager.listeners).toContain(callback);
        });

        test('should return instance for chaining', () => {
            const callback = jest.fn();

            const result = filterManager.onChange(callback);

            expect(result).toBe(filterManager);
        });

        test('should allow multiple listeners', () => {
            const callback1 = jest.fn();
            const callback2 = jest.fn();

            filterManager.onChange(callback1);
            filterManager.onChange(callback2);

            expect(filterManager.listeners.length).toBe(2);
        });
    });

    describe('init', () => {
        test('should attach change event listener', () => {
            const spy = jest.spyOn(filterSelect, 'addEventListener');

            filterManager.init();

            expect(spy).toHaveBeenCalledWith('change', expect.any(Function));
        });

        test('should not throw when filter select is null', () => {
            const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();
            const manager = new FilterManager(null);

            expect(() => manager.init()).not.toThrow();
            expect(consoleWarnSpy).toHaveBeenCalled();

            consoleWarnSpy.mockRestore();
        });

        test('should not throw when filter select is undefined', () => {
            const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();
            const manager = new FilterManager(undefined);

            expect(() => manager.init()).not.toThrow();

            consoleWarnSpy.mockRestore();
        });

        test('should trigger callback on change event', () => {
            const callback = jest.fn();
            filterManager.onChange(callback);
            filterManager.init();

            filterSelect.value = '2';
            filterSelect.dispatchEvent(new Event('change'));

            expect(callback).toHaveBeenCalledWith('2');
        });
    });

    describe('Filter Changes', () => {
        test('should call listener with selected filter id', () => {
            const callback = jest.fn();
            filterManager.onChange(callback);
            filterManager.init();

            filterSelect.value = '1';
            filterSelect.dispatchEvent(new Event('change'));

            expect(callback).toHaveBeenCalledWith('1');
        });

        test('should call all registered listeners', () => {
            const callback1 = jest.fn();
            const callback2 = jest.fn();
            const callback3 = jest.fn();

            filterManager.onChange(callback1);
            filterManager.onChange(callback2);
            filterManager.onChange(callback3);
            filterManager.init();

            filterSelect.value = '3';
            filterSelect.dispatchEvent(new Event('change'));

            expect(callback1).toHaveBeenCalledWith('3');
            expect(callback2).toHaveBeenCalledWith('3');
            expect(callback3).toHaveBeenCalledWith('3');
        });

        test('should handle changing to "all" filter', () => {
            const callback = jest.fn();
            filterManager.onChange(callback);
            filterManager.init();

            filterSelect.value = '2';
            filterSelect.dispatchEvent(new Event('change'));

            filterSelect.value = 'all';
            filterSelect.dispatchEvent(new Event('change'));

            expect(callback).toHaveBeenLastCalledWith('all');
        });

        test('should handle multiple consecutive changes', () => {
            const callback = jest.fn();
            filterManager.onChange(callback);
            filterManager.init();

            filterSelect.value = '1';
            filterSelect.dispatchEvent(new Event('change'));

            filterSelect.value = '2';
            filterSelect.dispatchEvent(new Event('change'));

            filterSelect.value = '3';
            filterSelect.dispatchEvent(new Event('change'));

            expect(callback).toHaveBeenCalledTimes(3);
            expect(callback).toHaveBeenNthCalledWith(1, '1');
            expect(callback).toHaveBeenNthCalledWith(2, '2');
            expect(callback).toHaveBeenNthCalledWith(3, '3');
        });

        test('should handle changing to same value', () => {
            const callback = jest.fn();
            filterManager.onChange(callback);
            filterManager.init();

            filterSelect.value = '1';
            filterSelect.dispatchEvent(new Event('change'));

            filterSelect.value = '1';
            filterSelect.dispatchEvent(new Event('change'));

            expect(callback).toHaveBeenCalledTimes(2);
            expect(callback).toHaveBeenCalledWith('1');
        });
    });

    describe('getValue', () => {
        test('should return current filter value', () => {
            filterSelect.value = '2';

            expect(filterManager.getValue()).toBe('2');
        });

        test('should return "all" by default', () => {
            expect(filterManager.getValue()).toBe('all');
        });

        test('should return "all" when filter select is null', () => {
            const manager = new FilterManager(null);

            expect(manager.getValue()).toBe('all');
        });

        test('should return "all" when filter select is undefined', () => {
            const manager = new FilterManager(undefined);

            expect(manager.getValue()).toBe('all');
        });

        test('should return current value after changes', () => {
            filterSelect.value = '1';
            expect(filterManager.getValue()).toBe('1');

            filterSelect.value = '3';
            expect(filterManager.getValue()).toBe('3');
        });
    });

    describe('setValue', () => {
        test('should set filter value', () => {
            filterManager.setValue('2');

            expect(filterSelect.value).toBe('2');
        });

        test('should handle setting to "all"', () => {
            filterSelect.value = '1';

            filterManager.setValue('all');

            expect(filterSelect.value).toBe('all');
        });

        test('should handle null filter select', () => {
            const manager = new FilterManager(null);

            expect(() => manager.setValue('1')).not.toThrow();
        });

        test('should handle undefined filter select', () => {
            const manager = new FilterManager(undefined);

            expect(() => manager.setValue('1')).not.toThrow();
        });

        test('should not trigger change event', () => {
            const callback = jest.fn();
            filterManager.onChange(callback);
            filterManager.init();

            filterManager.setValue('2');

            expect(callback).not.toHaveBeenCalled();
        });

        test('should handle invalid values', () => {
            filterManager.setValue('999');

            expect(filterSelect.value).toBe('');
        });
    });

    describe('reset', () => {
        test('should reset to "all" value', () => {
            filterSelect.value = '2';

            filterManager.reset();

            expect(filterSelect.value).toBe('all');
        });

        test('should handle resetting when already at "all"', () => {
            filterSelect.value = 'all';

            filterManager.reset();

            expect(filterSelect.value).toBe('all');
        });

        test('should not trigger change event', () => {
            const callback = jest.fn();
            filterManager.onChange(callback);
            filterManager.init();

            filterSelect.value = '2';
            filterManager.reset();

            expect(callback).not.toHaveBeenCalled();
        });

        test('should handle null filter select', () => {
            const manager = new FilterManager(null);

            expect(() => manager.reset()).not.toThrow();
        });
    });

    describe('Integration', () => {
        test('should handle complete filter flow', () => {
            const callback = jest.fn();
            filterManager.onChange(callback).init();

            expect(filterManager.getValue()).toBe('all');

            filterSelect.value = '1';
            filterSelect.dispatchEvent(new Event('change'));
            expect(callback).toHaveBeenCalledWith('1');
            expect(filterManager.getValue()).toBe('1');

            filterSelect.value = '2';
            filterSelect.dispatchEvent(new Event('change'));
            expect(callback).toHaveBeenCalledWith('2');

            filterManager.reset();
            expect(filterManager.getValue()).toBe('all');
        });

        test('should work with setValue and manual changes', () => {
            const callback = jest.fn();
            filterManager.onChange(callback).init();

            filterManager.setValue('1');
            expect(filterManager.getValue()).toBe('1');
            expect(callback).not.toHaveBeenCalled();

            filterSelect.value = '2';
            filterSelect.dispatchEvent(new Event('change'));
            expect(callback).toHaveBeenCalledWith('2');

            filterManager.reset();
            expect(filterManager.getValue()).toBe('all');
        });

        test('should maintain state across multiple operations', () => {
            filterManager.init();

            filterManager.setValue('1');
            expect(filterManager.getValue()).toBe('1');

            filterManager.setValue('2');
            expect(filterManager.getValue()).toBe('2');

            filterManager.reset();
            expect(filterManager.getValue()).toBe('all');

            filterManager.setValue('3');
            expect(filterManager.getValue()).toBe('3');
        });
    });

    describe('Edge Cases', () => {
        test('should handle select with no options', () => {
            const emptySelect = document.createElement('select');
            const manager = new FilterManager(emptySelect);

            manager.init();

            expect(manager.getValue()).toBe('');
        });

        test('should handle select with single option', () => {
            const singleSelect = document.createElement('select');
            const option = document.createElement('option');
            option.value = 'only';
            option.textContent = 'Only Option';
            singleSelect.appendChild(option);

            const manager = new FilterManager(singleSelect);

            expect(manager.getValue()).toBe('only');
        });

        test('should handle very long option values', () => {
            const longValue = 'a'.repeat(1000);
            const option = document.createElement('option');
            option.value = longValue;
            filterSelect.appendChild(option);

            filterManager.setValue(longValue);

            expect(filterManager.getValue()).toBe(longValue);
        });

        test('should handle special characters in values', () => {
            const specialValue = '<>&"\'';
            const option = document.createElement('option');
            option.value = specialValue;
            filterSelect.appendChild(option);

            filterManager.setValue(specialValue);

            expect(filterManager.getValue()).toBe(specialValue);
        });

        test('should handle rapid filter changes', () => {
            const callback = jest.fn();
            filterManager.onChange(callback);
            filterManager.init();

            for (let i = 1; i <= 3; i++) {
                filterSelect.value = String(i);
                filterSelect.dispatchEvent(new Event('change'));
            }

            expect(callback).toHaveBeenCalledTimes(3);
            expect(filterManager.getValue()).toBe('3');
        });

        test('should handle filter select being removed from DOM', () => {
            const container = document.createElement('div');
            container.appendChild(filterSelect);
            document.body.appendChild(container);

            filterManager.init();

            container.remove();

            expect(() => filterManager.getValue()).not.toThrow();
            expect(() => filterManager.setValue('1')).not.toThrow();
        });

        test('should handle disabled select', () => {
            filterSelect.disabled = true;

            const callback = jest.fn();
            filterManager.onChange(callback);
            filterManager.init();

            filterSelect.value = '1';
            filterSelect.dispatchEvent(new Event('change'));

            expect(callback).toHaveBeenCalledWith('1');
        });

        test('should handle readonly select', () => {
            filterSelect.readOnly = true;

            expect(() => filterManager.setValue('1')).not.toThrow();
            expect(filterManager.getValue()).toBe('1');
        });

        test('should handle multiple attribute on select', () => {
            filterSelect.multiple = true;

            filterManager.init();

            expect(() => filterManager.getValue()).not.toThrow();
        });
    });
});
