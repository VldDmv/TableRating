/**
 * Renders rating statistics charts on a user's profile and refreshes them
 * whenever the active category tab changes.
 */

const charts = {};
const BUCKET_LABELS = ['1-10', '11-20', '21-30', '31-40', '41-50',
    '51-60', '61-70', '71-80', '81-90', '91-100'];

function destroyChart(key) {
    if (charts[key]) {
        charts[key].destroy();
        charts[key] = null;
    }
}

async function loadStats(category) {
    const username = window.profileConfig?.username;
    if (!username) return;

    try {
        const url = new URL(`${window.location.origin}/profile-stats`);
        url.searchParams.set('username', username);
        url.searchParams.set('category', category);

        const response = await fetch(url);
        if (!response.ok) return;

        const stats = await response.json();
        render(stats);
    } catch (err) {
        console.error('[profileStats]', err);
    }
}

function render(stats) {
    renderSummary(stats);
    renderScoreDistribution(stats.scoreBuckets || []);
    renderCompleted(stats.completed || 0, stats.notCompleted || 0);
    renderCategoryAverages(stats.categoryAverages || []);
}

function renderSummary(stats) {
    const el = document.getElementById('statsSummary');
    if (!el) return;
    const count = stats.count || 0;
    if (count === 0) {
        el.innerHTML = '<span class="stats-empty">No rated items yet.</span>';
        return;
    }
    el.innerHTML = `
        <span class="stat-pill"><strong>${count}</strong> items</span>
        <span class="stat-pill">avg <strong>${stats.avgScore}</strong></span>
        <span class="stat-pill"><strong>${stats.completed}</strong> completed</span>`;
}

function renderScoreDistribution(buckets) {
    const ctx = document.getElementById('scoreDistChart');
    if (!ctx) return;
    destroyChart('scoreDist');
    charts.scoreDist = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: BUCKET_LABELS,
            datasets: [{
                label: 'Items',
                data: buckets,
                backgroundColor: 'rgba(59, 130, 246, 0.7)',
                borderColor: 'rgba(59, 130, 246, 1)',
                borderWidth: 1,
            }],
        },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: { y: { beginAtZero: true, ticks: { precision: 0 } } },
        },
    });
}

function renderCompleted(completed, notCompleted) {
    const ctx = document.getElementById('completedChart');
    if (!ctx) return;
    destroyChart('completed');
    charts.completed = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Completed', 'Not completed'],
            datasets: [{
                data: [completed, notCompleted],
                backgroundColor: ['rgba(34, 197, 94, 0.8)', 'rgba(148, 163, 184, 0.6)'],
                borderWidth: 1,
            }],
        },
        options: {
            responsive: true,
            plugins: { legend: { position: 'bottom' } },
        },
    });
}

function renderCategoryAverages(rows) {
    const ctx = document.getElementById('categoryAvgChart');
    if (!ctx) return;
    destroyChart('categoryAvg');
    charts.categoryAvg = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: rows.map((r) => r.name),
            datasets: [{
                label: 'Average score',
                data: rows.map((r) => r.avg),
                backgroundColor: 'rgba(249, 115, 22, 0.7)',
                borderColor: 'rgba(249, 115, 22, 1)',
                borderWidth: 1,
            }],
        },
        options: {
            indexAxis: 'y',
            responsive: true,
            plugins: { legend: { display: false } },
            scales: { x: { beginAtZero: true, max: 100 } },
        },
    });
}

document.addEventListener('DOMContentLoaded', () => {
    if (typeof Chart === 'undefined') return;
    loadStats(window.profileConfig?.entityType || 'games');
});

document.addEventListener('profile:categoryChanged', (e) => {
    loadStats(e.detail.category);
});
