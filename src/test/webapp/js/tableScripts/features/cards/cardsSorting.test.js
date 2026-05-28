import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';
import { SortControls } from '@/tableScripts/features/cards/cardsSorting.js';

describe('SortControls', () => {
    let controls;
    let mockConfig;
    let mockStateManager;

    beforeEach(() => {
        jest.spyOn(console, 'warn').mockImplementation();
        jest.spyOn(console, 'log').mockImplementation();

        document.body.innerHTML = `<div class="table-controls"></div>`;

        mockConfig = {
            columns: {
                cover: { name: 'Cover', sortable: false },
                name: { name: 'Name', sortable: true },
                score: { name: 'Score', sortable: true },
                tags: { name: 'Tags', sortable: false },
                completed: { name: 'Completed', sortable: true },
                actions: { name: 'Actions', sortable: false },
            },
        };

        mockStateManager = {
            getState: jest.fn(() => ({ sortBy: 'name', sortOrder: 'asc' })),
            setState: jest.fn(),
        };

        controls = new SortControls(mockConfig, mockStateManager);
        controls.init();
    });

    afterEach(() => {
        document.body.innerHTML = '';
        jest.restoreAllMocks();
    });

    describe('Constructor', () => {
        test('should store config and stateManager', () => {
            expect(controls.config).toBe(mockConfig);
            expect(controls.stateManager).toBe(mockStateManager);
        });
    });

    describe('getSortableColumns', () => {
        test('should return only sortable columns', () => {
            const cols = controls.getSortableColumns();
            expect(
                cols.every((c) => c.key !== 'cover' && c.key !== 'tags' && c.key !== 'actions')
            ).toBe(true);
        });

        test('should exclude cover and actions regardless of sortable flag', () => {
            const cols = controls.getSortableColumns();
            expect(cols.find((c) => c.key === 'cover')).toBeUndefined();
            expect(cols.find((c) => c.key === 'actions')).toBeUndefined();
        });

        test('should return correct key/name pairs', () => {
            const cols = controls.getSortableColumns();
            const nameCol = cols.find((c) => c.key === 'name');
            expect(nameCol).toBeDefined();
            expect(nameCol.name).toBe('Name');
        });
    });

    describe('init', () => {
        test('should create sort controls container in .table-controls', () => {
            expect(document.getElementById('sort-controls')).toBeTruthy();
        });

        test('should render a select with sortable columns', () => {
            const select = document.getElementById('sortBySelect');
            expect(select).toBeTruthy();
            expect(select.options.length).toBeGreaterThan(0);
        });

        test('should render a sort order toggle button', () => {
            expect(document.getElementById('sortOrderBtn')).toBeTruthy();
        });

        test('should not throw if .table-controls is missing', () => {
            document.body.innerHTML = '';
            const c = new SortControls(mockConfig, mockStateManager);
            expect(() => c.init()).not.toThrow();
        });
    });

    describe('updateActiveSort', () => {
        test('should set select value from state', () => {
            mockStateManager.getState.mockReturnValue({ sortBy: 'score', sortOrder: 'desc' });
            controls.updateActiveSort();
            expect(document.getElementById('sortBySelect').value).toBe('score');
        });

        test('should show ↑ for asc order', () => {
            mockStateManager.getState.mockReturnValue({ sortBy: 'name', sortOrder: 'asc' });
            controls.updateActiveSort();
            expect(document.querySelector('.sort-icon').textContent).toBe('↑');
        });

        test('should show ↓ for desc order', () => {
            mockStateManager.getState.mockReturnValue({ sortBy: 'name', sortOrder: 'desc' });
            controls.updateActiveSort();
            expect(document.querySelector('.sort-icon').textContent).toBe('↓');
        });
    });

    describe('Event listeners', () => {
        test('should call setState when sort column changes', () => {
            const select = document.getElementById('sortBySelect');
            select.value = 'score';
            select.dispatchEvent(new Event('change'));
            expect(mockStateManager.setState).toHaveBeenCalledWith(
                expect.objectContaining({ sortBy: 'score', currentPage: 1 })
            );
        });

        test('should toggle sort order on button click', () => {
            const btn = document.getElementById('sortOrderBtn');
            btn.click();
            expect(mockStateManager.setState).toHaveBeenCalledWith(
                expect.objectContaining({ sortOrder: 'desc' })
            );
        });

        test('second button click should toggle back to asc', () => {
            mockStateManager.getState.mockReturnValue({ sortBy: 'name', sortOrder: 'desc' });
            const btn = document.getElementById('sortOrderBtn');
            btn.click();
            expect(mockStateManager.setState).toHaveBeenCalledWith(
                expect.objectContaining({ sortOrder: 'asc' })
            );
        });
    });

    describe('show / hide', () => {
        test('show should set display to flex', () => {
            controls.container.style.display = 'none';
            controls.show();
            expect(controls.container.style.display).toBe('flex');
        });

        test('hide should set display to none', () => {
            controls.hide();
            expect(controls.container.style.display).toBe('none');
        });
    });

    describe('getCurrentSort', () => {
        test('should return sortBy and sortOrder from state', () => {
            mockStateManager.getState.mockReturnValue({ sortBy: 'score', sortOrder: 'desc' });
            expect(controls.getCurrentSort()).toEqual({ sortBy: 'score', sortOrder: 'desc' });
        });
    });
});
