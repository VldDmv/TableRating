/**
 * Service for fetching data from the server.
 */

import { CONSTANTS } from './utils.js';
import { ErrorHandler } from './errorHandler.js';

export class DataService {
   /**
   * @param {Object} config — entity configuration from ENTITY_CONFIGS
   */
    constructor(config) {
        this.config = config;
        this.abortController = null;
    }

   /**
   * Loads a data page based on the current state.
   * @param {Object} state — the current state of the application
   * @returns {Promise<Object|null>}
   */
    async fetchData(state) {

        this.abortController?.abort();
        this.abortController = new AbortController();
        const { signal } = this.abortController;

        const url = this.buildUrl(state);

        try {
            const response = await fetch(url, {
                signal,
                headers: {
                    [CONSTANTS.AJAX_HEADER]: CONSTANTS.AJAX_HEADER_VALUE
                }
            });

            if (!response.ok) {
                const errorMessage = await ErrorHandler.parseErrorResponse(response);
                throw new Error(errorMessage);
            }

            const result = await response.json();
            return result.success ? result.data : result;

        } catch (error) {
            if (error.name === 'AbortError') {
                return null;
            }
            throw new Error(`Failed to fetch data: ${error.message}`);

        } finally {

            if (this.abortController?.signal === signal) {
                this.abortController = null;
            }
        }
    }

 /**
 * Builds a request URL from the current state.
 * @param {Object} state
 * @returns {URL}
 */
    buildUrl(state) {
        const base = `${window.location.origin}/api/category/${this.config.entityType}`;
        const url  = new URL(base);

        url.searchParams.set('page',      state.currentPage);
        url.searchParams.set('rows',      state.rowsPerPage);
        url.searchParams.set('sortBy',    state.sortBy);
        url.searchParams.set('sortOrder', state.sortOrder);

        if (state.filterId !== 'all') {
            url.searchParams.set(this.config.filterParamName, state.filterId);
        }

        if (state.searchTerm) {
            url.searchParams.set('search', state.searchTerm);
        }

        if (state.minScore != null && state.minScore !== '') {
            url.searchParams.set('minScore', state.minScore);
        }

        if (state.maxScore != null && state.maxScore !== '') {
            url.searchParams.set('maxScore', state.maxScore);
        }

        return url;
    }

   /**
   * Sends data to the server using the POST method.
   * @param {string} endpoint
   * @param {Object} data
   * @returns {Promise<Object>}
   */
    async postData(endpoint, data) {
        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body:    new URLSearchParams(data).toString()
            });

            if (!response.ok) {
                const errorMessage = await ErrorHandler.parseErrorResponse(response);
                throw new Error(errorMessage);
            }

            return await response.json();

        } catch (error) {
            throw new Error(`Failed to post data: ${error.message}`);
        }
    }
}