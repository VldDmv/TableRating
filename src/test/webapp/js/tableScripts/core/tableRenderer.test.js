import { describe, test, expect, jest, beforeEach, afterEach } from '@jest/globals';
import { TableRenderer } from '../../../../../main/webapp/js/tableScripts/core/tableRenderer.js';

describe('TableRenderer', () => {
    let tableRenderer;
    let mockTableBody;
    let mockConfig;

    beforeEach(() => {
        mockTableBody = document.createElement('tbody');


        const meta = document.createElement('meta');
        meta.name = '_csrf_token';
        meta.content = 'test-token';
        document.head.appendChild(meta);

        mockConfig = {
            entityType: 'games',
            entityNameSingular: 'Game',
            paramNames: {
                removeItem: 'removeGame'
            },
            applyScoreStyling: jest.fn()
        };

        tableRenderer = new TableRenderer(mockConfig, mockTableBody);
    });

    afterEach(() => {
        document.head.innerHTML = '';
    });

    describe('Constructor', () => {
        test('should initialize with config and tableBody', () => {
            expect(tableRenderer.config).toBe(mockConfig);
            expect(tableRenderer.tableBody).toBe(mockTableBody);
        });
    });

    describe('render', () => {
        test('should render items correctly', () => {
            const items = [
                { name: 'Test Game 1', score: 85, tags: [], completed: true },
                { name: 'Test Game 2', score: 70, tags: [], completed: false }
            ];

            tableRenderer.render(items);

            expect(mockTableBody.children.length).toBe(2);
            expect(mockTableBody.textContent).toContain('Test Game 1');
            expect(mockTableBody.textContent).toContain('Test Game 2');
        });

        test('should render empty state when no items', () => {
            tableRenderer.render([]);

            expect(mockTableBody.textContent).toContain('No items found');
        });

        test('should render empty state when items is null', () => {
            tableRenderer.render(null);

            expect(mockTableBody.textContent).toContain('No items found');
        });

        test('should render empty state when items is undefined', () => {
            tableRenderer.render(undefined);

            expect(mockTableBody.textContent).toContain('No items found');
        });

        test('should call applyScoreStyling if configured', () => {
            const items = [
                { name: 'Test', score: 85, tags: [], completed: true }
            ];

            tableRenderer.render(items);

            expect(mockConfig.applyScoreStyling).toHaveBeenCalledWith(mockTableBody);
        });

        test('should not call applyScoreStyling if not configured', () => {
            mockConfig.applyScoreStyling = undefined;
            const items = [
                { name: 'Test', score: 85, tags: [], completed: true }
            ];

            expect(() => {
                tableRenderer.render(items);
            }).not.toThrow();
        });

        test('should clear existing content before rendering', () => {
            mockTableBody.innerHTML = '<tr><td>Old content</td></tr>';

            const items = [
                { name: 'New Game', score: 85, tags: [], completed: true }
            ];

            tableRenderer.render(items);

            expect(mockTableBody.textContent).not.toContain('Old content');
            expect(mockTableBody.textContent).toContain('New Game');
        });

        test('should handle large number of items', () => {
            const items = Array.from({ length: 100 }, (_, i) => ({
                name: `Game ${i}`,
                score: 50 + i,
                tags: [],
                completed: i % 2 === 0
            }));

            tableRenderer.render(items);

            expect(mockTableBody.children.length).toBe(100);
        });
    });

    describe('renderEmpty', () => {
        test('should display "No items found" message', () => {
            tableRenderer.renderEmpty();

            expect(mockTableBody.textContent).toContain('No items found matching your criteria');
        });

        test('should render single row with colspan', () => {
            tableRenderer.renderEmpty();

            const row = mockTableBody.querySelector('tr');
            const cell = row.querySelector('td');

            expect(cell.getAttribute('colspan')).toBe('5');
        });

        test('should apply centered styling', () => {
            tableRenderer.renderEmpty();

            const cell = mockTableBody.querySelector('td');

            expect(cell.style.textAlign).toBe('center');
            expect(cell.style.padding).toBe('20px');
        });
    });

    describe('renderLoading', () => {
        test('should display "Loading..." message', () => {
            tableRenderer.renderLoading();

            expect(mockTableBody.textContent).toContain('Loading...');
        });

        test('should render single row with colspan', () => {
            tableRenderer.renderLoading();

            const row = mockTableBody.querySelector('tr');
            const cell = row.querySelector('td');

            expect(cell.getAttribute('colspan')).toBe('5');
        });

        test('should apply centered styling', () => {
            tableRenderer.renderLoading();

            const cell = mockTableBody.querySelector('td');

            expect(cell.style.textAlign).toBe('center');
        });
    });

    describe('createRow', () => {
        test('should create row with all columns', () => {
            const item = {
                name: 'Test Game',
                score: 85,
                tags: [],
                completed: true
            };

            const row = tableRenderer.createRow(item);

            expect(row.children.length).toBe(5);
        });

        test('should render item name correctly', () => {
            const item = {
                name: 'Super Mario',
                score: 95,
                tags: [],
                completed: true
            };

            const row = tableRenderer.createRow(item);

            expect(row.querySelector('.col-name').textContent).toBe('Super Mario');
        });

        test('should render score correctly', () => {
            const item = {
                name: 'Test',
                score: 87,
                tags: [],
                completed: false
            };

            const row = tableRenderer.createRow(item);

            expect(row.querySelector('.score-cell').textContent).toBe('87');
        });

        test('should render completed icon correctly', () => {
            const completedItem = {
                name: 'Test',
                score: 85,
                tags: [],
                completed: true
            };

            const row = tableRenderer.createRow(completedItem);

            expect(row.querySelector('.status-button').textContent.trim()).toBe('✅');
        });

        test('should render not completed icon correctly', () => {
            const notCompletedItem = {
                name: 'Test',
                score: 85,
                tags: [],
                completed: false
            };

            const row = tableRenderer.createRow(notCompletedItem);

            expect(row.querySelector('.status-button').textContent.trim()).toBe('❌');
        });

        test('should escape HTML in item name', () => {
            const item = {
                name: '<script>alert("xss")</script>',
                score: 85,
                tags: [],
                completed: false
            };
            const row = tableRenderer.createRow(item);

            expect(row.innerHTML).not.toContain('<script>alert("xss")</script>');

            const nameCell = row.querySelector('.col-name');
            expect(nameCell.innerHTML).toContain('&lt;script&gt;');
            expect(nameCell.innerHTML).toContain('&lt;/script&gt;');
            expect(nameCell.textContent).toBe('<script>alert("xss")</script>');

            const statusButton = row.querySelector('.status-button');
            expect(statusButton.dataset.itemName).toBe('<script>alert("xss")</script>');

            expect(row.querySelector('script')).toBeNull();
        });

        test('should render tags correctly', () => {
            const item = {
                name: 'Test',
                score: 85,
                tags: [
                    { id: 1, name: 'Action' },
                    { id: 2, name: 'RPG' }
                ],
                completed: true
            };

            const row = tableRenderer.createRow(item);
            const tagBadges = row.querySelectorAll('.tag-badge');

            expect(tagBadges.length).toBe(2);
            expect(tagBadges[0].textContent).toBe('Action');
            expect(tagBadges[1].textContent).toBe('RPG');
        });

        test('should render genres instead of tags for movies', () => {
            const item = {
                name: 'Test Movie',
                score: 85,
                genres: [
                    { id: 1, name: 'Action' }
                ],
                completed: true
            };

            const row = tableRenderer.createRow(item);
            const tagBadges = row.querySelectorAll('.tag-badge');

            expect(tagBadges.length).toBe(1);
            expect(tagBadges[0].textContent).toBe('Action');
        });

        test('should include CSRF token in delete form', () => {
            const item = {
                name: 'Test',
                score: 85,
                tags: [],
                completed: true
            };

            const row = tableRenderer.createRow(item);
            const csrfInput = row.querySelector('input[name="_csrf"]');

            expect(csrfInput).toBeTruthy();
            expect(csrfInput.value).toBe('test-token');
        });

        test('should set correct data attributes on buttons', () => {
            const item = {
                name: 'Test Game',
                score: 85,
                tags: [],
                completed: true
            };

            const row = tableRenderer.createRow(item);

            const statusButton = row.querySelector('.status-button');
            const editButton = row.querySelector('.btn--edit');
            const deleteButton = row.querySelector('.btn--delete');

            expect(statusButton.dataset.itemName).toBe('Test Game');
            expect(editButton.dataset.itemName).toBe('Test Game');
            expect(deleteButton.dataset.itemName).toBe('Test Game');
        });

        test('should include edit and delete buttons', () => {
            const item = {
                name: 'Test',
                score: 85,
                tags: [],
                completed: true
            };

            const row = tableRenderer.createRow(item);

            expect(row.querySelector('.btn--edit')).toBeTruthy();
            expect(row.querySelector('.btn--delete')).toBeTruthy();
        });

        test('should render edit icon', () => {
            const item = {
                name: 'Test',
                score: 85,
                tags: [],
                completed: true
            };

            const row = tableRenderer.createRow(item);
            const editButton = row.querySelector('.btn--edit');

            expect(editButton.innerHTML).toContain('✏️');
        });

        test('should render delete icon', () => {
            const item = {
                name: 'Test',
                score: 85,
                tags: [],
                completed: true
            };

            const row = tableRenderer.createRow(item);
            const deleteButton = row.querySelector('.btn--delete');

            expect(deleteButton.innerHTML).toContain('🗑️');
        });

        test('should set proper button titles', () => {
            const item = {
                name: 'Test',
                score: 85,
                tags: [],
                completed: true
            };

            const row = tableRenderer.createRow(item);

            expect(row.querySelector('.btn--edit').title).toBe('Edit Game');
            expect(row.querySelector('.btn--delete').title).toBe('Delete Game');
        });
    });

    describe('createTagsHtml', () => {
        test('should create HTML for tags', () => {
            const tags = [
                { id: 1, name: 'Action' },
                { id: 2, name: 'Adventure' }
            ];

            const html = tableRenderer.createTagsHtml(tags);

            expect(html).toContain('Action');
            expect(html).toContain('Adventure');
            expect(html).toContain('tag-badge');
            expect(html).toContain('data-tag-id="1"');
            expect(html).toContain('data-tag-id="2"');
        });

        test('should return empty string for empty tags array', () => {
            const html = tableRenderer.createTagsHtml([]);

            expect(html).toBe('');
        });

        test('should return empty string for null tags', () => {
            const html = tableRenderer.createTagsHtml(null);

            expect(html).toBe('');
        });

        test('should return empty string for undefined tags', () => {
            const html = tableRenderer.createTagsHtml(undefined);

            expect(html).toBe('');
        });

        test('should escape HTML in tag names', () => {
            const tags = [
                { id: 1, name: '<script>alert(1)</script>' }
            ];

            const html = tableRenderer.createTagsHtml(tags);

            expect(html).not.toContain('<script>');
            expect(html).toContain('&lt;script&gt;');
        });

        test('should handle special characters in tag names', () => {
            const tags = [
                { id: 1, name: 'Action & Adventure' },
                { id: 2, name: 'Sci-Fi > Fantasy' }
            ];

            const html = tableRenderer.createTagsHtml(tags);

            expect(html).toContain('&amp;');
            expect(html).toContain('&gt;');
        });

        test('should join tags with spaces', () => {
            const tags = [
                { id: 1, name: 'Tag1' },
                { id: 2, name: 'Tag2' }
            ];

            const html = tableRenderer.createTagsHtml(tags);

            expect(html).toMatch(/Tag1.*Tag2/);
        });
    });

    describe('Integration Tests', () => {
        test('should render complete table with multiple items', () => {
            const items = [
                {
                    name: 'Game 1',
                    score: 95,
                    tags: [{ id: 1, name: 'Action' }],
                    completed: true
                },
                {
                    name: 'Game 2',
                    score: 70,
                    tags: [{ id: 2, name: 'RPG' }],
                    completed: false
                },
                {
                    name: 'Game 3',
                    score: 85,
                    tags: [],
                    completed: true
                }
            ];

            tableRenderer.render(items);

            expect(mockTableBody.children.length).toBe(3);
            expect(mockTableBody.querySelectorAll('.status-button').length).toBe(3);
            expect(mockTableBody.querySelectorAll('.btn--edit').length).toBe(3);
            expect(mockTableBody.querySelectorAll('.btn--delete').length).toBe(3);
        });

        test('should handle transition from loading to content', () => {
            tableRenderer.renderLoading();
            expect(mockTableBody.textContent).toContain('Loading...');

            const items = [
                { name: 'Test', score: 85, tags: [], completed: true }
            ];

            tableRenderer.render(items);
            expect(mockTableBody.textContent).not.toContain('Loading...');
            expect(mockTableBody.textContent).toContain('Test');
        });

        test('should handle transition from content to empty', () => {
            const items = [
                { name: 'Test', score: 85, tags: [], completed: true }
            ];

            tableRenderer.render(items);
            expect(mockTableBody.children.length).toBe(1);

            tableRenderer.render([]);
            expect(mockTableBody.textContent).toContain('No items found');
        });
    });

    describe('Edge Cases', () => {
        test('should handle items with missing properties', () => {
            const items = [
                { name: 'Test', score: 85 }
            ];

            expect(() => {
                tableRenderer.render(items);
            }).not.toThrow();
        });

        test('should handle very long item names', () => {
            const item = {
                name: 'A'.repeat(1000),
                score: 85,
                tags: [],
                completed: true
            };

            const row = tableRenderer.createRow(item);

            expect(row.querySelector('.col-name').textContent.length).toBe(1000);
        });

        test('should handle score of 0', () => {
            const item = {
                name: 'Test',
                score: 0,
                tags: [],
                completed: true
            };

            const row = tableRenderer.createRow(item);

            expect(row.querySelector('.score-cell').textContent).toBe('0');
        });

        test('should handle very high scores', () => {
            const item = {
                name: 'Test',
                score: 999999,
                tags: [],
                completed: true
            };

            const row = tableRenderer.createRow(item);

            expect(row.querySelector('.score-cell').textContent).toBe('999999');
        });

        test('should handle unicode characters in item names', () => {
            const item = {
                name: '测试游戏 🎮',
                score: 85,
                tags: [],
                completed: true
            };

            const row = tableRenderer.createRow(item);

            expect(row.querySelector('.col-name').textContent).toBe('测试游戏 🎮');
        });
    });
});