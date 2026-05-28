import { describe, test, expect, jest, beforeEach } from '@jest/globals';
import { StateManager } from '@/tableScripts/core/stateManager.js';

describe('StateManager', () => {
    let stateManager;

    beforeEach(() => {
        stateManager = new StateManager({ count: 0, name: 'test' });
    });

    describe('Constructor', () => {
        test('should initialize with provided state', () => {
            expect(stateManager.getState()).toEqual({ count: 0, name: 'test' });
        });

        test('should initialize with empty state when no argument', () => {
            const sm = new StateManager();
            expect(stateManager.getState()).toBeDefined();
        });

        test('should start with no listeners', () => {
            expect(stateManager.listeners).toHaveLength(0);
        });
    });

    describe('getState', () => {
        test('should return current state', () => {
            expect(stateManager.getState().count).toBe(0);
        });

        test('should return a copy, not the internal reference', () => {
            const state = stateManager.getState();
            state.count = 999;
            expect(stateManager.getState().count).toBe(0);
        });
    });

    describe('setState', () => {
        test('should merge updates into existing state', () => {
            stateManager.setState({ count: 5 });
            expect(stateManager.getState()).toEqual({ count: 5, name: 'test' });
        });

        test('should not remove existing keys not in update', () => {
            stateManager.setState({ count: 1 });
            expect(stateManager.getState().name).toBe('test');
        });

        test('should add new keys', () => {
            stateManager.setState({ page: 2 });
            expect(stateManager.getState().page).toBe(2);
        });

        test('should notify listeners after update', () => {
            const listener = jest.fn();
            stateManager.subscribe(listener);
            stateManager.setState({ count: 1 });
            expect(listener).toHaveBeenCalledTimes(1);
        });

        test('should pass oldState and newState to listeners', () => {
            const listener = jest.fn();
            stateManager.subscribe(listener);
            stateManager.setState({ count: 7 });
            expect(listener).toHaveBeenCalledWith(
                { count: 0, name: 'test' },
                { count: 7, name: 'test' }
            );
        });
    });

    describe('subscribe', () => {
        test('should add listener', () => {
            const listener = jest.fn();
            stateManager.subscribe(listener);
            expect(stateManager.listeners).toHaveLength(1);
        });

        test('should return an unsubscribe function', () => {
            const listener = jest.fn();
            const unsubscribe = stateManager.subscribe(listener);
            expect(typeof unsubscribe).toBe('function');
        });

        test('unsubscribe should remove the listener', () => {
            const listener = jest.fn();
            const unsubscribe = stateManager.subscribe(listener);
            unsubscribe();
            stateManager.setState({ count: 1 });
            expect(listener).not.toHaveBeenCalled();
        });

        test('should support multiple listeners', () => {
            const l1 = jest.fn();
            const l2 = jest.fn();
            stateManager.subscribe(l1);
            stateManager.subscribe(l2);
            stateManager.setState({ count: 1 });
            expect(l1).toHaveBeenCalled();
            expect(l2).toHaveBeenCalled();
        });

        test('should not remove other listeners when one unsubscribes', () => {
            const l1 = jest.fn();
            const l2 = jest.fn();
            const unsub1 = stateManager.subscribe(l1);
            stateManager.subscribe(l2);
            unsub1();
            stateManager.setState({ count: 1 });
            expect(l1).not.toHaveBeenCalled();
            expect(l2).toHaveBeenCalled();
        });
    });

    describe('reset', () => {
        test('should replace state with new initial state', () => {
            stateManager.setState({ count: 5 });
            stateManager.reset({ count: 0, name: 'reset' });
            expect(stateManager.getState()).toEqual({ count: 0, name: 'reset' });
        });

        test('should notify listeners', () => {
            const listener = jest.fn();
            stateManager.subscribe(listener);
            stateManager.reset({});
            expect(listener).toHaveBeenCalled();
        });

        test('should reset to empty state when no argument', () => {
            stateManager.setState({ count: 5 });
            stateManager.reset();
            expect(stateManager.getState()).toEqual({});
        });
    });

    describe('notifyListeners', () => {
        test('should not throw if a listener throws', () => {
            const badListener = jest.fn(() => {
                throw new Error('listener error');
            });
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
            stateManager.subscribe(badListener);
            expect(() => stateManager.setState({ count: 1 })).not.toThrow();
            consoleErrorSpy.mockRestore();
        });
    });
});
