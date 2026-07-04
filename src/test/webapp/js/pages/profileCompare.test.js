const { compatibilityClass, formatDiff, renderCategory, renderComparison } =
    await import('@/pages/profileCompare.js');

// ─── Helpers ──────────────────────────────────────────────────────────────────

const NAMES = { me: 'alice', other: 'bob' };

function makeCategory(overrides = {}) {
    return {
        myCount: 2,
        theirCount: 3,
        commonCount: 1,
        avgDiff: 5.0,
        compatibility: 95.0,
        items: [
            {
                name: 'Celeste',
                myScore: 90,
                theirScore: 85,
                diff: 5,
                myStatus: 'COMPLETED',
                theirStatus: 'IN_PROGRESS',
            },
        ],
        ...overrides,
    };
}

afterEach(() => {
    document.body.innerHTML = '';
});

// ─── compatibilityClass ───────────────────────────────────────────────────────

describe('compatibilityClass', () => {
    test.each([
        [null, 'compat-none'],
        [undefined, 'compat-none'],
        [100, 'compat-high'],
        [85, 'compat-high'],
        [84.9, 'compat-mid'],
        [65, 'compat-mid'],
        [64.9, 'compat-low'],
        [0, 'compat-low'],
    ])('maps %p to %s', (value, expected) => {
        expect(compatibilityClass(value)).toBe(expected);
    });
});

// ─── formatDiff ───────────────────────────────────────────────────────────────

describe('formatDiff', () => {
    test('renders zero as an equals sign', () => {
        expect(formatDiff(0)).toBe('=');
    });

    test('prefixes positive diffs with a plus', () => {
        expect(formatDiff(12)).toBe('+12');
    });

    test('renders negative diffs with a minus sign', () => {
        expect(formatDiff(-7)).toBe('−7');
    });
});

// ─── renderCategory ───────────────────────────────────────────────────────────

describe('renderCategory', () => {
    test('renders a table with both scores, statuses and the diff', () => {
        const html = renderCategory('games', makeCategory(), NAMES);

        expect(html).toContain('Games');
        expect(html).toContain('Celeste');
        expect(html).toContain('✅');
        expect(html).toContain('▶️');
        expect(html).toContain('+5');
        expect(html).toContain('95%');
        expect(html).toContain('alice');
        expect(html).toContain('bob');
    });

    test('shows an empty message when there is no overlap', () => {
        const html = renderCategory(
            'books',
            makeCategory({ commonCount: 0, items: [], compatibility: null }),
            NAMES
        );

        expect(html).toContain('No items in common');
        expect(html).not.toContain('<table');
    });

    test('escapes HTML in item names', () => {
        const category = makeCategory();
        category.items[0].name = '<img src=x onerror=alert(1)>';

        const html = renderCategory('games', category, NAMES);

        expect(html).not.toContain('<img src=x');
        expect(html).toContain('&lt;img');
    });

    test('marks negative diffs with the diff-neg class', () => {
        const category = makeCategory();
        category.items[0].diff = -10;

        const html = renderCategory('games', category, NAMES);

        expect(html).toContain('diff-neg');
        expect(html).toContain('−10');
    });
});

// ─── renderComparison ─────────────────────────────────────────────────────────

describe('renderComparison', () => {
    function makeData(overrides = {}) {
        return {
            me: 'alice',
            other: 'bob',
            commonCount: 1,
            compatibility: 95.0,
            categories: {
                games: makeCategory(),
                movies: makeCategory({ commonCount: 0, items: [], compatibility: null }),
                books: makeCategory({ commonCount: 0, items: [], compatibility: null }),
                shows: makeCategory({ commonCount: 0, items: [], compatibility: null }),
            },
            ...overrides,
        };
    }

    test('renders the overall badge and every category', () => {
        const html = renderComparison(makeData());

        expect(html).toContain('Taste compatibility with bob');
        expect(html).toContain('compat-badge--big');
        expect(html).toContain('95%');
        expect(html).toContain('Games');
        expect(html).toContain('Movies');
        expect(html).toContain('Books');
        expect(html).toContain('Shows');
    });

    test('renders a friendly message when nothing is shared', () => {
        const html = renderComparison(makeData({ commonCount: 0, compatibility: null }));

        expect(html).toContain('no rated items in common');
        expect(html).not.toContain('compat-badge--big');
    });

    test('escapes the other username', () => {
        const html = renderComparison(makeData({ other: '<b>bob</b>' }));

        expect(html).toContain('&lt;b&gt;bob&lt;/b&gt;');
        expect(html).not.toContain('<b>bob</b>');
    });

    test('pluralizes the shared item count', () => {
        expect(renderComparison(makeData())).toContain('1 shared item.');
        expect(renderComparison(makeData({ commonCount: 4 }))).toContain('4 shared items.');
    });
});
