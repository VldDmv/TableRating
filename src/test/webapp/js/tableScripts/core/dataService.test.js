

import { jest } from '@jest/globals';

jest.unstable_mockModule('@/tableScripts/core/utils.js', () => ({
    CONSTANTS: {
        AJAX_HEADER:       'X-Requested-With',
        AJAX_HEADER_VALUE: 'XMLHttpRequest'
    }
}));

jest.unstable_mockModule('@/tableScripts/core/errorHandler.js', () => ({
    ErrorHandler: {
        parseErrorResponse: jest.fn(async () => 'Error 500'),
        handle:             jest.fn()
    }
}));

const { DataService } = await import('@/tableScripts/core/dataService.js');

// ─── Helpers ──────────────────────────────────────────────────────────────────

function makeConfig(overrides = {}) {
    return { entityType: 'games', filterParamName: 'tag_id', ...overrides };
}

function makeState(overrides = {}) {
    return {
        currentPage: 1,
        rowsPerPage: 25,
        sortBy:      'name',
        sortOrder:   'asc',
        filterId:    'all',
        searchTerm:  '',
        ...overrides
    };
}

beforeEach(() => {
    Object.defineProperty(window, 'location', {
        value: { origin: 'http://localhost' },
        writable: true
    });
});

// ─── buildUrl ─────────────────────────────────────────────────────────────────

describe('DataService.buildUrl', () => {
    test('builds correct base URL pathname for entity type', () => {
        const url = new DataService(makeConfig()).buildUrl(makeState());
        expect(url.pathname).toBe('/api/category/games');
    });

    test('includes page, rows, sortBy, sortOrder params', () => {
        const url = new DataService(makeConfig())
            .buildUrl(makeState({ currentPage: 3, rowsPerPage: 10, sortBy: 'score', sortOrder: 'desc' }));
        expect(url.searchParams.get('page')).toBe('3');
        expect(url.searchParams.get('rows')).toBe('10');
        expect(url.searchParams.get('sortBy')).toBe('score');
        expect(url.searchParams.get('sortOrder')).toBe('desc');
    });

    test('does NOT add filter param when filterId is "all"', () => {
        const url = new DataService(makeConfig()).buildUrl(makeState({ filterId: 'all' }));
        expect(url.searchParams.has('tag_id')).toBe(false);
    });

    test('adds filter param when filterId is not "all"', () => {
        const url = new DataService(makeConfig()).buildUrl(makeState({ filterId: '5' }));
        expect(url.searchParams.get('tag_id')).toBe('5');
    });

    test('uses config.filterParamName for the filter key (genre_id)', () => {
        const url = new DataService(makeConfig({ filterParamName: 'genre_id' }))
            .buildUrl(makeState({ filterId: '3' }));
        expect(url.searchParams.get('genre_id')).toBe('3');
        expect(url.searchParams.has('tag_id')).toBe(false);
    });

    test('adds search param when searchTerm is not empty', () => {
        const url = new DataService(makeConfig()).buildUrl(makeState({ searchTerm: 'witcher' }));
        expect(url.searchParams.get('search')).toBe('witcher');
    });

    test('does NOT add search param when searchTerm is empty', () => {
        const url = new DataService(makeConfig()).buildUrl(makeState({ searchTerm: '' }));
        expect(url.searchParams.has('search')).toBe(false);
    });

    test('combines filter and search together', () => {
        const url = new DataService(makeConfig()).buildUrl(makeState({ filterId: '7', searchTerm: 'rpg' }));
        expect(url.searchParams.get('tag_id')).toBe('7');
        expect(url.searchParams.get('search')).toBe('rpg');
    });
});

// ─── fetchData ────────────────────────────────────────────────────────────────

describe('DataService.fetchData', () => {
    beforeEach(() => { global.fetch = jest.fn(); });
    afterEach(() => { jest.resetAllMocks(); });

    test('returns data.data when response has success:true', async () => {
        const payload = { success: true, data: { items: [{ name: 'Witcher' }], totalPages: 1 } };
        global.fetch.mockResolvedValue({ ok: true, json: async () => payload });
        const data = await new DataService(makeConfig()).fetchData(makeState());
        expect(data).toEqual(payload.data);
    });

    test('returns raw response when no success wrapper', async () => {
        const payload = { items: [{ name: 'Game' }], totalPages: 2 };
        global.fetch.mockResolvedValue({ ok: true, json: async () => payload });
        const data = await new DataService(makeConfig()).fetchData(makeState());
        expect(data).toEqual(payload);
    });

    test('throws when response is not ok', async () => {
        global.fetch.mockResolvedValue({ ok: false, status: 500 });
        await expect(new DataService(makeConfig()).fetchData(makeState()))
            .rejects.toThrow('Failed to fetch data');
    });

    test('sends AJAX header X-Requested-With: XMLHttpRequest', async () => {
        global.fetch.mockResolvedValue({ ok: true, json: async () => ({ items: [] }) });
        await new DataService(makeConfig()).fetchData(makeState());
        expect(global.fetch).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({
                headers: expect.objectContaining({ 'X-Requested-With': 'XMLHttpRequest' })
            })
        );
    });

    test('returns null when request is aborted (AbortError)', async () => {
        const abortError = new DOMException('Aborted', 'AbortError');
        global.fetch.mockRejectedValue(abortError);
        const data = await new DataService(makeConfig()).fetchData(makeState());
        expect(data).toBeNull();
    });


    test('aborts previous AbortController when a new request starts', () => {
        // fetchData returns a never-resolving promise so the first call stays in-flight
        global.fetch.mockImplementation(() => new Promise(() => {}));

        const svc = new DataService(makeConfig());
        svc.fetchData(makeState());

        const firstController = svc.abortController;
        const abortSpy = jest.spyOn(firstController, 'abort');

        svc.fetchData(makeState()); // second call should abort the first

        expect(abortSpy).toHaveBeenCalled();
    });

    test('creates a fresh AbortController for each request', () => {
        global.fetch.mockImplementation(() => new Promise(() => {}));
        const svc = new DataService(makeConfig());
        svc.fetchData(makeState());
        const ctrl1 = svc.abortController;
        svc.fetchData(makeState());
        const ctrl2 = svc.abortController;
        expect(ctrl1).not.toBe(ctrl2);
    });
});

// ─── postData ─────────────────────────────────────────────────────────────────

describe('DataService.postData', () => {
    beforeEach(() => { global.fetch = jest.fn(); });
    afterEach(() => { jest.resetAllMocks(); });

    test('sends POST with application/x-www-form-urlencoded', async () => {
        global.fetch.mockResolvedValue({ ok: true, json: async () => ({ success: true }) });
        await new DataService(makeConfig()).postData('/api/games', { name: 'Test', score: 80 });
        expect(global.fetch).toHaveBeenCalledWith(
            '/api/games',
            expect.objectContaining({
                method: 'POST',
                headers: expect.objectContaining({
                    'Content-Type': 'application/x-www-form-urlencoded'
                })
            })
        );
    });

    test('returns parsed JSON on success', async () => {
        const payload = { id: 42, name: 'Test' };
        global.fetch.mockResolvedValue({ ok: true, json: async () => payload });
        const result = await new DataService(makeConfig()).postData('/api/games', { name: 'Test' });
        expect(result).toEqual(payload);
    });

    test('throws on non-ok response', async () => {
        global.fetch.mockResolvedValue({ ok: false, status: 400 });
        await expect(new DataService(makeConfig()).postData('/api/games', {}))
            .rejects.toThrow('Failed to post data');
    });
});