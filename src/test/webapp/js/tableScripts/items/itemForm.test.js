import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';
import { ItemFormManager } from '@/tableScripts/items/itemForm.js';

describe('ItemFormManager', () => {
    let formManager;
    let formElement;
    let scoreInput;
    let validateScore;

    beforeEach(() => {
        formElement = document.createElement('form');
        scoreInput = document.createElement('input');
        scoreInput.type = 'text';
        scoreInput.name = 'gameScore';

        const nameInput = document.createElement('input');
        nameInput.type = 'text';
        nameInput.name = 'gameName';

        const submitButton = document.createElement('button');
        submitButton.type = 'submit';
        submitButton.textContent = 'Add Game';

        formElement.appendChild(nameInput);
        formElement.appendChild(scoreInput);
        formElement.appendChild(submitButton);

        validateScore = jest.fn((val) => {
            const score = parseInt(val, 10);
            return !isNaN(score) && score >= 1 && score <= 100;
        });

        formManager = new ItemFormManager(formElement, scoreInput, validateScore);
    });

    describe('Constructor', () => {
        test('should initialize with form, score input, and validator', () => {
            expect(formManager.formElement).toBe(formElement);
            expect(formManager.scoreInputElement).toBe(scoreInput);
            expect(formManager.validateScore).toBe(validateScore);
        });
    });

    describe('init', () => {
        test('should attach submit event listener', () => {
            const spy = jest.spyOn(formElement, 'addEventListener');

            formManager.init();

            expect(spy).toHaveBeenCalledWith('submit', expect.any(Function));
        });

        test('should log error if form element is missing', () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
            const manager = new ItemFormManager(null, scoreInput, validateScore);

            manager.init();

            expect(consoleErrorSpy).toHaveBeenCalled();
            consoleErrorSpy.mockRestore();
        });

        test('should log error if score input is missing', () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
            const manager = new ItemFormManager(formElement, null, validateScore);

            manager.init();

            expect(consoleErrorSpy).toHaveBeenCalled();
            consoleErrorSpy.mockRestore();
        });
    });

    describe('handleSubmit', () => {
        test('should return true for valid score', () => {
            scoreInput.value = '85';
            validateScore.mockReturnValue(true);

            const result = formManager.handleSubmit();

            expect(result).toBe(true);
            expect(validateScore).toHaveBeenCalledWith('85');
        });

        test('should return false for invalid score', () => {
            scoreInput.value = '101';
            validateScore.mockReturnValue(false);

            const result = formManager.handleSubmit();

            expect(result).toBe(false);
            expect(validateScore).toHaveBeenCalledWith('101');
        });

        test('should focus score input on validation failure', () => {
            scoreInput.value = '0';
            validateScore.mockReturnValue(false);
            const focusSpy = jest.spyOn(scoreInput, 'focus');

            formManager.handleSubmit();

            expect(focusSpy).toHaveBeenCalled();
        });

        test('should validate with current input value', () => {
            scoreInput.value = '50';
            validateScore.mockReturnValue(true);

            formManager.handleSubmit();

            expect(validateScore).toHaveBeenCalledWith('50');
        });

        test('should handle empty score', () => {
            scoreInput.value = '';
            validateScore.mockReturnValue(false);

            const result = formManager.handleSubmit();

            expect(result).toBe(false);
            expect(validateScore).toHaveBeenCalledWith('');
        });

        test('should handle whitespace in score', () => {
            scoreInput.value = '  85  ';
            validateScore.mockReturnValue(true);

            formManager.handleSubmit();

              expect(validateScore).toHaveBeenCalledWith('85')
        });
    });

    describe('Form Submission', () => {
        test('should allow submission when validation passes', () => {
            formManager.init();
            validateScore.mockReturnValue(true);

            const event = new Event('submit', { cancelable: true });
            const preventDefaultSpy = jest.spyOn(event, 'preventDefault');

            formElement.dispatchEvent(event);

            expect(preventDefaultSpy).not.toHaveBeenCalled();
        });

        test('should prevent submission when validation fails', () => {
            formManager.init();
            validateScore.mockReturnValue(false);

            const event = new Event('submit', { cancelable: true });
            const preventDefaultSpy = jest.spyOn(event, 'preventDefault');

            formElement.dispatchEvent(event);

            expect(preventDefaultSpy).toHaveBeenCalled();
        });

        test('should call validateScore on submit', () => {
            formManager.init();
            scoreInput.value = '75';

            formElement.dispatchEvent(new Event('submit', { cancelable: true }));

            expect(validateScore).toHaveBeenCalledWith('75');
        });

        test('should handle multiple submit attempts', () => {
            formManager.init();
            validateScore.mockReturnValue(false);

            formElement.dispatchEvent(new Event('submit', { cancelable: true }));
            formElement.dispatchEvent(new Event('submit', { cancelable: true }));

            expect(validateScore).toHaveBeenCalledTimes(2);
        });
    });

    describe('reset', () => {
        test('should reset form fields', () => {
            scoreInput.value = '99';
            const nameInput = formElement.querySelector('input[name="gameName"]');
            nameInput.value = 'Test Game';

            formManager.reset();

            expect(scoreInput.value).toBe('');
            expect(nameInput.value).toBe('');
        });

        test('should handle missing form element', () => {
            const manager = new ItemFormManager(null, scoreInput, validateScore);

            expect(() => manager.reset()).not.toThrow();
        });

        test('should handle form with no inputs', () => {
            const emptyForm = document.createElement('form');
            const manager = new ItemFormManager(emptyForm, scoreInput, validateScore);

            expect(() => manager.reset()).not.toThrow();
        });
    });

    describe('setDisabled', () => {
        test('should disable all form elements', () => {
            formManager.setDisabled(true);

            const inputs = formElement.querySelectorAll('input, button');
            inputs.forEach(input => {
                expect(input.disabled).toBe(true);
            });
        });

        test('should enable all form elements', () => {
            formElement.querySelectorAll('input, button').forEach(el => {
                el.disabled = true;
            });

            formManager.setDisabled(false);

            const inputs = formElement.querySelectorAll('input, button');
            inputs.forEach(input => {
                expect(input.disabled).toBe(false);
            });
        });

        test('should handle missing form element', () => {
            const manager = new ItemFormManager(null, scoreInput, validateScore);

            expect(() => manager.setDisabled(true)).not.toThrow();
        });

        test('should handle form with no elements', () => {
            const emptyForm = document.createElement('form');
            const manager = new ItemFormManager(emptyForm, scoreInput, validateScore);

            expect(() => manager.setDisabled(true)).not.toThrow();
        });

        test('should affect all element types', () => {
            const textarea = document.createElement('textarea');
            const select = document.createElement('select');

            formElement.appendChild(textarea);
            formElement.appendChild(select);

            formManager.setDisabled(true);

            expect(textarea.disabled).toBe(true);
            expect(select.disabled).toBe(true);
        });
    });

    describe('Validation Integration', () => {
        test('should validate scores between 1 and 100', () => {
            formManager.init();

            const validScores = ['1', '50', '100'];
            validScores.forEach(score => {
                scoreInput.value = score;
                expect(formManager.handleSubmit()).toBe(true);
            });
        });

       test('should reject scores outside valid range', () => {
                  formManager.init();

                  const invalidScores = ['0', '-1', '101', '1000'];
                  invalidScores.forEach(score => {
                      scoreInput.value = score;
                      const result = formManager.handleSubmit();
                      expect(result).toBe(false);
                  });
              });

              test('should reject non-numeric scores', () => {
                  formManager.init();

                  const invalidScores = ['abc', 'test', 'NaN'];
                  invalidScores.forEach(score => {
                      scoreInput.value = score;
                      expect(formManager.handleSubmit()).toBe(false);
                  });
              });

   test('should use custom validator function', () => {
              const customValidator = jest.fn(() => true);
              const manager = new ItemFormManager(formElement, scoreInput, customValidator);

              scoreInput.value = 'anything';
              manager.handleSubmit();

              expect(customValidator).toHaveBeenCalledWith('anything');
          });
      });

    describe('Integration', () => {
        test('should handle complete form submission flow', () => {
            formManager.init();
            validateScore.mockReturnValue(true);

            const nameInput = formElement.querySelector('input[name="gameName"]');
            nameInput.value = 'New Game';
            scoreInput.value = '85';

            const event = new Event('submit', { cancelable: true });
            const preventDefaultSpy = jest.spyOn(event, 'preventDefault');

            formElement.dispatchEvent(event);

            expect(validateScore).toHaveBeenCalledWith('85');
            expect(preventDefaultSpy).not.toHaveBeenCalled();
        });

        test('should handle form reset after submission', () => {
            formManager.init();

            scoreInput.value = '85';
            formElement.querySelector('input[name="gameName"]').value = 'Test';

            formManager.reset();

            expect(scoreInput.value).toBe('');
            expect(formElement.querySelector('input[name="gameName"]').value).toBe('');
        });

        test('should handle disable during submission', () => {
            formManager.init();
            validateScore.mockReturnValue(true);

            formManager.setDisabled(true);

            const submitButton = formElement.querySelector('button[type="submit"]');
            expect(submitButton.disabled).toBe(true);

            formManager.setDisabled(false);
            expect(submitButton.disabled).toBe(false);
        });
    });

    describe('Edge Cases', () => {
        test('should handle form with many inputs', () => {
            for (let i = 0; i < 100; i++) {
                const input = document.createElement('input');
                input.name = `field${i}`;
                formElement.appendChild(input);
            }

            formManager.setDisabled(true);

            const inputs = formElement.querySelectorAll('input');
            expect(inputs.length).toBeGreaterThan(100);
            inputs.forEach(input => {
                expect(input.disabled).toBe(true);
            });
        });

        test('should handle validator that throws error', () => {
            const throwingValidator = jest.fn(() => {
                throw new Error('Validation error');
            });
            const manager = new ItemFormManager(formElement, scoreInput, throwingValidator);

            expect(() => manager.handleSubmit()).toThrow('Validation error');
        });

        test('should handle form with nested elements', () => {
            const fieldset = document.createElement('fieldset');
            const nestedInput = document.createElement('input');
            nestedInput.type = 'text';

            fieldset.appendChild(nestedInput);
            formElement.appendChild(fieldset);

            formManager.setDisabled(true);

            expect(nestedInput.disabled).toBe(true);
        });

        test('should handle rapid enable/disable toggles', () => {
            for (let i = 0; i < 10; i++) {
                formManager.setDisabled(i % 2 === 0);
            }

            const inputs = formElement.querySelectorAll('input, button');
            inputs.forEach(input => {
                expect(input.disabled).toBe(false);
            });
        });

        test('should handle form with read-only inputs', () => {
            scoreInput.readOnly = true;

            expect(() => formManager.setDisabled(true)).not.toThrow();
        });

        test('should handle score input with min/max attributes', () => {
            scoreInput.min = '1';
            scoreInput.max = '100';

            formManager.init();

            scoreInput.value = '50';
            expect(formManager.handleSubmit()).toBe(true);
        });

        test('should handle very long form', () => {
            const longForm = document.createElement('form');

            for (let i = 0; i < 1000; i++) {
                const input = document.createElement('input');
                longForm.appendChild(input);
            }

            const manager = new ItemFormManager(longForm, scoreInput, validateScore);

            const start = performance.now();
            manager.setDisabled(true);
            const end = performance.now();

            expect(end - start).toBeLessThan(2000);
        });
    });



    describe('Accessibility', () => {
        test('should maintain focus on score input after validation failure', () => {
            formManager.init();
            validateScore.mockReturnValue(false);

            const focusSpy = jest.spyOn(scoreInput, 'focus');

            formElement.dispatchEvent(new Event('submit', { cancelable: true }));

            expect(focusSpy).toHaveBeenCalled();
        });

        test('should not interfere with form accessibility attributes', () => {
            scoreInput.setAttribute('aria-label', 'Game score');
            scoreInput.setAttribute('aria-required', 'true');

            formManager.setDisabled(true);

            expect(scoreInput.getAttribute('aria-label')).toBe('Game score');
            expect(scoreInput.getAttribute('aria-required')).toBe('true');
        });
    });
});