import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';
import { UniversalAutocomplete } from '@/tableScripts/features/forms/categoryAutocomplete.js';

describe('UniversalAutocomplete', () => {
    let input;
    let autocomplete;

    beforeEach(() => {
        jest.useFakeTimers();
        document.body.innerHTML = `
            <input type="text" id="nameInput" />
            <input type="url"  id="coverUrlInput" />
        `;
        input = document.getElementById('nameInput');
        global.fetch = jest.fn();
    });

    afterEach(() => {
        document.body.innerHTML = '';
        jest.useRealTimers();
    });

    describe('Constructor', () => {
        test('should store input and entityType', () => {
            const ac = new UniversalAutocomplete(input, 'games');
            expect(ac.input).toBe(input);
            expect(ac.entityType).toBe('games');
        });

        test('should use default options when not provided', () => {
            const ac = new UniversalAutocomplete(input, 'games');
            expect(ac.options.minChars).toBe(2);
            expect(ac.options.debounceMs).toBe(300);
            expect(ac.options.maxResults).toBe(10);
        });

        test('should override options when provided', () => {
            const ac = new UniversalAutocomplete(input, 'games', { minChars: 3, debounceMs: 500 });
            expect(ac.options.minChars).toBe(3);
            expect(ac.options.debounceMs).toBe(500);
        });

        test('should support games, movies, shows, books entity types', () => {
            ['games', 'movies', 'shows', 'books'].forEach((type) => {
                expect(() => new UniversalAutocomplete(input, type)).not.toThrow();
            });
        });
    });

    describe('Input debouncing', () => {
        test('should not fetch before debounce fires', () => {
            const ac = new UniversalAutocomplete(input, 'games', { debounceMs: 300, minChars: 2 });
            ac.init?.();
            input.value = 'ma';
            input.dispatchEvent(new Event('input'));
            expect(global.fetch).not.toHaveBeenCalled();
        });

        test('should not search when input is shorter than minChars', async () => {
            const ac = new UniversalAutocomplete(input, 'games', { minChars: 2, debounceMs: 300 });
            ac.init?.();
            input.value = 'a';
            input.dispatchEvent(new Event('input'));
            jest.advanceTimersByTime(400);
            expect(global.fetch).not.toHaveBeenCalled();
        });
    });

    describe('buildUrl', () => {
        test('should build a URL object for games', () => {
            const ac = new UniversalAutocomplete(input, 'games');
            const url = ac.apiConfigs.games.buildUrl('mario');
            expect(url.searchParams.get('search')).toBe('mario');
        });

        test('should build a URL object for movies', () => {
            const ac = new UniversalAutocomplete(input, 'movies');
            const url = ac.apiConfigs.movies.buildUrl('inception');
            expect(url.searchParams.get('query')).toBe('inception');
        });
    });

    describe('parseResults', () => {
        test('should parse games results to common format', () => {
            const ac = new UniversalAutocomplete(input, 'games');
            const raw = {
                results: [
                    {
                        name: 'Mario',
                        released: '2020-01-01',
                        rating: 9,
                        background_image: 'http://img.jpg',
                    },
                ],
            };
            const parsed = ac.apiConfigs.games.parseResults(raw);
            expect(parsed[0].name).toBe('Mario');
            expect(parsed[0].coverUrl).toBe('http://img.jpg');
        });

        test('should parse movies results to common format', () => {
            const ac = new UniversalAutocomplete(input, 'movies');
            const raw = {
                results: [
                    {
                        title: 'Inception',
                        release_date: '2010-07-16',
                        vote_average: 8.8,
                        poster_path: '/abc.jpg',
                    },
                ],
            };
            const parsed = ac.apiConfigs.movies.parseResults(raw);
            expect(parsed[0].name).toBe('Inception');
            expect(parsed[0].coverUrl).toContain('/abc.jpg');
        });

        test('should handle empty results array', () => {
            const ac = new UniversalAutocomplete(input, 'games');
            const parsed = ac.apiConfigs.games.parseResults({ results: [] });
            expect(parsed).toEqual([]);
        });

        test('should handle missing results key', () => {
            const ac = new UniversalAutocomplete(input, 'games');
            expect(() => ac.apiConfigs.games.parseResults({})).not.toThrow();
        });
    });

    describe('coverUrl input tracking', () => {
        test('should clear lastSelectedCover when user types manually after selection', () => {
            const ac = new UniversalAutocomplete(input, 'games');
            ac.createCoverUrlInput();

            ac.coverUrlInput.value = 'http://example.com/img.jpg';
            ac.lastSelectedCover = 'http://example.com/img.jpg';

            input.value = 'new search';
            input.dispatchEvent(new Event('input'));

            expect(ac.lastSelectedCover).toBeFalsy();
            expect(ac.coverUrlInput.value).toBeFalsy();
        });

        test('lastSelectedCover is set after selecting an autocomplete result', () => {
            const ac = new UniversalAutocomplete(input, 'games');
            ac.createCoverUrlInput();
            const coverUrl = 'http://example.com/cover.jpg';

            ac.results = [{ name: 'Mario', coverUrl }];
            ac.selectItem(0);

            expect(ac.lastSelectedCover).toBe(coverUrl);
        });
    });
});
