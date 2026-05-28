/**
 * Manages application state with observer pattern.
 */

export class StateManager {
    /**
     * Creates a new StateManager instance.
     * @param {Object} initialState - Initial state object.
     */
    constructor(initialState = {}) {
        this.state = { ...initialState };
        this.listeners = [];
    }

    /**
     * Gets the current state.
     * @returns {Object} Current state object.
     */
    getState() {
        return { ...this.state };
    }

    /**
     * Updates state and notifies listeners.
     * @param {Object} updates - State updates to apply.
     */
    setState(updates) {
        const oldState = { ...this.state };
        this.state = { ...this.state, ...updates };
        this.notifyListeners(oldState, this.state);
    }

    /**
     * Subscribes a listener to state changes.
     * @param {Function} listener - Callback function (oldState, newState) => void.
     * @returns {Function} Unsubscribe function.
     */
    subscribe(listener) {
        this.listeners.push(listener);
        return () => {
            this.listeners = this.listeners.filter((l) => l !== listener);
        };
    }

    /**
     * Notifies all listeners of state change.
     * @param {Object} oldState - Previous state.
     * @param {Object} newState - New state.
     */
    notifyListeners(oldState, newState) {
        this.listeners.forEach((listener) => {
            try {
                listener(oldState, newState);
            } catch (error) {
                console.error('Error in state listener:', error);
            }
        });
    }

    /**
     * Resets state to initial values.
     * @param {Object} initialState - State to reset to.
     */
    reset(initialState = {}) {
        const oldState = { ...this.state };
        this.state = { ...initialState };
        this.notifyListeners(oldState, this.state);
    }
}
