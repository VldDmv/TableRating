import { jest, describe, test, expect, beforeEach, afterEach } from '@jest/globals';

// ─── Mock Chart.js ────────────────────────────────────────────────────────────

const chartInstances = [];
const MockChart = jest.fn(function (ctx, config) {
    chartInstances.push({ ctx, config });
});
global.Chart = MockChart;

// ─── Import real Dashboard ────────────────────────────────────────────────────

const { Dashboard } = await import('@/pages/admin/dashboard.js');

// ─── Helpers ──────────────────────────────────────────────────────────────────

function setupStatCards(values = [42, 100]) {
    document.body.innerHTML =
        values
            .map(
                (v) => `
        <div class="dashboard-stat-card">
            <span class="stat-value">${v}</span>
        </div>
    `
            )
            .join('') + '<canvas id="mediaDistributionChart"></canvas>';
}

function setupCanvas() {
    document.body.innerHTML = '<canvas id="mediaDistributionChart"></canvas>';
}

// ─── animateValue ─────────────────────────────────────────────────────────────

describe('Dashboard.animateValue', () => {
    let dashboard;

    beforeEach(() => {
        document.body.innerHTML = '<canvas id="mediaDistributionChart"></canvas>';

        jest.spyOn(window, 'requestAnimationFrame').mockImplementation(() => {});
        jest.spyOn(window, 'performance', 'get').mockReturnValue({ now: () => 0 });
        dashboard = new Dashboard();
    });

    afterEach(() => {
        document.body.innerHTML = '';
        jest.restoreAllMocks();
        chartInstances.length = 0;
        MockChart.mockClear();
    });

    test('sets element to end value immediately when progress=1', () => {
        const el = document.createElement('span');
        el.textContent = '0';

        jest.spyOn(window, 'requestAnimationFrame').mockImplementation((cb) => {
            // progress=1 → elapsed >= duration
            jest.spyOn(performance, 'now').mockReturnValue(1001);
            cb(1001);
        });
        jest.spyOn(performance, 'now').mockReturnValue(0);

        dashboard.animateValue(el, 0, 50, 1000);
        expect(el.textContent).toBe('50');
    });

    test('uses easeOutQuart — at 50% time, value exceeds linear midpoint', () => {
        const el = document.createElement('span');

        jest.spyOn(performance, 'now').mockReturnValue(0);
        jest.spyOn(window, 'requestAnimationFrame').mockImplementationOnce((cb) => {
            jest.spyOn(performance, 'now').mockReturnValue(500);
            cb(500);
        });

        dashboard.animateValue(el, 0, 100, 1000);

        const capturedValue = parseInt(el.textContent, 10);
        expect(capturedValue).toBeGreaterThan(50);
    });

    test('result is always an integer (Math.floor)', () => {
        const el = document.createElement('span');

        jest.spyOn(performance, 'now').mockReturnValue(0);
        jest.spyOn(window, 'requestAnimationFrame').mockImplementationOnce((cb) => {
            jest.spyOn(performance, 'now').mockReturnValue(333);
            cb(333);
        });

        dashboard.animateValue(el, 0, 99, 1000);
        expect(Number.isInteger(parseInt(el.textContent, 10))).toBe(true);
    });

    test('works with start === end (zero range)', () => {
        const el = document.createElement('span');

        jest.spyOn(window, 'requestAnimationFrame').mockImplementation((cb) => {
            jest.spyOn(performance, 'now').mockReturnValue(1001);
            cb(1001);
        });
        jest.spyOn(performance, 'now').mockReturnValue(0);

        dashboard.animateValue(el, 50, 50, 1000);
        expect(el.textContent).toBe('50');
    });
});

// ─── animateStatCards ─────────────────────────────────────────────────────────

