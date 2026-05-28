import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';
import { CoverModal } from '@/tableScripts/features/cover/coverModal.js';

describe('CoverModal', () => {
    let coverModal;

    beforeEach(() => {
        const consoleLogSpy = jest.spyOn(console, 'log').mockImplementation();
        coverModal = new CoverModal();
        consoleLogSpy.mockRestore();
    });

    afterEach(() => {
        document.body.innerHTML = '';
    });

    describe('Constructor / init', () => {
        test('should create and append modal element to body', () => {
            expect(document.querySelector('.cover-modal')).toBeTruthy();
        });

        test('should contain a close button', () => {
            expect(document.querySelector('.cover-modal-close')).toBeTruthy();
        });

        test('should contain an image element', () => {
            expect(document.querySelector('.cover-modal-image')).toBeTruthy();
        });

        test('should contain an edit button', () => {
            expect(document.querySelector('.cover-modal-edit-btn')).toBeTruthy();
        });

        test('should be hidden initially', () => {
            const modal = document.querySelector('.cover-modal');
            expect(modal.style.display).not.toBe('block');
        });
    });

    describe('show', () => {
        test('should display the modal', () => {
            coverModal.show('http://example.com/img.jpg', 'Test Game');
            expect(coverModal.modal.style.display).toBe('block');
        });

        test('should set image src', () => {
            coverModal.show('http://example.com/img.jpg', 'Test Game');
            const img = document.querySelector('.cover-modal-image');
            expect(img.src).toBe('http://example.com/img.jpg');
        });

        test('should set title text', () => {
            coverModal.show('http://example.com/img.jpg', 'My Game');
            const titleEl = document.querySelector('.cover-modal-title');
            expect(titleEl.textContent).toBe('My Game');
        });

        test('should store current item name', () => {
            coverModal.show('http://example.com/img.jpg', 'My Game');
            expect(coverModal.currentItemName).toBe('My Game');
        });

        test('should store current cover url', () => {
            coverModal.show('http://example.com/img.jpg', 'My Game');
            expect(coverModal.currentCoverUrl).toBe('http://example.com/img.jpg');
        });

        test('should work without title argument', () => {
            expect(() => coverModal.show('http://example.com/img.jpg')).not.toThrow();
        });
    });

    describe('close', () => {
        test('should hide the modal', () => {
            coverModal.show('http://example.com/img.jpg', 'Test');
            coverModal.close();
            expect(coverModal.modal.style.display).toBe('none');
        });

        test('should restore body overflow', () => {
            coverModal.show('http://example.com/img.jpg', 'Test');
            coverModal.close();
            expect(document.body.style.overflow).toBe('');
        });
    });

    describe('Close interactions', () => {
        test('should close when close button is clicked', () => {
            coverModal.show('http://example.com/img.jpg', 'Test');
            document.querySelector('.cover-modal-close').click();
            expect(coverModal.modal.style.display).toBe('none');
        });

        test('should close on Escape key press', () => {
            coverModal.show('http://example.com/img.jpg', 'Test');
            document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
            expect(coverModal.modal.style.display).toBe('none');
        });

        test('should not close on other key press', () => {
            coverModal.show('http://example.com/img.jpg', 'Test');
            document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
            expect(coverModal.modal.style.display).toBe('block');
        });

        test('should close when clicking backdrop', () => {
            coverModal.show('http://example.com/img.jpg', 'Test');
            coverModal.modal.click();
            expect(coverModal.modal.style.display).toBe('none');
        });
    });

    describe('handleEdit', () => {
        test('should close modal on edit button click', () => {
            coverModal.show('http://example.com/img.jpg', 'Test Game');
            document.querySelector('.cover-modal-edit-btn').click();
            expect(coverModal.modal.style.display).toBe('none');
        });

        test('should dispatch coverEditRequested event', () => {
            const handler = jest.fn();
            document.addEventListener('coverEditRequested', handler);
            coverModal.show('http://example.com/img.jpg', 'Test Game');
            document.querySelector('.cover-modal-edit-btn').click();
            expect(handler).toHaveBeenCalled();
            document.removeEventListener('coverEditRequested', handler);
        });

        test('should include item name in event detail', () => {
            let detail;
            document.addEventListener('coverEditRequested', (e) => {
                detail = e.detail;
            });
            coverModal.show('http://example.com/img.jpg', 'Test Game');
            document.querySelector('.cover-modal-edit-btn').click();
            expect(detail.itemName).toBe('Test Game');
            expect(detail.currentUrl).toBe('http://example.com/img.jpg');
        });
    });
});
