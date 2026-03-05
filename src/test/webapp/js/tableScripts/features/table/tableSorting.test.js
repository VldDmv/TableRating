import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';
import { SortManager } from '@/tableScripts/features/table/tableSorting.js';

describe('SortManager', () => {
    let sortManager;
    let headers;
    let columnsConfig;

    beforeEach(() => {

        headers = [];
        for (let i = 0; i < 5; i++) {
            const th = document.createElement('th');

            Object.defineProperty(th, 'cellIndex', {
                value: i,
                writable: true,
                configurable: true
            });
            th.textContent = `Column ${i}`;
            headers.push(th);
        }

        columnsConfig = {
            name: { name: 'Name', index: 0, sortable: true },
            score: { name: 'Score', index: 1, sortable: true },
            tags: { name: 'Tags', index: 2, sortable: false },
            completed: { name: 'Completed', index: 3, sortable: true },
            actions: { name: 'Actions', index: 4, sortable: false }
        };

        sortManager = new SortManager(headers, columnsConfig);
    });

    describe('Constructor', () => {
        test('should initialize with headers and config', () => {
            expect(sortManager.headers).toBe(headers);
            expect(sortManager.columnsConfig).toBe(columnsConfig);
        });

        test('should initialize with empty listeners array', () => {
            expect(sortManager.listeners).toEqual([]);
        });
    });

    describe('onSort', () => {
        test('should add sort listener', () => {
            const callback = jest.fn();

            sortManager.onSort(callback);

            expect(sortManager.listeners).toContain(callback);
        });

        test('should return instance for chaining', () => {
            const callback = jest.fn();

            const result = sortManager.onSort(callback);

            expect(result).toBe(sortManager);
        });

        test('should allow multiple listeners', () => {
            const callback1 = jest.fn();
            const callback2 = jest.fn();

            sortManager.onSort(callback1);
            sortManager.onSort(callback2);

            expect(sortManager.listeners.length).toBe(2);
        });
    });

    describe('init', () => {
        test('should set cursor pointer on sortable headers', () => {
            sortManager.init();

            expect(headers[0].style.cursor).toBe('pointer');
            expect(headers[1].style.cursor).toBe('pointer');
            expect(headers[2].style.cursor).not.toBe('pointer');
            expect(headers[3].style.cursor).toBe('pointer');
            expect(headers[4].style.cursor).not.toBe('pointer');
        });

        test('should set data-column-key on sortable headers', () => {
            sortManager.init();

            expect(headers[0].dataset.columnKey).toBe('name');
            expect(headers[1].dataset.columnKey).toBe('score');
            expect(headers[2].dataset.columnKey).toBeUndefined();
            expect(headers[3].dataset.columnKey).toBe('completed');
            expect(headers[4].dataset.columnKey).toBeUndefined();
        });

        test('should attach click listeners to sortable headers', () => {
            const callback = jest.fn();
            sortManager.onSort(callback);
            sortManager.init();

            headers[0].click();

            expect(callback).toHaveBeenCalled();
        });

        test('should not attach click listeners to non-sortable headers', () => {
            const callback = jest.fn();
            sortManager.onSort(callback);
            sortManager.init();

            headers[2].click();

            expect(callback).not.toHaveBeenCalled();
        });

        test('should handle headers without matching column config', () => {
            const extraHeader = document.createElement('th');
            Object.defineProperty(extraHeader, 'cellIndex', {
                value: 99,
                writable: true,
                configurable: true
            });
            headers.push(extraHeader);

            expect(() => sortManager.init()).not.toThrow();
        });
    });

    describe('handleHeaderClick', () => {
        test('should trigger callback with column key and order', () => {
            const callback = jest.fn();
            sortManager.onSort(callback);
            sortManager.init();

            headers[0].click();

            expect(callback).toHaveBeenCalledWith('name', 'asc');
        });

        test('should toggle sort order on subsequent clicks', () => {
            const callback = jest.fn();
            sortManager.onSort(callback);
            sortManager.init();

            headers[0].dataset.sortOrder = 'asc';
            headers[0].click();

            expect(callback).toHaveBeenCalledWith('name', 'desc');
        });

        test('should default to asc on first click', () => {
            const callback = jest.fn();
            sortManager.onSort(callback);
            sortManager.init();

            headers[0].click();

            expect(callback).toHaveBeenCalledWith('name', 'asc');
        });

        test('should alternate between asc and desc', () => {
            const callback = jest.fn();
            sortManager.onSort(callback);
            sortManager.init();

            headers[0].click();
            expect(callback).toHaveBeenLastCalledWith('name', 'asc');

            headers[0].dataset.sortOrder = 'asc';
            headers[0].click();
            expect(callback).toHaveBeenLastCalledWith('name', 'desc');

            headers[0].dataset.sortOrder = 'desc';
            headers[0].click();
            expect(callback).toHaveBeenLastCalledWith('name', 'asc');
        });

        test('should not trigger callback for non-sortable columns', () => {
            const callback = jest.fn();
            sortManager.onSort(callback);
            sortManager.init();

            headers[2].click();

            expect(callback).not.toHaveBeenCalled();
        });

        test('should trigger all registered listeners', () => {
            const callback1 = jest.fn();
            const callback2 = jest.fn();

            sortManager.onSort(callback1);
            sortManager.onSort(callback2);
            sortManager.init();

            headers[0].click();

            expect(callback1).toHaveBeenCalled();
            expect(callback2).toHaveBeenCalled();
        });

        test('should handle header without columnKey', () => {
            sortManager.init();

            const header = document.createElement('th');
            Object.defineProperty(header, 'cellIndex', {
                value: 10,
                writable: true,
                configurable: true
            });

            expect(() => sortManager.handleHeaderClick(header)).not.toThrow();
        });

        test('should not trigger callback if sortable is explicitly false', () => {
            const callback = jest.fn();
            sortManager.onSort(callback);
            sortManager.init();

            headers[2].dataset.columnKey = 'tags';
            sortManager.handleHeaderClick(headers[2]);

            expect(callback).not.toHaveBeenCalled();
        });
    });

    describe('updateHeaders', () => {
        test('should add sort indicator to active column', () => {
            sortManager.init();

            sortManager.updateHeaders('name', 'asc');

            expect(headers[0].textContent).toContain('▲');
        });

        test('should show up arrow for ascending sort', () => {
            sortManager.init();

            sortManager.updateHeaders('score', 'asc');

            expect(headers[1].textContent).toContain('▲');
        });

        test('should show down arrow for descending sort', () => {
            sortManager.init();

            sortManager.updateHeaders('score', 'desc');

            expect(headers[1].textContent).toContain('▼');
        });

        test('should remove arrows from inactive columns', () => {
            sortManager.init();

            headers[0].textContent = 'Name ▲';
            headers[1].textContent = 'Score ▼';

            sortManager.updateHeaders('completed', 'asc');

            expect(headers[0].textContent).not.toContain('▲');
            expect(headers[1].textContent).not.toContain('▼');
            expect(headers[3].textContent).toContain('▲');
        });

        test('should set data-sort-order attribute', () => {
            sortManager.init();

            sortManager.updateHeaders('name', 'desc');

            expect(headers[0].dataset.sortOrder).toBe('desc');
        });

        test('should clear data-sort-order from other columns', () => {
            sortManager.init();

            headers[0].dataset.sortOrder = 'asc';
            headers[1].dataset.sortOrder = 'desc';

            sortManager.updateHeaders('completed', 'asc');

            expect(headers[0].dataset.sortOrder).toBe('');
            expect(headers[1].dataset.sortOrder).toBe('');
            expect(headers[3].dataset.sortOrder).toBe('asc');
        });

        test('should handle column that does not exist', () => {
            sortManager.init();

            expect(() => {
                sortManager.updateHeaders('nonexistent', 'asc');
            }).not.toThrow();
        });

        test('should preserve original header text', () => {
            sortManager.init();

            headers[0].textContent = 'Name';
            sortManager.updateHeaders('name', 'asc');

            expect(headers[0].textContent).toBe('Name ▲');

            sortManager.updateHeaders('score', 'desc');

            expect(headers[0].textContent).toBe('Name');
        });

        test('should handle headers with existing arrows', () => {
            sortManager.init();

            headers[0].textContent = 'Name ▲';

            sortManager.updateHeaders('name', 'desc');

            expect(headers[0].textContent).toBe('Name ▼');
            expect(headers[0].textContent).not.toContain('▲');
        });
    });

    describe('Integration', () => {
        test('should handle complete sort flow', () => {
            const callback = jest.fn();
            sortManager.onSort(callback).init();

            headers[0].click();
            expect(callback).toHaveBeenCalledWith('name', 'asc');

            sortManager.updateHeaders('name', 'asc');
            expect(headers[0].textContent).toContain('▲');

            headers[0].dataset.sortOrder = 'asc';
            headers[0].click();
            expect(callback).toHaveBeenCalledWith('name', 'desc');

            sortManager.updateHeaders('name', 'desc');
            expect(headers[0].textContent).toContain('▼');
        });

        test('should handle switching between columns', () => {
            const callback = jest.fn();
            sortManager.onSort(callback).init();

            headers[0].click();
            sortManager.updateHeaders('name', 'asc');
            expect(headers[0].textContent).toContain('▲');

            headers[1].click();
            sortManager.updateHeaders('score', 'asc');
            expect(headers[0].textContent).not.toContain('▲');
            expect(headers[1].textContent).toContain('▲');
        });

        test('should maintain sort state across multiple updates', () => {
            sortManager.init();

            sortManager.updateHeaders('name', 'asc');
            expect(headers[0].dataset.sortOrder).toBe('asc');

            sortManager.updateHeaders('name', 'desc');
            expect(headers[0].dataset.sortOrder).toBe('desc');

            sortManager.updateHeaders('score', 'asc');
            expect(headers[0].dataset.sortOrder).toBe('');
            expect(headers[1].dataset.sortOrder).toBe('asc');
        });
    });

    describe('Edge Cases', () => {
        test('should handle empty headers array', () => {
            const manager = new SortManager([], columnsConfig);

            expect(() => manager.init()).not.toThrow();
        });

        test('should handle empty columns config', () => {
            const manager = new SortManager(headers, {});

            expect(() => manager.init()).not.toThrow();
        });

        test('should handle null headers', () => {
            const manager = new SortManager(null, columnsConfig);

            expect(() => manager.init()).toThrow();
        });

        test('should handle headers with duplicate cellIndex', () => {
            Object.defineProperty(headers[0], 'cellIndex', { value: 0 });
            Object.defineProperty(headers[1], 'cellIndex', { value: 0 });

            expect(() => sortManager.init()).not.toThrow();
        });

        test('should handle rapid successive clicks', () => {
            const callback = jest.fn();
            sortManager.onSort(callback).init();

            headers[0].click();
            headers[0].dataset.sortOrder = 'asc';
            headers[0].click();
            headers[0].dataset.sortOrder = 'desc';
            headers[0].click();

            expect(callback).toHaveBeenCalledTimes(3);
        });

        test('should handle columns with undefined sortable property', () => {
            columnsConfig.name.sortable = undefined;
            sortManager.init();

            expect(headers[0].style.cursor).toBe('pointer');
        });

     test('should handle very long column names', () => {

                 sortManager.init();


                 headers[0].textContent = 'A'.repeat(1000);
                 sortManager.updateHeaders('name', 'asc');
                 expect(headers[0].textContent).toContain('▲');
             });
         });
});