describe('Dashboard.animateStatCards', () => {
    beforeEach(() => {
        jest.useFakeTimers();
        jest.spyOn(window, 'requestAnimationFrame').mockImplementation(() => {});
    });

    afterEach(() => {
        document.body.innerHTML = '';
        jest.useRealTimers();
        jest.restoreAllMocks();
        chartInstances.length = 0;
        MockChart.mockClear();
    });

    test('sets opacity to 0 on all cards after stagger timeouts', () => {
        setupStatCards([10, 20]);
        new Dashboard();

        jest.runAllTimers();

        document.querySelectorAll('.dashboard-stat-card').forEach((card) => {
            expect(card.style.opacity).toBe('0');
        });
    });

    test('first card animates at 0ms, second at 100ms', () => {
        setupStatCards([5, 15]);
        new Dashboard();

        jest.advanceTimersByTime(50);
        const cards = document.querySelectorAll('.dashboard-stat-card');
        expect(cards[0].style.opacity).toBe('0');
        expect(cards[1].style.opacity).toBe('');

        jest.advanceTimersByTime(100);
        expect(cards[1].style.opacity).toBe('0');
    });

    test('does not throw when no stat cards in DOM', () => {
        setupCanvas();
        expect(() => new Dashboard()).not.toThrow();
    });

    test('does not throw when stat-value is missing inside card', () => {
        document.body.innerHTML = `
            <div class="dashboard-stat-card"></div>
            <canvas id="mediaDistributionChart"></canvas>
        `;
        expect(() => {
            jest.runAllTimers();
            new Dashboard();
        }).not.toThrow();
    });
});

// ─── createMediaDistributionChart ────────────────────────────────────────────

describe('Dashboard.createMediaDistributionChart', () => {
    beforeEach(() => {
        jest.useFakeTimers();
        jest.spyOn(window, 'requestAnimationFrame').mockImplementation(() => {});
        MockChart.mockClear();
        chartInstances.length = 0;
    });

    afterEach(() => {
        document.body.innerHTML = '';
        delete window.statsData;
        jest.useRealTimers();
        jest.restoreAllMocks();
    });

    test('does not throw and does not create chart when canvas missing', () => {
        document.body.innerHTML = '';
        expect(() => new Dashboard()).not.toThrow();
        expect(MockChart).not.toHaveBeenCalled();
    });

    test('creates Chart when canvas is present', () => {
        setupCanvas();
        new Dashboard();
        expect(MockChart).toHaveBeenCalledTimes(1);
    });

    test('chart type is doughnut', () => {
        setupCanvas();
        new Dashboard();
        expect(chartInstances[0].config.type).toBe('doughnut');
    });

    test('uses window.statsData when available', () => {
        window.statsData = { totalGames: 10, totalMovies: 5, totalBooks: 3, totalShows: 7 };
        setupCanvas();
        new Dashboard();

        const data = chartInstances[0].config.data.datasets[0].data;
        expect(data).toEqual([10, 5, 3, 7]);
    });

    test('falls back to zeros when window.statsData is absent', () => {
        delete window.statsData;
        setupCanvas();
        new Dashboard();

        const data = chartInstances[0].config.data.datasets[0].data;
        expect(data).toEqual([0, 0, 0, 0]);
    });

    test('chart has 4 labels in correct order', () => {
        setupCanvas();
        new Dashboard();

        const labels = chartInstances[0].config.data.labels;
        expect(labels).toHaveLength(4);
        expect(labels[0]).toContain('Games');
        expect(labels[1]).toContain('Movies');
        expect(labels[2]).toContain('Books');
        expect(labels[3]).toContain('Shows');
    });

    test('tooltip callback formats label with percentage', () => {
        setupCanvas();
        new Dashboard();

        const tooltipFn = chartInstances[0].config.options.plugins.tooltip.callbacks.label;
        const result = tooltipFn({
            label: '🎮 Games',
            parsed: 10,
            dataset: { data: [10, 10, 10, 10] },
        });
        expect(result).toBe('🎮 Games: 10 (25.0%)');
    });

    test('tooltip callback handles 100% case', () => {
        setupCanvas();
        new Dashboard();

        const tooltipFn = chartInstances[0].config.options.plugins.tooltip.callbacks.label;
        const result = tooltipFn({
            label: '📚 Books',
            parsed: 7,
            dataset: { data: [0, 0, 7, 0] },
        });
        expect(result).toBe('📚 Books: 7 (100.0%)');
    });
});

// ─── Dashboard stores chart instance ──────────────────────────────────────────

describe('Dashboard.charts', () => {
    beforeEach(() => {
        jest.useFakeTimers();
        jest.spyOn(window, 'requestAnimationFrame').mockImplementation(() => {});
        MockChart.mockClear();
        chartInstances.length = 0;
    });

    afterEach(() => {
        document.body.innerHTML = '';
        jest.useRealTimers();
        jest.restoreAllMocks();
    });

    test('stores chart in this.charts.mediaDistribution', () => {
        setupCanvas();
        const d = new Dashboard();
        expect(d.charts.mediaDistribution).toBeDefined();
    });

    test('chart is not created when canvas absent', () => {
        document.body.innerHTML = '';
        const d = new Dashboard();
        expect(d.charts.mediaDistribution).toBeUndefined();
    });
});
