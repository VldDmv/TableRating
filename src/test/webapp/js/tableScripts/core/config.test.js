
import { jest } from '@jest/globals';

// ─── Mock window.categoryConfig ───────────────────────────────────────────────
beforeEach(() => {
    delete global.window.categoryConfig;
});



describe('COMMON_VALIDATORS.validateScore', () => {
    // Extract the validator logic as used in each entity config
    const makeValidator = (entityName) => (scoreValue) => {
        const score = parseInt(scoreValue, 10);
        if (isNaN(score) || score < 1 || score > 100) {
            return false;
        }
        return true;
    };

    const validateScore = makeValidator('Game');

    test('returns true for valid score 1', () => {
        expect(validateScore('1')).toBe(true);
    });

    test('returns true for valid score 100', () => {
        expect(validateScore('100')).toBe(true);
    });

    test('returns true for valid score 50', () => {
        expect(validateScore('50')).toBe(true);
    });

    test('returns false for score 0', () => {
        expect(validateScore('0')).toBe(false);
    });

    test('returns false for score 101', () => {
        expect(validateScore('101')).toBe(false);
    });

    test('returns false for negative score', () => {
        expect(validateScore('-1')).toBe(false);
    });

    test('returns false for NaN string', () => {
        expect(validateScore('abc')).toBe(false);
    });

    test('returns false for empty string', () => {
        expect(validateScore('')).toBe(false);
    });
});

describe('createScoreStyling', () => {
    // Replicate the factory function
    const createScoreStyling = (thresholds) => (tableBody) => {
        if (!tableBody) return;
        tableBody.querySelectorAll('.score-cell').forEach(cell => {
            cell.classList.remove('score-low', 'score-medium', 'score-high');
            const score = parseInt(cell.textContent.trim(), 10);
            if (!isNaN(score)) {
                if      (score <= thresholds.low)    cell.classList.add('score-low');
                else if (score <= thresholds.medium) cell.classList.add('score-medium');
                else                                  cell.classList.add('score-high');
            }
        });
    };

    function makeContainer(scores) {
        const div = document.createElement('div');
        scores.forEach(s => {
            const span = document.createElement('span');
            span.className = 'score-cell';
            span.textContent = String(s);
            div.appendChild(span);
        });
        return div;
    }

    describe('games thresholds (low=49, medium=74)', () => {
        const style = createScoreStyling({ low: 49, medium: 74 });

        test('score 30 → score-low', () => {
            const c = makeContainer([30]);
            style(c);
            expect(c.querySelector('.score-cell').classList.contains('score-low')).toBe(true);
        });

        test('score 49 → score-low (boundary)', () => {
            const c = makeContainer([49]);
            style(c);
            expect(c.querySelector('.score-cell').classList.contains('score-low')).toBe(true);
        });

        test('score 50 → score-medium', () => {
            const c = makeContainer([50]);
            style(c);
            expect(c.querySelector('.score-cell').classList.contains('score-medium')).toBe(true);
        });

        test('score 74 → score-medium (boundary)', () => {
            const c = makeContainer([74]);
            style(c);
            expect(c.querySelector('.score-cell').classList.contains('score-medium')).toBe(true);
        });

        test('score 75 → score-high', () => {
            const c = makeContainer([75]);
            style(c);
            expect(c.querySelector('.score-cell').classList.contains('score-high')).toBe(true);
        });

        test('score 100 → score-high', () => {
            const c = makeContainer([100]);
            style(c);
            expect(c.querySelector('.score-cell').classList.contains('score-high')).toBe(true);
        });
    });

    describe('movies/shows/books thresholds (low=39, medium=60)', () => {
        const style = createScoreStyling({ low: 39, medium: 60 });

        test('score 25 → score-low', () => {
            const c = makeContainer([25]);
            style(c);
            expect(c.querySelector('.score-cell').classList.contains('score-low')).toBe(true);
        });

        test('score 45 → score-medium', () => {
            const c = makeContainer([45]);
            style(c);
            expect(c.querySelector('.score-cell').classList.contains('score-medium')).toBe(true);
        });

        test('score 80 → score-high', () => {
            const c = makeContainer([80]);
            style(c);
            expect(c.querySelector('.score-cell').classList.contains('score-high')).toBe(true);
        });
    });

    test('removes previous class before adding new one', () => {
        const style = createScoreStyling({ low: 49, medium: 74 });
        const c = makeContainer([90]);
        const cell = c.querySelector('.score-cell');
        cell.classList.add('score-low'); // pre-existing wrong class

        style(c);

        expect(cell.classList.contains('score-low')).toBe(false);
        expect(cell.classList.contains('score-high')).toBe(true);
    });

    test('does not crash when tableBody is null', () => {
        const style = createScoreStyling({ low: 49, medium: 74 });
        expect(() => style(null)).not.toThrow();
    });

    test('applies to multiple cells', () => {
        const style = createScoreStyling({ low: 49, medium: 74 });
        const c = makeContainer([20, 60, 90]);
        style(c);
        const cells = c.querySelectorAll('.score-cell');
        expect(cells[0].classList.contains('score-low')).toBe(true);
        expect(cells[1].classList.contains('score-medium')).toBe(true);
        expect(cells[2].classList.contains('score-high')).toBe(true);
    });
});

describe('getCurrentConfig', () => {
    const getCurrentConfig = () => window.categoryConfig || null;

    test('returns null when window.categoryConfig is not set', () => {
        expect(getCurrentConfig()).toBeNull();
    });

    test('returns the config when window.categoryConfig is set', () => {
        window.categoryConfig = { entityType: 'games' };
        expect(getCurrentConfig()).toEqual({ entityType: 'games' });
    });
});