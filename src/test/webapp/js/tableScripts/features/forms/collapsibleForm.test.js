import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';
import { CollapsibleForm } from '@/tableScripts/features/forms/collapsibleForm.js';

describe('CollapsibleForm', () => {
    let form;
    let collapsible;

    beforeEach(() => {
        jest.useFakeTimers();
        document.body.innerHTML = `
            <div id="wrapper">
                <form id="test-form">
                    <input type="text" name="name" />
                    <button type="submit">Submit</button>
                </form>
            </div>
        `;
        form = document.getElementById('test-form');
        const consoleStub = jest.spyOn(console, 'error').mockImplementation();
        collapsible = new CollapsibleForm(form, { startCollapsed: true });
        consoleStub.mockRestore();
    });

    afterEach(() => {
        document.body.innerHTML = '';
        jest.useRealTimers();
    });

    describe('Constructor', () => {
        test('should wrap the form in a collapsible container', () => {
            expect(document.querySelector('.collapsible-form-wrapper')).toBeTruthy();
        });

        test('should create a toggle button', () => {
            expect(document.querySelector('.collapsible-form-toggle-btn')).toBeTruthy();
        });

        test('should start collapsed when startCollapsed is true', () => {
            expect(collapsible.isCollapsed).toBe(true);
        });

        test('should start expanded when startCollapsed is false', () => {
            document.body.innerHTML = `<div><form id="f2"><input /></form></div>`;
            const f2 = document.getElementById('f2');
            const c2 = new CollapsibleForm(f2, { startCollapsed: false });
            expect(c2.isCollapsed).toBe(false);
        });

        test('should not throw when form is null', () => {
            const consoleStub = jest.spyOn(console, 'error').mockImplementation();
            expect(() => new CollapsibleForm(null)).not.toThrow();
            consoleStub.mockRestore();
        });
    });

    describe('toggle', () => {
        test('should expand when collapsed', () => {
            collapsible.isCollapsed = true;
            collapsible.toggle();
            expect(collapsible.isCollapsed).toBe(false);
        });

        test('should collapse when expanded', () => {
            collapsible.isCollapsed = false;
            collapsible.toggle();
            expect(collapsible.isCollapsed).toBe(true);
        });

        test('should toggle on button click', () => {
            const btn = document.querySelector('.collapsible-form-toggle-btn');
            const before = collapsible.isCollapsed;
            btn.click();
            expect(collapsible.isCollapsed).toBe(!before);
        });
    });

    describe('expand', () => {
        test('should set isCollapsed to false', () => {
            collapsible.expand(false);
            expect(collapsible.isCollapsed).toBe(false);
        });

        test('should add expanded class to wrapper', () => {
            collapsible.expand(false);
            expect(
                document.querySelector('.collapsible-form-wrapper').classList.contains('expanded')
            ).toBe(true);
        });

        test('should add expanded class to toggle button', () => {
            collapsible.expand(false);
            expect(
                document
                    .querySelector('.collapsible-form-toggle-btn')
                    .classList.contains('expanded')
            ).toBe(true);
        });
    });

    describe('collapse', () => {
        test('should set isCollapsed to true', () => {
            collapsible.expand(false);
            collapsible.collapse(false);
            expect(collapsible.isCollapsed).toBe(true);
        });

        test('should remove expanded class from wrapper', () => {
            collapsible.expand(false);
            collapsible.collapse(false);
            expect(
                document.querySelector('.collapsible-form-wrapper').classList.contains('expanded')
            ).toBe(false);
        });
    });

    describe('show / hide aliases', () => {
        test('show() should expand the form', () => {
            collapsible.isCollapsed = true;
            collapsible.show();
            expect(collapsible.isCollapsed).toBe(false);
        });

        test('hide() should collapse the form', () => {
            collapsible.isCollapsed = false;
            collapsible.hide();
            expect(collapsible.isCollapsed).toBe(true);
        });
    });

    describe('isHidden', () => {
        test('should return true when collapsed', () => {
            collapsible.isCollapsed = true;
            expect(collapsible.isHidden()).toBe(true);
        });

        test('should return false when expanded', () => {
            collapsible.isCollapsed = false;
            expect(collapsible.isHidden()).toBe(false);
        });
    });

    describe('collapseAfterSubmit', () => {
        test('should NOT collapse after submit by default', () => {
            collapsible.expand(false);
            form.dispatchEvent(new Event('submit'));
            jest.advanceTimersByTime(200);
            expect(collapsible.isCollapsed).toBe(false);
        });

        test('should collapse after submit when collapseAfterSubmit is true', () => {
            document.body.innerHTML = `<div><form id="f3"><input /></form></div>`;
            const f3 = document.getElementById('f3');
            const c3 = new CollapsibleForm(f3, {
                startCollapsed: false,
                collapseAfterSubmit: true,
            });
            c3.expand(false);
            f3.dispatchEvent(new Event('submit'));
            jest.advanceTimersByTime(200);
            expect(c3.isCollapsed).toBe(true);
        });
    });
});
