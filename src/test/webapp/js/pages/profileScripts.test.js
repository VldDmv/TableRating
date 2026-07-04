import { jest } from '@jest/globals';

const {
    getScoreClass,
    getSortIcon,
    escapeHtml,
    buildApiUrl,
    renderTableRow,
    renderCard,
    getEmptyMessage,
    ProfilePageManager,
} = await import('@/pages/profileScripts.js');

// ─── Helpers ──────────────────────────────────────────────────────────────────

function makeItem(overrides = {}) {
    return {
        name: 'Test Game',
        score: 75,
        status: 'PLANNED',
        coverUrl: '',
        tags: [],
        genres: [],
        ...overrides,
    };
}

afterEach(() => {
    document.body.innerHTML = '';
    localStorage.clear();
});

// ─── getScoreClass ────────────────────────────────────────────────────────────

describe('getScoreClass', () => {
    test.each([
        [0, 'score-low'],
        [30, 'score-low'],
        [49, 'score-medium'],
        [50, 'score-medium'],
        [74, 'score-high'],
        [100, 'score-high'],
    ])('score %i → %s', (score, expected) => {
        expect(getScoreClass(score)).toBe(expected);
    });

    test('returns falsy for NaN', () => {
        expect(getScoreClass(NaN)).toBeFalsy();
    });
});

// ─── getSortIcon ──────────────────────────────────────────────────────────────

describe('getSortIcon', () => {
    test('returns empty string when column is not the active sort', () => {
        expect(getSortIcon('score', 'name', 'asc')).toBe('');
    });

    test('returns ▲ for ascending on the active column', () => {
        expect(getSortIcon('name', 'name', 'asc')).toBe('▲');
    });

    test('returns ▼ for descending on the active column', () => {
        expect(getSortIcon('score', 'score', 'desc')).toBe('▼');
    });
});

// ─── escapeHtml ───────────────────────────────────────────────────────────────

describe('escapeHtml', () => {
    test('escapes <script> tags', () => {
        const out = escapeHtml('<script>xss</script>');
        expect(out).not.toContain('<script>');
        expect(out).toContain('&lt;script&gt;');
    });

    test('escapes & to &amp;', () => {
        expect(escapeHtml('cats & dogs')).toBe('cats &amp; dogs');
    });

    test('escapes <img onerror> XSS vector', () => {
        expect(escapeHtml('<img src=x onerror=alert(1)>')).not.toContain('<img');
    });

    test('leaves normal text unchanged', () => {
        expect(escapeHtml('Normal text')).toBe('Normal text');
    });

    test('handles empty string', () => {
        expect(escapeHtml('')).toBe('');
    });
});

// ─── buildApiUrl ──────────────────────────────────────────────────────────────

describe('buildApiUrl', () => {
    function makeUrl(overrides = {}) {
        const base = {
            username: 'alice',
            currentCategory: 'games',
            currentPage: 1,
            rowsPerPage: 10,
            sortBy: 'name',
            sortOrder: 'asc',
            searchTerm: '',
            filterId: 'all',
            contextPath: '',
            ...overrides,
        };

        return new URL(buildApiUrl(base));
    }

    test('pathname is /profile-data', () => {
        expect(makeUrl().pathname).toBe('/profile-data');
    });

    test('includes all required params', () => {
        const url = makeUrl();
        expect(url.searchParams.get('username')).toBe('alice');
        expect(url.searchParams.get('category')).toBe('games');
        expect(url.searchParams.get('page')).toBe('1');
        expect(url.searchParams.get('pageSize')).toBe('10');
        expect(url.searchParams.get('sortBy')).toBe('name');
        expect(url.searchParams.get('sortOrder')).toBe('asc');
    });

    test('adds search param when searchTerm is non-empty', () => {
        expect(makeUrl({ searchTerm: 'witcher' }).searchParams.get('search')).toBe('witcher');
    });

    test('omits search param when searchTerm is empty', () => {
        expect(makeUrl({ searchTerm: '' }).searchParams.has('search')).toBe(false);
    });

    test('adds tag_id for games when filterId is not "all"', () => {
        const url = makeUrl({ filterId: '3' });
        expect(url.searchParams.get('tag_id')).toBe('3');
        expect(url.searchParams.has('genre_id')).toBe(false);
    });

    test('adds genre_id for movies when filterId is not "all"', () => {
        const url = makeUrl({ currentCategory: 'movies', filterId: '5' });
        expect(url.searchParams.get('genre_id')).toBe('5');
        expect(url.searchParams.has('tag_id')).toBe(false);
    });

    test('omits filter params when filterId is "all"', () => {
        const url = makeUrl({ filterId: 'all' });
        expect(url.searchParams.has('tag_id')).toBe(false);
        expect(url.searchParams.has('genre_id')).toBe(false);
    });

    test('prepends contextPath to pathname', () => {
        expect(makeUrl({ contextPath: '/app' }).pathname).toBe('/app/profile-data');
    });
});

