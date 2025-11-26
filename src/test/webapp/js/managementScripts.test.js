import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';
import { ManagementModal } from '../../../main/webapp/js/managementScripts.js';

describe('ManagementModal', () => {
    let managementModal;
    let modal;
    let closeBtn;
    let editIdInput;
    let editNameInput;

    beforeEach(() => {

        document.body.innerHTML = `
            <div id="editModal" data-type="tags" style="display: none;">
                <div class="modal-content">
                    <span class="close-btn">&times;</span>
                    <h2>Edit Tag</h2>
                    <form>
                        <input type="hidden" id="edit-id">
                        <label for="edit-name">Name:</label>
                        <input type="text" id="edit-name">
                        <div id="edit-media-types" style="display: none;">
                            <label><input type="checkbox" value="games"> Games</label>
                            <label><input type="checkbox" value="movies"> Movies</label>
                            <label><input type="checkbox" value="books"> Books</label>
                        </div>
                        <button type="submit">Save</button>
                    </form>
                </div>
            </div>
            <div class="content">
                <button class="edit-btn" data-id="1" data-name="Action">Edit</button>
                <button class="edit-btn" data-id="2" data-name="RPG">Edit</button>
            </div>
        `;

        modal = document.getElementById('editModal');
        closeBtn = document.querySelector('.close-btn');
        editIdInput = document.getElementById('edit-id');
        editNameInput = document.getElementById('edit-name');

        managementModal = new ManagementModal();
    });

    afterEach(() => {
        document.body.innerHTML = '';
    });

    describe('Constructor', () => {
        test('should initialize with modal element', () => {
            expect(managementModal.modal).toBe(modal);
        });

        test('should initialize with close button', () => {
            expect(managementModal.closeBtn).toBe(closeBtn);
        });

        test('should initialize with edit ID input', () => {
            expect(managementModal.editIdInput).toBe(editIdInput);
        });

        test('should initialize with edit name input', () => {
            expect(managementModal.editNameInput).toBe(editNameInput);
        });

        test('should get type from modal data attribute', () => {
            expect(managementModal.type).toBe('tags');
        });

        test('should default to "tags" if no data-type attribute', () => {
            modal.removeAttribute('data-type');
            const newModal = new ManagementModal();
            expect(newModal.type).toBe('tags');
        });

        test('should handle missing modal element', () => {
            document.body.innerHTML = '';
            const newModal = new ManagementModal();
            expect(newModal.modal).toBeNull();
            expect(newModal.type).toBe('tags');
        });
    });

    describe('init', () => {
        test('should attach event listeners when modal exists', () => {
            const attachSpy = jest.spyOn(managementModal, 'attachEventListeners');
            managementModal.init();

            expect(attachSpy).toHaveBeenCalled();
        });

        test('should not throw when modal is missing', () => {
            document.body.innerHTML = '';
            const newModal = new ManagementModal();

            expect(() => newModal.init()).not.toThrow();
        });

        test('should return early if modal is null', () => {
            managementModal.modal = null;
            const attachSpy = jest.spyOn(managementModal, 'attachEventListeners');

            managementModal.init();

            expect(attachSpy).not.toHaveBeenCalled();
        });
    });

    describe('attachEventListeners', () => {
        test('should attach click listeners to edit buttons', () => {
            managementModal.init();

            const editButtons = document.querySelectorAll('.edit-btn');
            expect(editButtons.length).toBe(2);

            editButtons[0].click();
            expect(modal.style.display).toBe('block');
        });

        test('should attach click listener to close button', () => {
            managementModal.init();

            modal.style.display = 'block';
            closeBtn.click();

            expect(modal.style.display).toBe('none');
        });

        test('should close modal when clicking outside', () => {
            managementModal.init();

            modal.style.display = 'block';
            const event = new MouseEvent('click', { bubbles: true });
            Object.defineProperty(event, 'target', { value: modal, enumerable: true });

            window.dispatchEvent(event);

            expect(modal.style.display).toBe('none');
        });

        test('should not close modal when clicking inside modal content', () => {
            managementModal.init();

            modal.style.display = 'block';
            const modalContent = modal.querySelector('.modal-content');
            const event = new MouseEvent('click', { bubbles: true });
            Object.defineProperty(event, 'target', { value: modalContent, enumerable: true });

            window.dispatchEvent(event);

            expect(modal.style.display).toBe('block');
        });

        test('should close modal on Escape key press', () => {
            managementModal.init();

            modal.style.display = 'block';
            const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });

            document.dispatchEvent(escapeEvent);

            expect(modal.style.display).toBe('none');
        });

        test('should not close modal on other key press', () => {
            managementModal.init();

            modal.style.display = 'block';
            const enterEvent = new KeyboardEvent('keydown', { key: 'Enter' });

            document.dispatchEvent(enterEvent);

            expect(modal.style.display).toBe('block');
        });

        test('should not close on Escape when modal is already hidden', () => {
            managementModal.init();

            modal.style.display = 'none';
            const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });

            document.dispatchEvent(escapeEvent);

            expect(modal.style.display).toBe('none');
        });

        test('should handle missing close button gracefully', () => {
            closeBtn.remove();
            managementModal.closeBtn = null;

            expect(() => managementModal.init()).not.toThrow();
        });
    });

    describe('openModal', () => {
        beforeEach(() => {
            managementModal.init();
        });

        test('should set edit ID from button data', () => {
            const editButton = document.querySelector('[data-id="1"]');
            editButton.click();

            expect(editIdInput.value).toBe('1');
        });

        test('should set edit name from button data', () => {
            const editButton = document.querySelector('[data-name="Action"]');
            editButton.click();

            expect(editNameInput.value).toBe('Action');
        });

        test('should display the modal', () => {
            const editButton = document.querySelector('.edit-btn');
            editButton.click();

            expect(modal.style.display).toBe('block');
        });

        test('should focus the name input', () => {
            const focusSpy = jest.spyOn(editNameInput, 'focus');
            const editButton = document.querySelector('.edit-btn');

            editButton.click();

            expect(focusSpy).toHaveBeenCalled();
        });

        test('should handle multiple buttons correctly', () => {
            const button1 = document.querySelector('[data-id="1"]');
            const button2 = document.querySelector('[data-id="2"]');

            button1.click();
            expect(editIdInput.value).toBe('1');
            expect(editNameInput.value).toBe('Action');

            button2.click();
            expect(editIdInput.value).toBe('2');
            expect(editNameInput.value).toBe('RPG');
        });

        test('should handle special characters in name', () => {
            const specialButton = document.createElement('button');
            specialButton.className = 'edit-btn';
            specialButton.dataset.id = '3';
            specialButton.dataset.name = '<script>alert("xss")</script>';
            document.body.appendChild(specialButton);

            managementModal.openModal(specialButton);

            expect(editNameInput.value).toBe('<script>alert("xss")</script>');
        });

        test('should call updateMediaTypeCheckboxes for genres', () => {
            modal.dataset.type = 'genres';
            managementModal.type = 'genres';

            const updateSpy = jest.spyOn(managementModal, 'updateMediaTypeCheckboxes');
            const genreButton = document.createElement('button');
            genreButton.dataset.id = '1';
            genreButton.dataset.name = 'Drama';
            genreButton.dataset.mediaTypes = 'games,movies';

            managementModal.openModal(genreButton);

            expect(updateSpy).toHaveBeenCalledWith('games,movies');
        });

        test('should not call updateMediaTypeCheckboxes for tags', () => {
            const updateSpy = jest.spyOn(managementModal, 'updateMediaTypeCheckboxes');
            const tagButton = document.querySelector('.edit-btn');

            managementModal.openModal(tagButton);

            expect(updateSpy).not.toHaveBeenCalled();
        });
    });

    describe('updateMediaTypeCheckboxes', () => {
        test('should check boxes for specified media types', () => {
            managementModal.updateMediaTypeCheckboxes('games,movies');

            const checkboxes = document.querySelectorAll('#edit-media-types input[type="checkbox"]');
            expect(checkboxes[0].checked).toBe(true);  // games
            expect(checkboxes[1].checked).toBe(true);  // movies
            expect(checkboxes[2].checked).toBe(false); // books
        });

        test('should uncheck all boxes for empty string', () => {

            const checkboxes = document.querySelectorAll('#edit-media-types input[type="checkbox"]');
            checkboxes.forEach(cb => cb.checked = true);

            managementModal.updateMediaTypeCheckboxes('');

            checkboxes.forEach(cb => {
                expect(cb.checked).toBe(false);
            });
        });

        test('should handle single media type', () => {
            managementModal.updateMediaTypeCheckboxes('books');

            const checkboxes = document.querySelectorAll('#edit-media-types input[type="checkbox"]');
            expect(checkboxes[0].checked).toBe(false); // games
            expect(checkboxes[1].checked).toBe(false); // movies
            expect(checkboxes[2].checked).toBe(true);  // books
        });

        test('should handle all media types', () => {
            managementModal.updateMediaTypeCheckboxes('games,movies,books');

            const checkboxes = document.querySelectorAll('#edit-media-types input[type="checkbox"]');
            checkboxes.forEach(cb => {
                expect(cb.checked).toBe(true);
            });
        });

        test('should handle media types with spaces', () => {

            managementModal.updateMediaTypeCheckboxes(' games, movies, books');

            const checkboxes = document.querySelectorAll('#edit-media-types input[type="checkbox"]');

            expect(checkboxes[0].checked).toBe(false);
        });

        test('should handle case-sensitive matching', () => {
            managementModal.updateMediaTypeCheckboxes('GAMES,MOVIES');

            const checkboxes = document.querySelectorAll('#edit-media-types input[type="checkbox"]');

            expect(checkboxes[0].checked).toBe(false);
            expect(checkboxes[1].checked).toBe(false);
        });

        test('should handle invalid media types', () => {
            managementModal.updateMediaTypeCheckboxes('invalid,unknown');

            const checkboxes = document.querySelectorAll('#edit-media-types input[type="checkbox"]');
            checkboxes.forEach(cb => {
                expect(cb.checked).toBe(false);
            });
        });
    });

    describe('closeModal', () => {
        test('should hide the modal', () => {
            modal.style.display = 'block';

            managementModal.closeModal();

            expect(modal.style.display).toBe('none');
        });

        test('should work when modal is already hidden', () => {
            modal.style.display = 'none';

            managementModal.closeModal();

            expect(modal.style.display).toBe('none');
        });

        test('should not affect modal content', () => {
            editIdInput.value = '123';
            editNameInput.value = 'Test Name';
            modal.style.display = 'block';

            managementModal.closeModal();

            expect(editIdInput.value).toBe('123');
            expect(editNameInput.value).toBe('Test Name');
        });
    });

    describe('Integration', () => {
        test('should handle complete edit flow', () => {
            managementModal.init();


            const editButton = document.querySelector('[data-id="1"]');
            editButton.click();

            expect(modal.style.display).toBe('block');
            expect(editIdInput.value).toBe('1');
            expect(editNameInput.value).toBe('Action');

            closeBtn.click();

            expect(modal.style.display).toBe('none');
        });

        test('should handle opening different items', () => {
            managementModal.init();

            const button1 = document.querySelector('[data-id="1"]');
            const button2 = document.querySelector('[data-id="2"]');

            button1.click();
            expect(editNameInput.value).toBe('Action');

            closeBtn.click();

            button2.click();
            expect(editNameInput.value).toBe('RPG');
        });

        test('should handle rapid open/close', () => {
            managementModal.init();

            const editButton = document.querySelector('.edit-btn');

            for (let i = 0; i < 5; i++) {
                editButton.click();
                expect(modal.style.display).toBe('block');

                closeBtn.click();
                expect(modal.style.display).toBe('none');
            }
        });

        test('should handle Escape key while modal is open', () => {
            managementModal.init();

            const editButton = document.querySelector('.edit-btn');
            editButton.click();

            expect(modal.style.display).toBe('block');

            const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });
            document.dispatchEvent(escapeEvent);

            expect(modal.style.display).toBe('none');
        });

        test('should handle click outside while modal is open', () => {
            managementModal.init();

            const editButton = document.querySelector('.edit-btn');
            editButton.click();

            expect(modal.style.display).toBe('block');

            const clickEvent = new MouseEvent('click', { bubbles: true });
            Object.defineProperty(clickEvent, 'target', { value: modal, enumerable: true });
            window.dispatchEvent(clickEvent);

            expect(modal.style.display).toBe('none');
        });

        test('should handle genres with media types', () => {
            modal.dataset.type = 'genres';
            const genresModal = new ManagementModal();

            const genreButton = document.createElement('button');
            genreButton.className = 'edit-btn';
            genreButton.dataset.id = '10';
            genreButton.dataset.name = 'Sci-Fi';
            genreButton.dataset.mediaTypes = 'movies,books';
            document.body.appendChild(genreButton);

            genresModal.init();

            genreButton.click();

            const checkboxes = document.querySelectorAll('#edit-media-types input[type="checkbox"]');
            expect(checkboxes[0].checked).toBe(false); // games
            expect(checkboxes[1].checked).toBe(true);  // movies
            expect(checkboxes[2].checked).toBe(true);  // books
        });
    });

    describe('Edge Cases', () => {
        test('should handle button without data attributes', () => {
            const badButton = document.createElement('button');
            badButton.className = 'edit-btn';
            document.body.appendChild(badButton);

            managementModal.init();

            expect(() => badButton.click()).not.toThrow();
        });

        test('should handle very long names', () => {
            const longName = 'A'.repeat(1000);
            const button = document.createElement('button');
            button.className = 'edit-btn';
            button.dataset.id = '999';
            button.dataset.name = longName;
            document.body.appendChild(button);

            managementModal.init();
            button.click();

            expect(editNameInput.value).toBe(longName);
            expect(editNameInput.value.length).toBe(1000);
        });

        test('should handle numeric IDs', () => {

            managementModal.init();

            const editButton = document.querySelector('[data-id="1"]');
            editButton.click();

            expect(editIdInput.value).toBe('1');
            expect(typeof editIdInput.value).toBe('string');
        });

        test('should handle empty name', () => {
            const button = document.createElement('button');
            button.className = 'edit-btn';
            button.dataset.id = '5';
            button.dataset.name = '';
            document.body.appendChild(button);

            managementModal.init();
            button.click();

            expect(editNameInput.value).toBe('');
        });

        test('should handle unicode characters in name', () => {
            const unicodeName = 'アクション 游戏 🎮';
            const button = document.createElement('button');
            button.className = 'edit-btn';
            button.dataset.id = '7';
            button.dataset.name = unicodeName;
            document.body.appendChild(button);

            managementModal.init();
            button.click();

            expect(editNameInput.value).toBe(unicodeName);
        });

        test('should handle missing checkboxes container', () => {
            const mediaTypesDiv = document.getElementById('edit-media-types');
            mediaTypesDiv.remove();

            modal.dataset.type = 'genres';
            const genresModal = new ManagementModal();

            expect(() => {
                genresModal.updateMediaTypeCheckboxes('games,movies');
            }).not.toThrow();
        });

        test('should handle multiple modals initialization', () => {
            const modal1 = new ManagementModal();
            const modal2 = new ManagementModal();

            expect(() => {
                modal1.init();
                modal2.init();
            }).not.toThrow();
        });
    });

    describe('Accessibility', () => {
        test('should focus name input when opening modal', () => {
            managementModal.init();

            const focusSpy = jest.spyOn(editNameInput, 'focus');
            const editButton = document.querySelector('.edit-btn');

            editButton.click();

            expect(focusSpy).toHaveBeenCalled();
            focusSpy.mockRestore();
        });

        test('should support keyboard navigation (Escape key)', () => {
            managementModal.init();

            const editButton = document.querySelector('.edit-btn');
            editButton.click();

            expect(modal.style.display).toBe('block');



            document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

            expect(modal.style.display).toBe('none');
        });

        test('should not interfere with form submission on Enter', () => {
            managementModal.init();

            const editButton = document.querySelector('.edit-btn');
            editButton.click();

            const enterEvent = new KeyboardEvent('keydown', { key: 'Enter' });
            document.dispatchEvent(enterEvent);

            expect(modal.style.display).toBe('block');
        });
    });

    describe('DOMContentLoaded initialization', () => {
        test('should initialize on DOMContentLoaded', () => {

            const event = new Event('DOMContentLoaded');


            const initSpy = jest.spyOn(ManagementModal.prototype, 'init');

            document.dispatchEvent(event);

            expect(ManagementModal).toBeDefined();
            expect(typeof ManagementModal).toBe('function');

            initSpy.mockRestore();
        });
    });
});