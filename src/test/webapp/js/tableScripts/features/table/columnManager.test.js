import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';
import { ColumnManager } from '@/tableScripts/features/table/columnManager.js';

describe('ColumnManager', () => {
    let columnManager;
    let mockConfig;
    let table;
    let container;

    beforeEach(() => {
        table = document.createElement('table');
        table.dataset.entityType = 'games';
        document.body.appendChild(table);

        container = document.createElement('div');
        container.id = 'column-toggle-container';
        document.body.appendChild(container);

        mockConfig = {
            entityType: 'games',
            columns: {
                name: { name: 'Name', index: 0, hideable: false },
                score: { name: 'Score', index: 1, hideable: false },
                tags: { name: 'Tags', index: 2, hideable: true },
                completed: { name: 'Completed', index: 3, hideable: true },
                actions: { name: 'Delete', index: 4, hideable: true },
            },
        };

        columnManager = new ColumnManager(mockConfig);
    });

    afterEach(() => {
        document.body.innerHTML = '';
        delete window.__columnVisibilityState;
    });

    describe('Constructor', () => {
        test('should initialize with config', () => {
            expect(columnManager.entityType).toBe('games');
            expect(columnManager.columnsConfig).toBe(mockConfig.columns);
        });

        test('should find table element', () => {
            expect(columnManager.tableElement).toBe(table);
        });

        test('should find container element', () => {
            expect(columnManager.containerElement).toBe(container);
        });

        test('should initialize state storage', () => {
            expect(window.__columnVisibilityState).toBeDefined();
            expect(columnManager.stateStorage).toBeDefined();
        });

        test('should reuse existing state storage', () => {
            window.__columnVisibilityState = { existing: 'data' };

            const manager = new ColumnManager(mockConfig);

            expect(manager.stateStorage).toEqual({ existing: 'data' });
        });
    });

    describe('State Storage', () => {
        test('should initialize global state storage', () => {
            expect(window.__columnVisibilityState).toBeDefined();
        });

        test('should use same storage instance across managers', () => {
            const manager1 = new ColumnManager(mockConfig);
            const manager2 = new ColumnManager(mockConfig);

            expect(manager1.stateStorage).toBe(manager2.stateStorage);
        });

        test('should persist state in memory', () => {
            columnManager.saveHiddenColumns(['tags']);

            const manager2 = new ColumnManager(mockConfig);

            expect(manager2.getHiddenColumns()).toEqual(['tags']);
        });
    });

    describe('getHiddenColumns', () => {
        test('should return empty array by default', () => {
            expect(columnManager.getHiddenColumns()).toEqual([]);
        });

        test('should return saved hidden columns', () => {
            columnManager.saveHiddenColumns(['tags', 'completed']);

            expect(columnManager.getHiddenColumns()).toEqual(['tags', 'completed']);
        });

        test('should return empty array for different entity', () => {
            columnManager.saveHiddenColumns(['tags']);

            const moviesConfig = { ...mockConfig, entityType: 'movies' };
            const moviesManager = new ColumnManager(moviesConfig);

            expect(moviesManager.getHiddenColumns()).toEqual([]);
        });
    });

    describe('saveHiddenColumns', () => {
        test('should save hidden columns', () => {
            columnManager.saveHiddenColumns(['tags']);

            expect(columnManager.getHiddenColumns()).toEqual(['tags']);
        });

        test('should overwrite previous values', () => {
            columnManager.saveHiddenColumns(['tags']);
            columnManager.saveHiddenColumns(['completed']);

            expect(columnManager.getHiddenColumns()).toEqual(['completed']);
        });

        test('should save empty array', () => {
            columnManager.saveHiddenColumns(['tags']);
            columnManager.saveHiddenColumns([]);

            expect(columnManager.getHiddenColumns()).toEqual([]);
        });

        test('should handle multiple columns', () => {
            columnManager.saveHiddenColumns(['tags', 'completed', 'actions']);

            expect(columnManager.getHiddenColumns()).toEqual(['tags', 'completed', 'actions']);
        });
    });

    describe('applyVisibility', () => {
        test('should set data-hidden-columns attribute', () => {
            columnManager.saveHiddenColumns(['tags', 'completed']);

            columnManager.applyVisibility();

            expect(table.dataset.hiddenColumns).toBe('tags completed');
        });

        test('should handle empty hidden columns', () => {
            columnManager.saveHiddenColumns([]);

            columnManager.applyVisibility();

            expect(table.dataset.hiddenColumns).toBe('');
        });

        test('should handle single hidden column', () => {
            columnManager.saveHiddenColumns(['tags']);

            columnManager.applyVisibility();

            expect(table.dataset.hiddenColumns).toBe('tags');
        });

        test('should not throw if table element is missing', () => {
            table.remove();
            columnManager.tableElement = null;

            expect(() => columnManager.applyVisibility()).not.toThrow();
        });

        test('should update attribute on multiple calls', () => {
            columnManager.saveHiddenColumns(['tags']);
            columnManager.applyVisibility();
            expect(table.dataset.hiddenColumns).toBe('tags');

            columnManager.saveHiddenColumns(['completed']);
            columnManager.applyVisibility();
            expect(table.dataset.hiddenColumns).toBe('completed');
        });
    });

    describe('createToggleUI', () => {
        test('should create checkboxes for hideable columns', () => {
            columnManager.createToggleUI();

            const checkboxes = container.querySelectorAll('input[type="checkbox"]');

            expect(checkboxes.length).toBe(3);
        });

        test('should not create checkboxes for non-hideable columns', () => {
            columnManager.createToggleUI();

            const labels = container.querySelectorAll('label');
            const labelTexts = Array.from(labels).map((l) => l.textContent.trim());

            expect(labelTexts).not.toContain('Name');
            expect(labelTexts).not.toContain('Score');
        });

        test('should check visible columns by default', () => {
            columnManager.createToggleUI();

            const checkboxes = container.querySelectorAll('input[type="checkbox"]');

            checkboxes.forEach((cb) => {
                expect(cb.checked).toBe(true);
            });
        });

        test('should uncheck hidden columns', () => {
            columnManager.saveHiddenColumns(['tags']);
            columnManager.createToggleUI();

            const tagsCheckbox = container.querySelector('[data-column-key="tags"]');

            expect(tagsCheckbox.checked).toBe(false);
        });

        test('should set correct data-column-key', () => {
            columnManager.createToggleUI();

            const tagsCheckbox = container.querySelector('[data-column-key="tags"]');
            const completedCheckbox = container.querySelector('[data-column-key="completed"]');

            expect(tagsCheckbox).toBeTruthy();
            expect(completedCheckbox).toBeTruthy();
        });

        test('should include title text', () => {
            columnManager.createToggleUI();

            expect(container.textContent).toContain('Show Columns:');
        });

        test('should not throw if container is missing', () => {
            container.remove();
            columnManager.containerElement = null;

            expect(() => columnManager.createToggleUI()).not.toThrow();
        });

        test('should replace existing content', () => {
            container.innerHTML = '<p>Old content</p>';

            columnManager.createToggleUI();

            expect(container.textContent).not.toContain('Old content');
            expect(container.textContent).toContain('Show Columns:');
        });
    });

    describe('handleToggleChange', () => {
        beforeEach(() => {
            columnManager.createToggleUI();
        });

        test('should update hidden columns when checkbox is unchecked', () => {
            const tagsCheckbox = container.querySelector('[data-column-key="tags"]');

            tagsCheckbox.checked = false;
            tagsCheckbox.dispatchEvent(new Event('change'));

            expect(columnManager.getHiddenColumns()).toContain('tags');
        });

        test('should update hidden columns when checkbox is checked', () => {
            columnManager.saveHiddenColumns(['tags']);
            columnManager.createToggleUI();

            const tagsCheckbox = container.querySelector('[data-column-key="tags"]');

            tagsCheckbox.checked = true;
            tagsCheckbox.dispatchEvent(new Event('change'));

            expect(columnManager.getHiddenColumns()).not.toContain('tags');
        });

        test('should handle multiple checkboxes', () => {
            const tagsCheckbox = container.querySelector('[data-column-key="tags"]');
            const completedCheckbox = container.querySelector('[data-column-key="completed"]');

            tagsCheckbox.checked = false;
            tagsCheckbox.dispatchEvent(new Event('change'));

            completedCheckbox.checked = false;
            completedCheckbox.dispatchEvent(new Event('change'));

            expect(columnManager.getHiddenColumns()).toContain('tags');
            expect(columnManager.getHiddenColumns()).toContain('completed');
        });

        test('should apply visibility after change', () => {
            const tagsCheckbox = container.querySelector('[data-column-key="tags"]');

            tagsCheckbox.checked = false;
            tagsCheckbox.dispatchEvent(new Event('change'));

            expect(table.dataset.hiddenColumns).toContain('tags');
        });

        test('should not throw if container is missing', () => {
            container.remove();
            columnManager.containerElement = null;

            expect(() => columnManager.handleToggleChange()).not.toThrow();
        });
    });

    describe('init', () => {
        test('should create UI and apply visibility', () => {
            columnManager.init();

            expect(container.querySelectorAll('input[type="checkbox"]').length).toBeGreaterThan(0);
            expect(table.dataset.hiddenColumns).toBeDefined();
        });

        test('should warn if required elements are missing', () => {
            const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();

            table.remove();
            columnManager.tableElement = null;

            columnManager.init();

            expect(consoleWarnSpy).toHaveBeenCalled();

            consoleWarnSpy.mockRestore();
        });

        test('should not throw if container is missing', () => {
            const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();

            container.remove();
            columnManager.containerElement = null;

            expect(() => columnManager.init()).not.toThrow();

            consoleWarnSpy.mockRestore();
        });

        test('should not throw if columns config is missing', () => {
            const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();

            const badConfig = { entityType: 'games', columns: null };
            const manager = new ColumnManager(badConfig);

            expect(() => manager.init()).not.toThrow();

            consoleWarnSpy.mockRestore();
        });
    });

    describe('reset', () => {
        test('should clear hidden columns', () => {
            columnManager.saveHiddenColumns(['tags', 'completed']);

            columnManager.reset();

            expect(columnManager.getHiddenColumns()).toEqual([]);
        });

        test('should apply visibility', () => {
            columnManager.saveHiddenColumns(['tags']);
            columnManager.applyVisibility();

            columnManager.reset();

            expect(table.dataset.hiddenColumns).toBe('');
        });

        test('should recreate UI', () => {
            columnManager.init();
            columnManager.saveHiddenColumns(['tags']);

            columnManager.reset();

            const checkboxes = container.querySelectorAll('input[type="checkbox"]');
            checkboxes.forEach((cb) => {
                expect(cb.checked).toBe(true);
            });
        });
    });

    describe('toggleColumn', () => {
        test('should hide visible column', () => {
            columnManager.toggleColumn('tags');

            expect(columnManager.getHiddenColumns()).toContain('tags');
        });

        test('should show hidden column', () => {
            columnManager.saveHiddenColumns(['tags']);

            columnManager.toggleColumn('tags');

            expect(columnManager.getHiddenColumns()).not.toContain('tags');
        });

        test('should apply visibility after toggle', () => {
            columnManager.toggleColumn('tags');

            expect(table.dataset.hiddenColumns).toContain('tags');
        });

        test('should handle toggling multiple times', () => {
            columnManager.toggleColumn('tags');
            expect(columnManager.getHiddenColumns()).toContain('tags');

            columnManager.toggleColumn('tags');
            expect(columnManager.getHiddenColumns()).not.toContain('tags');

            columnManager.toggleColumn('tags');
            expect(columnManager.getHiddenColumns()).toContain('tags');
        });

        test('should handle toggling different columns', () => {
            columnManager.toggleColumn('tags');
            columnManager.toggleColumn('completed');

            expect(columnManager.getHiddenColumns()).toContain('tags');
            expect(columnManager.getHiddenColumns()).toContain('completed');
        });
    });

    describe('Integration', () => {
        test('should handle complete workflow', () => {
            columnManager.init();
            expect(container.querySelectorAll('input[type="checkbox"]').length).toBe(3);

            const tagsCheckbox = container.querySelector('[data-column-key="tags"]');
            tagsCheckbox.checked = false;
            tagsCheckbox.dispatchEvent(new Event('change'));

            expect(columnManager.getHiddenColumns()).toContain('tags');
            expect(table.dataset.hiddenColumns).toContain('tags');

            columnManager.reset();

            expect(columnManager.getHiddenColumns()).toEqual([]);
            expect(table.dataset.hiddenColumns).toBe('');
        });

        test('should persist state across page interactions', () => {
            columnManager.init();

            columnManager.toggleColumn('tags');
            columnManager.toggleColumn('completed');

            const newManager = new ColumnManager(mockConfig);

            expect(newManager.getHiddenColumns()).toEqual(['tags', 'completed']);
        });

        test('should isolate state by entity type', () => {
            columnManager.init();
            columnManager.toggleColumn('tags');

            const moviesConfig = {
                entityType: 'movies',
                columns: mockConfig.columns,
            };

            const moviesTable = document.createElement('table');
            moviesTable.dataset.entityType = 'movies';
            document.body.appendChild(moviesTable);

            const moviesManager = new ColumnManager(moviesConfig);
            moviesManager.tableElement = moviesTable;

            expect(moviesManager.getHiddenColumns()).toEqual([]);
        });
    });

    describe('Edge Cases', () => {
        test('should handle column key that does not exist', () => {
            expect(() => columnManager.toggleColumn('nonexistent')).not.toThrow();
        });

        test('should handle empty columns config', () => {
            const emptyConfig = { entityType: 'test', columns: {} };
            const manager = new ColumnManager(emptyConfig);

            expect(() => manager.init()).not.toThrow();
        });

        test('should handle all columns being hideable', () => {
            const allHideableConfig = {
                entityType: 'test',
                columns: {
                    col1: { name: 'Col1', index: 0, hideable: true },
                    col2: { name: 'Col2', index: 1, hideable: true },
                },
            };

            const manager = new ColumnManager(allHideableConfig);
            manager.createToggleUI();

            expect(container.querySelectorAll('input[type="checkbox"]').length).toBe(2);
        });

        test('should handle no hideable columns', () => {
            const noHideableConfig = {
                entityType: 'test',
                columns: {
                    col1: { name: 'Col1', index: 0, hideable: false },
                    col2: { name: 'Col2', index: 1, hideable: false },
                },
            };

            const manager = new ColumnManager(noHideableConfig);
            manager.createToggleUI();

            expect(container.querySelectorAll('input[type="checkbox"]').length).toBe(0);
        });

        test('should handle very long entity type names', () => {
            const longEntityType = 'a'.repeat(1000);
            const config = { ...mockConfig, entityType: longEntityType };

            const manager = new ColumnManager(config);

            expect(manager.entityType).toBe(longEntityType);
        });
    });
});
