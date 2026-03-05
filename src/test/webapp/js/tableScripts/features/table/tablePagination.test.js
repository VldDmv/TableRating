import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';
import { PaginationManager } from '@/tableScripts/features/table/tablePagination.js';

describe('PaginationManager', () => {
    let paginationManager;
    let elements;

    beforeEach(() => {

        const container = document.createElement('div');
        container.className = 'pagination-controls-container';
        document.body.appendChild(container);

        elements = {
            prevButton: document.createElement('button'),
            nextButton: document.createElement('button'),
            rowsPerPageSelect: document.createElement('select'),
            pageDropdown: document.createElement('div'),
            pageList: document.createElement('ul')
        };

        [10, 25, 50].forEach(val => {
            const option = document.createElement('option');
            option.value = val;
            option.textContent = val;
            elements.rowsPerPageSelect.appendChild(option);
        });

        container.appendChild(elements.prevButton);
        container.appendChild(elements.nextButton);

        paginationManager = new PaginationManager(elements);
    });

    afterEach(() => {
        document.body.innerHTML = '';
    });

    describe('Constructor', () => {
        test('should initialize with provided elements', () => {
            expect(paginationManager.prevButton).toBe(elements.prevButton);
            expect(paginationManager.nextButton).toBe(elements.nextButton);
            expect(paginationManager.rowsPerPageSelect).toBe(elements.rowsPerPageSelect);
            expect(paginationManager.pageDropdown).toBe(elements.pageDropdown);
            expect(paginationManager.pageList).toBe(elements.pageList);
        });

        test('should initialize with default page values', () => {
            expect(paginationManager.getCurrentPage()).toBe(1);
            expect(paginationManager.getTotalPages()).toBe(1);
        });

        test('should initialize with empty listener arrays', () => {
            expect(paginationManager.pageChangeListeners).toEqual([]);
            expect(paginationManager.rowsChangeListeners).toEqual([]);
        });

        test('should find pagination container', () => {
            expect(paginationManager.paginationContainer).toBeTruthy();
            expect(paginationManager.paginationContainer.className).toContain('pagination-controls-container');
        });
    });

    describe('onPageChange', () => {
        test('should add page change listener', () => {
            const callback = jest.fn();

            paginationManager.onPageChange(callback);

            expect(paginationManager.pageChangeListeners).toContain(callback);
        });

        test('should return instance for chaining', () => {
            const callback = jest.fn();

            const result = paginationManager.onPageChange(callback);

            expect(result).toBe(paginationManager);
        });

        test('should allow multiple listeners', () => {
            const callback1 = jest.fn();
            const callback2 = jest.fn();

            paginationManager.onPageChange(callback1);
            paginationManager.onPageChange(callback2);

            expect(paginationManager.pageChangeListeners.length).toBe(2);
        });
    });

    describe('onRowsPerPageChange', () => {
        test('should add rows change listener', () => {
            const callback = jest.fn();

            paginationManager.onRowsPerPageChange(callback);

            expect(paginationManager.rowsChangeListeners).toContain(callback);
        });

        test('should return instance for chaining', () => {
            const callback = jest.fn();

            const result = paginationManager.onRowsPerPageChange(callback);

            expect(result).toBe(paginationManager);
        });
    });

    describe('init', () => {
        test('should initialize all components', () => {
            expect(() => paginationManager.init()).not.toThrow();
        });

        test('should setup prev button listener', () => {
            const callback = jest.fn();
            paginationManager.onPageChange(callback);
            paginationManager.init();

            paginationManager.currentPage = 2;
            paginationManager.totalPages = 5;

            elements.prevButton.click();

            expect(callback).toHaveBeenCalledWith(1);
        });

        test('should setup next button listener', () => {
            const callback = jest.fn();
            paginationManager.onPageChange(callback);
            paginationManager.init();

            paginationManager.currentPage = 1;
            paginationManager.totalPages = 5;

            elements.nextButton.click();

            expect(callback).toHaveBeenCalledWith(2);
        });

        test('should setup rows per page select listener', () => {
            const callback = jest.fn();
            paginationManager.onRowsPerPageChange(callback);
            paginationManager.init();

            elements.rowsPerPageSelect.value = '25';
            elements.rowsPerPageSelect.dispatchEvent(new Event('change'));

            expect(callback).toHaveBeenCalledWith(25);
        });

        test('should setup page dropdown listener', () => {
            paginationManager.init();

            elements.pageDropdown.click();

            expect(elements.pageList.style.display).toBe('block');
        });
    });

    describe('Previous Button', () => {
        test('should not trigger callback on first page', () => {
            const callback = jest.fn();
            paginationManager.onPageChange(callback);
            paginationManager.init();

            paginationManager.currentPage = 1;
            paginationManager.totalPages = 5;

            elements.prevButton.click();

            expect(callback).not.toHaveBeenCalled();
        });

        test('should trigger callback when not on first page', () => {
            const callback = jest.fn();
            paginationManager.onPageChange(callback);
            paginationManager.init();

            paginationManager.currentPage = 3;
            paginationManager.totalPages = 5;

            elements.prevButton.click();

            expect(callback).toHaveBeenCalledWith(2);
        });

        test('should handle missing prev button', () => {
            const manager = new PaginationManager({
                prevButton: null,
                nextButton: elements.nextButton,
                rowsPerPageSelect: elements.rowsPerPageSelect,
                pageDropdown: elements.pageDropdown,
                pageList: elements.pageList
            });

            expect(() => manager.init()).not.toThrow();
        });
    });

    describe('Next Button', () => {
        test('should not trigger callback on last page', () => {
            const callback = jest.fn();
            paginationManager.onPageChange(callback);
            paginationManager.init();

            paginationManager.currentPage = 5;
            paginationManager.totalPages = 5;

            elements.nextButton.click();

            expect(callback).not.toHaveBeenCalled();
        });

        test('should trigger callback when not on last page', () => {
            const callback = jest.fn();
            paginationManager.onPageChange(callback);
            paginationManager.init();

            paginationManager.currentPage = 2;
            paginationManager.totalPages = 5;

            elements.nextButton.click();

            expect(callback).toHaveBeenCalledWith(3);
        });

        test('should handle missing next button', () => {
            const manager = new PaginationManager({
                prevButton: elements.prevButton,
                nextButton: null,
                rowsPerPageSelect: elements.rowsPerPageSelect,
                pageDropdown: elements.pageDropdown,
                pageList: elements.pageList
            });

            expect(() => manager.init()).not.toThrow();
        });
    });

    describe('Rows Per Page Select', () => {
        test('should trigger callback with selected value', () => {
            const callback = jest.fn();
            paginationManager.onRowsPerPageChange(callback);
            paginationManager.init();

            elements.rowsPerPageSelect.value = '50';
            elements.rowsPerPageSelect.dispatchEvent(new Event('change'));

            expect(callback).toHaveBeenCalledWith(50);
        });

        test('should parse value as integer', () => {
            const callback = jest.fn();
            paginationManager.onRowsPerPageChange(callback);
            paginationManager.init();

            elements.rowsPerPageSelect.value = '25';
            elements.rowsPerPageSelect.dispatchEvent(new Event('change'));

            expect(callback).toHaveBeenCalledWith(25);
            expect(typeof callback.mock.calls[0][0]).toBe('number');
        });

        test('should handle missing select element', () => {
            const manager = new PaginationManager({
                prevButton: elements.prevButton,
                nextButton: elements.nextButton,
                rowsPerPageSelect: null,
                pageDropdown: elements.pageDropdown,
                pageList: elements.pageList
            });

            expect(() => manager.init()).not.toThrow();
        });
    });

    describe('Page Dropdown', () => {
        test('should toggle page list visibility', () => {
            paginationManager.init();

            elements.pageList.style.display = 'none';
            elements.pageDropdown.click();

            expect(elements.pageList.style.display).toBe('block');

            elements.pageDropdown.click();

            expect(elements.pageList.style.display).toBe('none');
        });

        test('should close dropdown when clicking page link', () => {
            paginationManager.init();

            const li = document.createElement('li');
            const a = document.createElement('a');
            a.href = '#';
            a.dataset.page = '3';
            a.textContent = '3';
            li.appendChild(a);
            elements.pageList.appendChild(li);

            elements.pageList.style.display = 'block';

            a.click();

            expect(elements.pageList.style.display).toBe('none');
        });

        test('should trigger page change when clicking page link', () => {
            const callback = jest.fn();
            paginationManager.onPageChange(callback);
            paginationManager.init();

            const li = document.createElement('li');
            const a = document.createElement('a');
            a.href = '#';
            a.dataset.page = '4';
            li.appendChild(a);
            elements.pageList.appendChild(li);

            a.click();

            expect(callback).toHaveBeenCalledWith(4);
        });

        test('should close dropdown when clicking outside', () => {
            paginationManager.init();

            elements.pageList.style.display = 'block';

            document.body.click();

            expect(elements.pageList.style.display).toBe('none');
        });

        test('should not close dropdown when clicking inside', () => {
            paginationManager.init();

            elements.pageList.style.display = 'block';

            elements.pageDropdown.click();

            expect(elements.pageList.style.display).toBe('none');
        });
    });

    describe('update', () => {
        test('should update current page and total pages', () => {
            const pageResult = {
                currentPage: 3,
                totalPages: 10
            };

            paginationManager.update(pageResult);

            expect(paginationManager.getCurrentPage()).toBe(3);
            expect(paginationManager.getTotalPages()).toBe(10);
        });

        test('should hide container when totalPages <= 1', () => {
            paginationManager.init();

            const pageResult = {
                currentPage: 1,
                totalPages: 1
            };

            paginationManager.update(pageResult);

            expect(paginationManager.paginationContainer.style.display).toBe('none');
        });

        test('should show container when totalPages > 1', () => {
            paginationManager.init();

            const pageResult = {
                currentPage: 1,
                totalPages: 5
            };

            paginationManager.update(pageResult);

            expect(paginationManager.paginationContainer.style.display).toBe('flex');
        });

        test('should disable prev button on first page', () => {
            const pageResult = {
                currentPage: 1,
                totalPages: 5
            };

            paginationManager.update(pageResult);

            expect(elements.prevButton.disabled).toBe(true);
        });

        test('should enable prev button when not on first page', () => {
            const pageResult = {
                currentPage: 2,
                totalPages: 5
            };

            paginationManager.update(pageResult);

            expect(elements.prevButton.disabled).toBe(false);
        });

        test('should disable next button on last page', () => {
            const pageResult = {
                currentPage: 5,
                totalPages: 5
            };

            paginationManager.update(pageResult);

            expect(elements.nextButton.disabled).toBe(true);
        });

        test('should enable next button when not on last page', () => {
            const pageResult = {
                currentPage: 2,
                totalPages: 5
            };

            paginationManager.update(pageResult);

            expect(elements.nextButton.disabled).toBe(false);
        });

        test('should update dropdown text', () => {
            const pageResult = {
                currentPage: 3,
                totalPages: 7
            };

            paginationManager.update(pageResult);

            expect(elements.pageDropdown.textContent).toBe('Page 3 of 7');
        });

        test('should update page list', () => {
            const pageResult = {
                currentPage: 2,
                totalPages: 5
            };

            paginationManager.update(pageResult);

            expect(elements.pageList.children.length).toBe(5);
        });

        test('should mark current page as active', () => {
            const pageResult = {
                currentPage: 3,
                totalPages: 5
            };

            paginationManager.update(pageResult);

            const links = elements.pageList.querySelectorAll('a');
            expect(links[2].classList.contains('active-page')).toBe(true);
        });

     test('should handle single page', () => {
                 paginationManager.init();

                 const pageResult = {
                     currentPage: 1,
                     totalPages: 1
                 };

                 paginationManager.update(pageResult);

                 expect(elements.prevButton.disabled).toBe(true);
                 expect(elements.nextButton.disabled).toBe(true);
                 expect(paginationManager.paginationContainer.style.display).toBe('none');
             });
         });

    describe('updatePageList', () => {
        test('should create correct number of page links', () => {
            paginationManager.currentPage = 1;
            paginationManager.totalPages = 10;

            paginationManager.updatePageList();

            expect(elements.pageList.children.length).toBe(10);
        });

        test('should set correct page numbers', () => {
            paginationManager.currentPage = 1;
            paginationManager.totalPages = 3;

            paginationManager.updatePageList();

            const links = elements.pageList.querySelectorAll('a');
            expect(links[0].textContent).toBe('1');
            expect(links[1].textContent).toBe('2');
            expect(links[2].textContent).toBe('3');
        });

        test('should set data-page attributes', () => {
            paginationManager.currentPage = 1;
            paginationManager.totalPages = 3;

            paginationManager.updatePageList();

            const links = elements.pageList.querySelectorAll('a');
            expect(links[0].dataset.page).toBe('1');
            expect(links[1].dataset.page).toBe('2');
            expect(links[2].dataset.page).toBe('3');
        });

        test('should handle missing pageList element', () => {
            const manager = new PaginationManager({
                prevButton: elements.prevButton,
                nextButton: elements.nextButton,
                rowsPerPageSelect: elements.rowsPerPageSelect,
                pageDropdown: elements.pageDropdown,
                pageList: null
            });

            expect(() => manager.updatePageList()).not.toThrow();
        });
    });

    describe('Integration', () => {
        test('should handle complete pagination flow', () => {
            const pageChangeCallback = jest.fn();
            const rowsChangeCallback = jest.fn();

            paginationManager
                .onPageChange(pageChangeCallback)
                .onRowsPerPageChange(rowsChangeCallback)
                .init();

            paginationManager.update({
                currentPage: 1,
                totalPages: 5
            });

            elements.nextButton.click();
            expect(pageChangeCallback).toHaveBeenCalledWith(2);

            elements.rowsPerPageSelect.value = '25';
            elements.rowsPerPageSelect.dispatchEvent(new Event('change'));
            expect(rowsChangeCallback).toHaveBeenCalledWith(25);
        });

        test('should handle navigation through pages', () => {
            const callback = jest.fn();
            paginationManager.onPageChange(callback).init();

            paginationManager.update({ currentPage: 3, totalPages: 5 });

            elements.nextButton.click();
            expect(callback).toHaveBeenCalledWith(4);

            paginationManager.update({ currentPage: 4, totalPages: 5 });

            elements.prevButton.click();
            expect(callback).toHaveBeenCalledWith(3);
        });
    });
});