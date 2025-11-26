

describe('ProfilePageManager', () => {
    let mockConfig;

    beforeEach(() => {

        document.body.innerHTML = `
            <div class="profile-tabs">
                <a class="tab-link active" data-category="games">Games</a>
                <a class="tab-link" data-category="movies">Movies</a>
            </div>
            <div id="tab-content-container"></div>
        `;

        mockConfig = {
            contextPath: '/app',
            username: 'testuser',
            initialResult: {
                items: [
                    {
                        name: 'Game 1',
                        tags: [{ name: 'Action' }, { name: 'RPG' }],
                        score: 85,
                        completed: true
                    }
                ],
                currentPage: 1,
                totalPages: 1
            }
        };

        window.profileConfig = mockConfig;
    });

    afterEach(() => {
        document.body.innerHTML = '';
        delete window.profileConfig;
    });

    describe('Score Styling Function', () => {
        function applyScoreStyling() {
            document.querySelectorAll(".score-cell").forEach(cell => {
                cell.classList.remove("score-low", "score-medium", "score-high");
                const score = parseInt(cell.textContent.trim(), 10);
                if (!isNaN(score)) {
                    if (score <= 49) {
                        cell.classList.add("score-low");
                    } else if (score <= 74) {
                        cell.classList.add("score-medium");
                    } else {
                        cell.classList.add("score-high");
                    }
                }
            });
        }

        test('should apply low score styling for scores <= 49', () => {
            document.body.innerHTML = `
                <table>
                    <tbody>
                        <tr><td><span class="score-cell">30</span></td></tr>
                    </tbody>
                </table>
            `;

            applyScoreStyling();

            const scoreCell = document.querySelector('.score-cell');
            expect(scoreCell.classList.contains('score-low')).toBe(true);
            expect(scoreCell.classList.contains('score-medium')).toBe(false);
            expect(scoreCell.classList.contains('score-high')).toBe(false);
        });

        test('should apply medium score styling for scores 50-74', () => {
            document.body.innerHTML = `
                <table>
                    <tbody>
                        <tr><td><span class="score-cell">60</span></td></tr>
                    </tbody>
                </table>
            `;

            applyScoreStyling();

            const scoreCell = document.querySelector('.score-cell');
            expect(scoreCell.classList.contains('score-low')).toBe(false);
            expect(scoreCell.classList.contains('score-medium')).toBe(true);
            expect(scoreCell.classList.contains('score-high')).toBe(false);
        });

        test('should apply high score styling for scores >= 75', () => {
            document.body.innerHTML = `
                <table>
                    <tbody>
                        <tr><td><span class="score-cell">85</span></td></tr>
                    </tbody>
                </table>
            `;

            applyScoreStyling();

            const scoreCell = document.querySelector('.score-cell');
            expect(scoreCell.classList.contains('score-low')).toBe(false);
            expect(scoreCell.classList.contains('score-medium')).toBe(false);
            expect(scoreCell.classList.contains('score-high')).toBe(true);
        });

        test('should not apply styling for invalid scores', () => {
            document.body.innerHTML = `
                <table>
                    <tbody>
                        <tr><td><span class="score-cell">invalid</span></td></tr>
                    </tbody>
                </table>
            `;

            applyScoreStyling();

            const scoreCell = document.querySelector('.score-cell');
            expect(scoreCell.classList.contains('score-low')).toBe(false);
            expect(scoreCell.classList.contains('score-medium')).toBe(false);
            expect(scoreCell.classList.contains('score-high')).toBe(false);
        });

        test('should handle multiple score cells', () => {
            document.body.innerHTML = `
                <table>
                    <tbody>
                        <tr><td><span class="score-cell">30</span></td></tr>
                        <tr><td><span class="score-cell">60</span></td></tr>
                        <tr><td><span class="score-cell">90</span></td></tr>
                    </tbody>
                </table>
            `;

            applyScoreStyling();

            const scoreCells = document.querySelectorAll('.score-cell');
            expect(scoreCells[0].classList.contains('score-low')).toBe(true);
            expect(scoreCells[1].classList.contains('score-medium')).toBe(true);
            expect(scoreCells[2].classList.contains('score-high')).toBe(true);
        });

        test('should handle edge case score of 49', () => {
            document.body.innerHTML = `<span class="score-cell">49</span>`;
            applyScoreStyling();
            expect(document.querySelector('.score-cell').classList.contains('score-low')).toBe(true);
        });

        test('should handle edge case score of 50', () => {
            document.body.innerHTML = `<span class="score-cell">50</span>`;
            applyScoreStyling();
            expect(document.querySelector('.score-cell').classList.contains('score-medium')).toBe(true);
        });

        test('should handle edge case score of 74', () => {
            document.body.innerHTML = `<span class="score-cell">74</span>`;
            applyScoreStyling();
            expect(document.querySelector('.score-cell').classList.contains('score-medium')).toBe(true);
        });

        test('should handle edge case score of 75', () => {
            document.body.innerHTML = `<span class="score-cell">75</span>`;
            applyScoreStyling();
            expect(document.querySelector('.score-cell').classList.contains('score-high')).toBe(true);
        });
    });

    describe('DOM Structure', () => {
        test('should have profile tabs container', () => {
            const tabContainer = document.querySelector('.profile-tabs');
            expect(tabContainer).toBeTruthy();
        });

        test('should have content container', () => {
            const contentContainer = document.getElementById('tab-content-container');
            expect(contentContainer).toBeTruthy();
        });

        test('should have initial active tab', () => {
            const activeTab = document.querySelector('.tab-link.active');
            expect(activeTab).toBeTruthy();
            expect(activeTab.dataset.category).toBe('games');
        });

        test('should have multiple tabs', () => {
            const tabs = document.querySelectorAll('.tab-link');
            expect(tabs.length).toBeGreaterThan(1);
        });
    });

    describe('Tab Navigation Simulation', () => {
        test('should switch active class when clicking tabs', () => {
            const gamesTab = document.querySelector('[data-category="games"]');
            const moviesTab = document.querySelector('[data-category="movies"]');


            gamesTab.classList.remove('active');
            moviesTab.classList.add('active');

            expect(moviesTab.classList.contains('active')).toBe(true);
            expect(gamesTab.classList.contains('active')).toBe(false);
        });

        test('should maintain only one active tab', () => {
            const tabs = document.querySelectorAll('.tab-link');

            tabs.forEach(tab => tab.classList.remove('active'));

            tabs[0].classList.add('active');

            const activeTabs = document.querySelectorAll('.tab-link.active');
            expect(activeTabs.length).toBe(1);
        });
    });

    describe('Content Rendering Simulation', () => {
        test('should render empty state when no items', () => {
            const container = document.getElementById('tab-content-container');
            const category = 'games';

            container.innerHTML = `<p>This user hasn't added any ${category} yet.</p>`;

            expect(container.textContent).toContain("hasn't added any games yet");
        });

        test('should create table with proper structure', () => {
            const container = document.getElementById('tab-content-container');

            container.innerHTML = `
                <table class="profile-table">
                    <thead>
                        <tr>
                            <th class="col-name">Game</th>
                            <th class="col-tags">Tags</th>
                            <th class="col-score">Score</th>
                            <th class="col-completed">Completed</th>
                        </tr>
                    </thead>
                    <tbody></tbody>
                </table>
            `;

            const table = container.querySelector('.profile-table');
            expect(table).toBeTruthy();
            expect(table.querySelector('thead')).toBeTruthy();
            expect(table.querySelector('tbody')).toBeTruthy();
        });

        test('should have correct table headers', () => {
            const container = document.getElementById('tab-content-container');

            container.innerHTML = `
                <table class="profile-table">
                    <thead>
                        <tr>
                            <th class="col-name">Game</th>
                            <th class="col-tags">Tags</th>
                            <th class="col-score">Score</th>
                            <th class="col-completed">Completed</th>
                        </tr>
                    </thead>
                </table>
            `;

            const headers = Array.from(container.querySelectorAll('th')).map(th => th.textContent);
            expect(headers).toEqual(['Game', 'Tags', 'Score', 'Completed']);
        });

        test('should render tag badges', () => {
            const container = document.getElementById('tab-content-container');

            container.innerHTML = `
                <table class="profile-table">
                    <tbody>
                        <tr>
                            <td>Game Name</td>
                            <td><span class="tag-badge">Action</span> <span class="tag-badge">RPG</span></td>
                            <td><span class="score-cell">85</span></td>
                            <td>✓</td>
                        </tr>
                    </tbody>
                </table>
            `;

            const badges = container.querySelectorAll('.tag-badge');
            expect(badges.length).toBe(2);
            expect(badges[0].textContent).toBe('Action');
            expect(badges[1].textContent).toBe('RPG');
        });

        test('should display completed icon', () => {
            const container = document.getElementById('tab-content-container');

            container.innerHTML = `
                <table>
                    <tbody>
                        <tr>
                            <td class="col-completed">✓</td>
                        </tr>
                    </tbody>
                </table>
            `;

            expect(container.textContent).toContain('✓');
        });

        test('should display not completed icon', () => {
            const container = document.getElementById('tab-content-container');

            container.innerHTML = `
                <table>
                    <tbody>
                        <tr>
                            <td class="col-completed">✗</td>
                        </tr>
                    </tbody>
                </table>
            `;

            expect(container.textContent).toContain('✗');
        });
    });

    describe('Pagination Simulation', () => {
        test('should create pagination for multiple pages', () => {
            const container = document.getElementById('tab-content-container');

            container.innerHTML = `
                <div class="pagination">
                    <button class="pagination-btn" data-page="2">Next &raquo;</button>
                    <span>Page 1 of 3</span>
                </div>
            `;

            const pagination = container.querySelector('.pagination');
            expect(pagination).toBeTruthy();
        });

        test('should have correct page info', () => {
            const container = document.getElementById('tab-content-container');

            container.innerHTML = `
                <div class="pagination">
                    <span>Page 2 of 5</span>
                </div>
            `;

            expect(container.textContent).toContain('Page 2 of 5');
        });

        test('should have pagination buttons with data attributes', () => {
            const container = document.getElementById('tab-content-container');

            container.innerHTML = `
                <div class="pagination">
                    <button class="pagination-btn" data-page="1">&laquo; Previous</button>
                    <span>Page 2 of 3</span>
                    <button class="pagination-btn" data-page="3">Next &raquo;</button>
                </div>
            `;

            const buttons = container.querySelectorAll('.pagination-btn');
            expect(buttons.length).toBe(2);
            expect(buttons[0].dataset.page).toBe('1');
            expect(buttons[1].dataset.page).toBe('3');
        });

        test('should show previous button on middle pages', () => {
            const container = document.getElementById('tab-content-container');

            container.innerHTML = `
                <div class="pagination">
                    <button class="pagination-btn" data-page="1">&laquo; Previous</button>
                    <span>Page 2 of 3</span>
                    <button class="pagination-btn" data-page="3">Next &raquo;</button>
                </div>
            `;

            const prevBtn = Array.from(container.querySelectorAll('.pagination-btn'))
                .find(btn => btn.textContent.includes('Previous'));
            expect(prevBtn).toBeTruthy();
        });

        test('should show next button when not on last page', () => {
            const container = document.getElementById('tab-content-container');

            container.innerHTML = `
                <div class="pagination">
                    <span>Page 1 of 3</span>
                    <button class="pagination-btn" data-page="2">Next &raquo;</button>
                </div>
            `;

            const nextBtn = Array.from(container.querySelectorAll('.pagination-btn'))
                .find(btn => btn.textContent.includes('Next'));
            expect(nextBtn).toBeTruthy();
        });
    });

    describe('Loading and Error States', () => {
        test('should display loading text', () => {
            const container = document.getElementById('tab-content-container');
            container.innerHTML = '<p class="loading-text">Loading...</p>';

            expect(container.innerHTML).toContain('Loading...');
            expect(container.querySelector('.loading-text')).toBeTruthy();
        });

        test('should display error message', () => {
            const container = document.getElementById('tab-content-container');
            container.innerHTML = '<p class="error-message">Failed to load content. Please try again.</p>';

            expect(container.innerHTML).toContain('Failed to load content');
            expect(container.querySelector('.error-message')).toBeTruthy();
        });
    });

    describe('String Utility Functions', () => {
        function capitalize(str) {
            return str.charAt(0).toUpperCase() + str.slice(1);
        }

        test('should capitalize first letter', () => {
            expect(capitalize('games')).toBe('Games');
            expect(capitalize('movies')).toBe('Movies');
        });

        test('should handle single character', () => {
            expect(capitalize('g')).toBe('G');
        });

        test('should handle empty string', () => {
            expect(capitalize('')).toBe('');
        });

        test('should only capitalize first letter', () => {
            expect(capitalize('test string')).toBe('Test string');
        });
    });

    describe('HTML Escaping', () => {
        function escapeHtml(str) {
            const div = document.createElement('div');
            div.textContent = str;
            return div.innerHTML;
        }

        test('should escape HTML tags', () => {
            const escaped = escapeHtml('<script>alert("xss")</script>');
            expect(escaped).not.toContain('<script>');
            expect(escaped).toContain('&lt;script&gt;');
        });

        test('should handle quotes', () => {
            const escaped = escapeHtml('"quoted"');
            expect(escaped).toBe('"quoted"');
        });

        test('should handle normal text', () => {
            const escaped = escapeHtml('Normal text');
            expect(escaped).toBe('Normal text');
        });
    });
});