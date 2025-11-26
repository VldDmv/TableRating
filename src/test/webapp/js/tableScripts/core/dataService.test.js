
import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';
import { DataService } from '../../../../../main/webapp/js/tableScripts/core/dataService.js';

describe('DataService', () => {
    let dataService;
    let mockConfig;


    const originalLocation = window.location;

    beforeEach(() => {
        mockConfig = {
            entityType: 'games',
            apiEndpoint: 'category'
        };
        dataService = new DataService(mockConfig);

        global.fetch = jest.fn();

        Object.defineProperty(window, 'location', {
            writable: true,
            value: {
                origin: 'http://test.com',
                pathname: '/games'
            }
        });

    });

    afterEach(() => {

        Object.defineProperty(window, 'location', {
            writable: true,
            value: originalLocation
        });
        jest.restoreAllMocks();
    });

    describe('Constructor', () => {
        test('should initialize with config', () => {
            expect(dataService.config).toBe(mockConfig);
        });

        test('should store entity type from config', () => {
            expect(dataService.config.entityType).toBe('games');
        });
    });

    describe('buildUrl', () => {
        test('should build correct URL with basic parameters', () => {
            const state = {
                currentPage: 1,
                rowsPerPage: 10,
                sortBy: 'name',
                sortOrder: 'asc',
                filterId: 'all',
                searchTerm: ''
            };

            const url = dataService.buildUrl(state);

            expect(url.origin).toBe('http://test.com');
            expect(url.pathname).toBe('/games');
            expect(url.searchParams.get('page')).toBe('1');
            expect(url.searchParams.get('rows')).toBe('10');
            expect(url.searchParams.get('sortBy')).toBe('name');
            expect(url.searchParams.get('sortOrder')).toBe('asc');
        });

        test('should include search parameter when searchTerm is provided', () => {
            const state = {
                currentPage: 1,
                rowsPerPage: 10,
                sortBy: 'name',
                sortOrder: 'asc',
                filterId: 'all',
                searchTerm: 'test game'
            };

            const url = dataService.buildUrl(state);

            expect(url.searchParams.get('search')).toBe('test game');
        });

        test('should not include search parameter when searchTerm is empty', () => {
            const state = {
                currentPage: 1,
                rowsPerPage: 10,
                sortBy: 'name',
                sortOrder: 'asc',
                filterId: 'all',
                searchTerm: ''
            };

            const url = dataService.buildUrl(state);

            expect(url.searchParams.has('search')).toBe(false);
        });

        test('should use tag_id for games entity', () => {
            const state = {
                currentPage: 1,
                rowsPerPage: 10,
                sortBy: 'name',
                sortOrder: 'asc',
                filterId: '5',
                searchTerm: ''
            };

            const url = dataService.buildUrl(state);

            expect(url.searchParams.get('tag_id')).toBe('5');
            expect(url.searchParams.has('genre_id')).toBe(false);
        });

        test('should use genre_id for movies entity', () => {
            const moviesConfig = { entityType: 'movies' };
            const moviesService = new DataService(moviesConfig);

            const state = {
                currentPage: 1,
                rowsPerPage: 10,
                sortBy: 'name',
                sortOrder: 'asc',
                filterId: '3',
                searchTerm: ''
            };

            const url = moviesService.buildUrl(state);

            expect(url.searchParams.get('genre_id')).toBe('3');
            expect(url.searchParams.has('tag_id')).toBe(false);
        });

        test('should not include filter param when filterId is "all"', () => {
            const state = {
                currentPage: 1,
                rowsPerPage: 10,
                sortBy: 'name',
                sortOrder: 'asc',
                filterId: 'all',
                searchTerm: ''
            };

            const url = dataService.buildUrl(state);

            expect(url.searchParams.has('tag_id')).toBe(false);
            expect(url.searchParams.has('genre_id')).toBe(false);
        });

        test('should handle all parameters at once', () => {
            const state = {
                currentPage: 3,
                rowsPerPage: 25,
                sortBy: 'score',
                sortOrder: 'desc',
                filterId: '7',
                searchTerm: 'mario'
            };

            const url = dataService.buildUrl(state);

            expect(url.searchParams.get('page')).toBe('3');
            expect(url.searchParams.get('rows')).toBe('25');
            expect(url.searchParams.get('sortBy')).toBe('score');
            expect(url.searchParams.get('sortOrder')).toBe('desc');
            expect(url.searchParams.get('tag_id')).toBe('7');
            expect(url.searchParams.get('search')).toBe('mario');
        });

        test('should handle special characters in search term', () => {
            const state = {
                currentPage: 1,
                rowsPerPage: 10,
                sortBy: 'name',
                sortOrder: 'asc',
                filterId: 'all',
                searchTerm: 'test & game'
            };

            const url = dataService.buildUrl(state);

            expect(url.searchParams.get('search')).toBe('test & game');
        });
    });

    describe('fetchData', () => {
        test('should fetch data successfully', async () => {
            const mockData = {
                success: true,
                data: {
                    items: [
                        { name: 'Test Game', score: 85, tags: [], completed: true }
                    ],
                    currentPage: 1,
                    totalPages: 5
                }
            };

            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => mockData
            });

            const state = {
                currentPage: 1,
                rowsPerPage: 10,
                sortBy: 'name',
                sortOrder: 'asc',
                filterId: 'all',
                searchTerm: ''
            };

            const result = await dataService.fetchData(state);

            expect(result).toEqual(mockData.data);
            expect(global.fetch).toHaveBeenCalledTimes(1);
        });

        test('should include AJAX headers in request', async () => {
            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => ({ success: true, data: {} })
            });

            const state = {
                currentPage: 1,
                rowsPerPage: 10,
                sortBy: 'name',
                sortOrder: 'asc',
                filterId: 'all',
                searchTerm: ''
            };

            await dataService.fetchData(state);

            expect(global.fetch).toHaveBeenCalledWith(
                expect.any(URL),
                expect.objectContaining({
                    headers: {
                        'X-Requested-With-AJAX': 'true'
                    }
                })
            );
        });

        test('should return data when success is true', async () => {
            const mockResponse = {
                success: true,
                data: { items: [], currentPage: 1, totalPages: 1 }
            };

            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => mockResponse
            });

            const state = {
                currentPage: 1,
                rowsPerPage: 10,
                sortBy: 'name',
                sortOrder: 'asc',
                filterId: 'all',
                searchTerm: ''
            };

            const result = await dataService.fetchData(state);

            expect(result).toEqual(mockResponse.data);
        });

        test('should return full result when success is not present', async () => {
            const mockResponse = {
                items: [],
                currentPage: 1,
                totalPages: 1
            };

            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => mockResponse
            });

            const state = {
                currentPage: 1,
                rowsPerPage: 10,
                sortBy: 'name',
                sortOrder: 'asc',
                filterId: 'all',
                searchTerm: ''
            };

            const result = await dataService.fetchData(state);

            expect(result).toEqual(mockResponse);
        });

        test('should throw error when response is not ok', async () => {
            global.fetch.mockResolvedValue({
                ok: false,
                status: 404,
                text: async () => 'Not Found'
            });

            const state = {
                currentPage: 1,
                rowsPerPage: 10,
                sortBy: 'name',
                sortOrder: 'asc',
                filterId: 'all',
                searchTerm: ''
            };

            await expect(dataService.fetchData(state)).rejects.toThrow('Failed to fetch data');
        });

        test('should handle network errors', async () => {
            global.fetch.mockRejectedValue(new Error('Network error'));

            const state = {
                currentPage: 1,
                rowsPerPage: 10,
                sortBy: 'name',
                sortOrder: 'asc',
                filterId: 'all',
                searchTerm: ''
            };

            await expect(dataService.fetchData(state)).rejects.toThrow('Failed to fetch data: Network error');
        });

        test('should parse error response correctly', async () => {
            global.fetch.mockResolvedValue({
                ok: false,
                status: 500,
                text: async () => JSON.stringify({ message: 'Server error' })
            });

            const state = {
                currentPage: 1,
                rowsPerPage: 10,
                sortBy: 'name',
                sortOrder: 'asc',
                filterId: 'all',
                searchTerm: ''
            };

            await expect(dataService.fetchData(state)).rejects.toThrow();
        });
    });

    describe('postData', () => {
        test('should post data successfully', async () => {
            const mockResponse = {
                success: true,
                message: 'Item added'
            };

            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => mockResponse
            });

            const result = await dataService.postData('/api/add', {
                name: 'Test',
                score: 85
            });

            expect(result).toEqual(mockResponse);
        });

        test('should send data as URL-encoded form', async () => {
            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => ({ success: true })
            });

            await dataService.postData('/api/add', {
                name: 'Test',
                score: 85
            });

            expect(global.fetch).toHaveBeenCalledWith(
                '/api/add',
                expect.objectContaining({
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    body: expect.stringContaining('name=Test')
                })
            );
        });

        test('should handle POST request errors', async () => {
            global.fetch.mockResolvedValue({
                ok: false,
                status: 400,
                text: async () => 'Bad Request'
            });

            await expect(
                dataService.postData('/api/add', { name: 'Test' })
            ).rejects.toThrow('Failed to post data');
        });

        test('should handle network errors on POST', async () => {
            global.fetch.mockRejectedValue(new Error('Connection failed'));

            await expect(
                dataService.postData('/api/add', { name: 'Test' })
            ).rejects.toThrow('Failed to post data: Connection failed');
        });

        test('should encode special characters in POST data', async () => {
            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => ({ success: true })
            });

            await dataService.postData('/api/add', {
                name: 'Test & Game',
                description: 'A=B'
            });

            const callArgs = global.fetch.mock.calls[0][1];
            expect(callArgs.body).toContain('Test+%26+Game');
        });

        test('should handle empty data object', async () => {
            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => ({ success: true })
            });

            await dataService.postData('/api/test', {});

            expect(global.fetch).toHaveBeenCalledWith(
                '/api/test',
                expect.objectContaining({
                    body: ''
                })
            );
        });

        test('should handle multiple values in POST data', async () => {
            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => ({ success: true })
            });

            await dataService.postData('/api/add', {
                name: 'Test',
                score: 85,
                tags: ['action', 'rpg'],
                completed: true
            });

            expect(global.fetch).toHaveBeenCalled();
            const callArgs = global.fetch.mock.calls[0][1];
            expect(callArgs.body).toContain('name=Test');
            expect(callArgs.body).toContain('score=85');
            expect(callArgs.body).toContain('completed=true');
        });
    });

    describe('Integration', () => {
        test('should work with different entity types', async () => {
            const moviesService = new DataService({ entityType: 'movies' });

            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => ({
                    success: true,
                    data: { items: [], currentPage: 1, totalPages: 1 }
                })
            });

            const state = {
                currentPage: 1,
                rowsPerPage: 10,
                sortBy: 'name',
                sortOrder: 'asc',
                filterId: '2',
                searchTerm: ''
            };

            const url = moviesService.buildUrl(state);
            expect(url.searchParams.get('genre_id')).toBe('2');

            await moviesService.fetchData(state);
            expect(global.fetch).toHaveBeenCalled();
        });

        test('should handle sequential fetch requests', async () => {
            global.fetch.mockResolvedValue({
                ok: true,
                json: async () => ({
                    success: true,
                    data: { items: [], currentPage: 1, totalPages: 1 }
                })
            });

            const state1 = {
                currentPage: 1,
                rowsPerPage: 10,
                sortBy: 'name',
                sortOrder: 'asc',
                filterId: 'all',
                searchTerm: ''
            };

            const state2 = {
                currentPage: 2,
                rowsPerPage: 10,
                sortBy: 'score',
                sortOrder: 'desc',
                filterId: '1',
                searchTerm: 'test'
            };

            await dataService.fetchData(state1);
            await dataService.fetchData(state2);

            expect(global.fetch).toHaveBeenCalledTimes(2);
        });
    });
});