// ─── renderTableRow ───────────────────────────────────────────────────────────

describe('renderTableRow', () => {
    // jsdom silently drops a bare <tr> inside a <div>; wrap in <table><tbody>.
    function parse(item, cat = 'games') {
        const table = document.createElement('table');
        table.innerHTML = `<tbody>${renderTableRow(item, cat)}</tbody>`;
        return table.querySelector('tr');
    }

    test('renders name in .col-name', () => {
        expect(parse(makeItem({ name: 'Elden Ring' })).querySelector('.col-name').textContent).toBe(
            'Elden Ring'
        );
    });

    test('score cell has correct value and CSS class', () => {
        const span = parse(makeItem({ score: 90 })).querySelector('.score-cell');
        expect(span.textContent).toBe('90');
        expect(span.classList.contains('score-high')).toBe(true);
    });

    test('renders cover image when coverUrl is set', () => {
        expect(
            parse(makeItem({ coverUrl: 'https://img.jpg' })).querySelector('.cover-thumbnail')
        ).not.toBeNull();
    });

    test('renders cover placeholder when coverUrl is empty', () => {
        expect(parse(makeItem()).querySelector('.cover-placeholder')).not.toBeNull();
    });

    test('shows ✅ for completed items', () => {
        expect(
            parse(makeItem({ status: 'COMPLETED' })).querySelector('.col-completed').textContent
        ).toBe('✅');
    });

    test('shows 📋 for planned items', () => {
        expect(
            parse(makeItem({ status: 'PLANNED' })).querySelector('.col-completed').textContent
        ).toBe('📋');
    });

    test('shows comma-joined tag names for games', () => {
        const tr = parse(makeItem({ tags: [{ name: 'Action' }, { name: 'RPG' }] }), 'games');
        expect(tr.querySelector('.col-tags').textContent).toBe('Action, RPG');
    });

    test('shows comma-joined genre names for movies', () => {
        const tr = parse(makeItem({ genres: [{ name: 'Drama' }, { name: 'Thriller' }] }), 'movies');
        expect(tr.querySelector('.col-tags').textContent).toBe('Drama, Thriller');
    });

    test('shows "-" when tag list is empty', () => {
        expect(parse(makeItem()).querySelector('.col-tags').textContent).toBe('-');
    });

    test('escapes XSS in item name', () => {
        const html = parse(makeItem({ name: '<script>xss</script>' })).innerHTML;
        expect(html).not.toContain('<script>');
        expect(html).toContain('&lt;script&gt;');
    });
});

// ─── renderCard ───────────────────────────────────────────────────────────────

describe('renderCard', () => {
    function parse(item, cat = 'games') {
        const div = document.createElement('div');
        div.innerHTML = renderCard(item, cat);
        return div.querySelector('.media-card');
    }

    test('root element has class media-card', () => {
        expect(parse(makeItem()).classList.contains('media-card')).toBe(true);
    });

    test('.card-title contains item name', () => {
        expect(parse(makeItem({ name: 'Sekiro' })).querySelector('.card-title').textContent).toBe(
            'Sekiro'
        );
    });

    test('score-cell has correct value and CSS class', () => {
        const span = parse(makeItem({ score: 85 })).querySelector('.score-cell');
        expect(span.textContent).toBe('85');
        expect(span.classList.contains('score-high')).toBe(true);
    });

    test('renders cover image when coverUrl is set', () => {
        expect(
            parse(makeItem({ coverUrl: 'https://img.jpg' })).querySelector('.card-cover-image')
        ).not.toBeNull();
    });

    test('renders cover placeholder when coverUrl is empty', () => {
        expect(parse(makeItem()).querySelector('.card-cover-placeholder')).not.toBeNull();
    });

    test('renders tag badges for games', () => {
        const badges = parse(
            makeItem({ tags: [{ name: 'Action' }, { name: 'RPG' }] }),
            'games'
        ).querySelectorAll('.tag-badge');
        expect(badges.length).toBe(2);
        expect(badges[0].textContent).toBe('Action');
    });

    test('renders genre badges for movies', () => {
        expect(
            parse(makeItem({ genres: [{ name: 'Drama' }] }), 'movies').querySelector('.tag-badge')
                .textContent
        ).toBe('Drama');
    });

    test('shows .card-no-tags when no tags', () => {
        expect(parse(makeItem()).querySelector('.card-no-tags')).not.toBeNull();
    });

    test('shows ✅ for completed items', () => {
        expect(
            parse(makeItem({ status: 'COMPLETED' })).querySelector('.card-status').textContent
        ).toContain('✅');
    });

    test('shows 📋 for planned items', () => {
        expect(
            parse(makeItem({ status: 'PLANNED' })).querySelector('.card-status').textContent
        ).toContain('📋');
    });

    test('escapes XSS in item name', () => {
        expect(parse(makeItem({ name: '<img src=x onerror=alert(1)>' })).innerHTML).not.toContain(
            '<img src=x'
        );
    });
});

