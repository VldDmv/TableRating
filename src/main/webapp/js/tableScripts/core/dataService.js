/**
 * Service for fetching data from the server.
 */

import { CONSTANTS } from './utils.js';
import { ErrorHandler } from './errorHandler.js';

export class DataService {
    /**
     * Creates a new DataService instance.
     * @param {Object} config - Configuration object.
     */
    constructor(config) {
        this.config = config;
        this.abortController = null;
    }

    /**
     * Fetches paginated data based on current state.
     * @param {Object} state - Current application state.
     * @returns {Promise<Object>} Page result object.
     */
   async fetchData(state) {
          if (this.abortController) {
              this.abortController.abort();
          }
          this.abortController = new AbortController();
          const signal = this.abortController.signal;
          const url = this.buildUrl(state);

          try {
              const response = await fetch(url, {
                  signal: signal,
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
                  console.log('Request aborted');
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
     * Builds the API URL with query parameters.
     * @param {Object} state - Current application state.
     * @returns {URL} Constructed URL object.
     */
    buildUrl(state) {
        const baseUrl = window.location.origin + window.location.pathname;
        const url = new URL(baseUrl);

        url.searchParams.set('page', state.currentPage);
        url.searchParams.set('rows', state.rowsPerPage);
        url.searchParams.set('sortBy', state.sortBy);
        url.searchParams.set('sortOrder', state.sortOrder);

        const filterParamName = (this.config.entityType === 'games') ? 'tag_id' : 'genre_id';
        if (state.filterId !== 'all') {
            url.searchParams.set(filterParamName, state.filterId);
        }

        if (state.searchTerm) {
            url.searchParams.set('search', state.searchTerm);
        }

        return url;
    }

    /**
     * Posts data to the server.
     * @param {string} endpoint - API endpoint.
     * @param {Object} data - Data to send.
     * @returns {Promise<Object>} Response data.
     */
    async postData(endpoint, data) {
        try {
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: new URLSearchParams(data).toString()
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