// ─── getEmptyMessage ──────────────────────────────────────────────────────────

describe('getEmptyMessage', () => {
    test('returns criteria message when search term is active', () => {
        expect(getEmptyMessage('witcher', 'all', 'games')).toBe(
            'No items found matching your criteria.'
        );
    });

    test('returns criteria message when filter is active', () => {
        expect(getEmptyMessage('', '3', 'games')).toBe('No items found matching your criteria.');
    });

    test('returns category message when no search or filter applied', () => {
        expect(getEmptyMessage('', 'all', 'movies')).toBe('No movies found');
    });
});

// ─── ProfilePageManager.switchView ───────────────────────────────────────────

describe('ProfilePageManager.switchView', () => {
    let manager;

    beforeEach(() => {
        document.body.innerHTML = `
            <button id="tableViewBtn"></button>
            <button id="cardsViewBtn"></button>
            <div id="sort-controls" style="display:none"></div>
            <div id="tab-content-container"></div>
            <div id="cards-container" style="display:none"></div>
        `;
        global.fetch = jest.fn().mockResolvedValue({
            ok: true,
            json: async () => ({ items: [], totalPages: 1, totalItems: 0 }),
        });
        manager = new ProfilePageManager({ username: 'alice', category: 'games' });
    });

    test('table mode: tableViewBtn active, cardsViewBtn not', () => {
        manager.switchView('table');
        expect(document.getElementById('tableViewBtn').classList.contains('active')).toBe(true);
        expect(document.getElementById('cardsViewBtn').classList.contains('active')).toBe(false);
    });

    test('cards mode: cardsViewBtn active, tableViewBtn not', () => {
        manager.switchView('cards');
        expect(document.getElementById('cardsViewBtn').classList.contains('active')).toBe(true);
        expect(document.getElementById('tableViewBtn').classList.contains('active')).toBe(false);
    });

    test('sort controls visible only in cards mode', () => {
        manager.switchView('cards');
        expect(document.getElementById('sort-controls').style.display).toBe('flex');
        manager.switchView('table');
        expect(document.getElementById('sort-controls').style.display).toBe('none');
    });

    test('persists view to localStorage keyed by username', () => {
        manager.switchView('cards');
        expect(localStorage.getItem('profile-view-alice')).toBe('cards');
        manager.switchView('table');
        expect(localStorage.getItem('profile-view-alice')).toBe('table');
    });
});

// ─── ProfilePageManager.updatePagination ─────────────────────────────────────

describe('ProfilePageManager.updatePagination', () => {
    let manager;

    beforeEach(() => {
        document.body.innerHTML = `
            <button id="prevPage"></button>
            <button id="nextPage"></button>
            <button id="pageDropdown">Page 1 of 1</button>
            <ul id="pageList"></ul>
            <div id="paginationContainer"></div>
        `;
        global.fetch = jest.fn().mockResolvedValue({
            ok: true,
            json: async () => ({ items: [], totalPages: 1, totalItems: 0 }),
        });
        manager = new ProfilePageManager({ username: 'alice', category: 'games' });
    });

    test('prevPage disabled on page 1', () => {
        manager.updatePagination(1, 5);
        expect(document.getElementById('prevPage').disabled).toBe(true);
    });

    test('nextPage disabled on last page', () => {
        manager.updatePagination(5, 5);
        expect(document.getElementById('nextPage').disabled).toBe(true);
    });

    test('both buttons enabled on a middle page', () => {
        manager.updatePagination(3, 5);
        expect(document.getElementById('prevPage').disabled).toBe(false);
        expect(document.getElementById('nextPage').disabled).toBe(false);
    });

    test('dropdown shows "Page X of Y" text', () => {
        manager.updatePagination(2, 5);
        expect(document.getElementById('pageDropdown').textContent).toBe('Page 2 of 5');
    });

    test('pageList has correct number of items', () => {
        manager.updatePagination(1, 4);
        expect(document.getElementById('pageList').querySelectorAll('li').length).toBe(4);
    });

    test('active page link has active-page class', () => {
        manager.updatePagination(3, 5);
        const links = document.getElementById('pageList').querySelectorAll('a');
        expect(links[2].classList.contains('active-page')).toBe(true);
    });

    test('paginationContainer hidden when only 1 page', () => {
        manager.updatePagination(1, 1);
        expect(document.getElementById('paginationContainer').style.display).toBe('none');
    });

    test('paginationContainer visible when multiple pages', () => {
        manager.updatePagination(1, 3);
        expect(document.getElementById('paginationContainer').style.display).toBe('flex');
    });
});
