/**
 * Profile comparison: fetches /profile-compare for the viewed profile and
 * renders common items, score differences and a taste-compatibility badge.
 * Loaded only for logged-in visitors looking at someone else's profile.
 */

const STATUS_ICONS = { PLANNED: '📋', IN_PROGRESS: '▶️', COMPLETED: '✅', DROPPED: '🚫' };
const CATEGORY_LABELS = { games: 'Games', movies: 'Movies', books: 'Books', shows: 'Shows' };

export { compatibilityClass, formatDiff, renderCategory, renderComparison };

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/** Maps a compatibility percentage to a badge colour class. */
function compatibilityClass(value) {
    if (value == null) return 'compat-none';
    if (value >= 85) return 'compat-high';
    if (value >= 65) return 'compat-mid';
    return 'compat-low';
}

/** Signed score difference: "+12", "−7" or "=". */
function formatDiff(diff) {
    if (diff === 0) return '=';
    return diff > 0 ? `+${diff}` : `−${-diff}`;
}

function statusIcon(status) {
    return STATUS_ICONS[status] || STATUS_ICONS.PLANNED;
}

function renderCategory(key, cat, names) {
    const label = CATEGORY_LABELS[key] || key;
    const counts = `${cat.myCount} vs ${cat.theirCount}`;

    if (cat.commonCount === 0) {
        return `
            <div class="compare-category">
                <h4>${label} <span class="compare-counts">(${counts})</span></h4>
                <p class="compare-empty">No items in common.</p>
            </div>`;
    }

    const rows = cat.items
        .map(
            (item) => `
            <tr>
                <td class="compare-name">${escapeHtml(item.name)}</td>
                <td class="compare-score">
                    <span title="${item.myStatus}">${statusIcon(item.myStatus)}</span>
                    ${item.myScore}
                </td>
                <td class="compare-score">
                    <span title="${item.theirStatus}">${statusIcon(item.theirStatus)}</span>
                    ${item.theirScore}
                </td>
                <td class="compare-diff ${
                    item.diff === 0 ? 'diff-zero' : item.diff > 0 ? 'diff-pos' : 'diff-neg'
                }">${formatDiff(item.diff)}</td>
            </tr>`
        )
        .join('');

    return `
        <div class="compare-category">
            <h4>
                ${label}
                <span class="compare-counts">(${counts}, ${cat.commonCount} in common)</span>
                <span class="compat-badge ${compatibilityClass(cat.compatibility)}">
                    ${cat.compatibility}%
                </span>
            </h4>
            <div class="compare-table-wrap">
                <table class="compare-table">
                    <thead>
                        <tr>
                            <th>Item</th>
                            <th>${escapeHtml(names.me)}</th>
                            <th>${escapeHtml(names.other)}</th>
                            <th>Δ</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
        </div>`;
}

function renderComparison(data) {
    const names = { me: data.me, other: data.other };

    if (data.commonCount === 0) {
        return `
            <div class="compare-summary">
                <h3>You and ${escapeHtml(data.other)} have no rated items in common yet.</h3>
                <p class="compare-empty">Rate some of the same titles to see how your tastes line up.</p>
            </div>`;
    }

    const categories = Object.entries(data.categories)
        .map(([key, cat]) => renderCategory(key, cat, names))
        .join('');

    return `
        <div class="compare-summary">
            <span class="compat-badge compat-badge--big ${compatibilityClass(data.compatibility)}">
                ${data.compatibility}%
            </span>
            <h3>Taste compatibility with ${escapeHtml(data.other)}</h3>
            <p class="compare-empty">Based on ${data.commonCount} shared item${
                data.commonCount === 1 ? '' : 's'
            }. Positive Δ means you rated it higher.</p>
        </div>
        ${categories}`;
}

async function loadComparison(section) {
    const config = window.profileConfig || {};
    const contextPath = config.contextPath || '/';
    const username = config.username || '';

    section.innerHTML = '<p class="compare-loading">Comparing lists…</p>';

    try {
        const response = await fetch(
            `${contextPath}profile-compare?username=${encodeURIComponent(username)}`
        );
        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || `Request failed (${response.status})`);
        }

        section.innerHTML = renderComparison(data);
        section.dataset.loaded = 'true';
    } catch (error) {
        section.innerHTML = `<p class="compare-error">${escapeHtml(error.message)}</p>`;
    }
}

function init() {
    const button = document.getElementById('compareBtn');
    const section = document.getElementById('compare-section');
    if (!button || !section) return;

    button.addEventListener('click', () => {
        const hidden = section.style.display === 'none';
        section.style.display = hidden ? 'block' : 'none';
        if (hidden && section.dataset.loaded !== 'true') {
            loadComparison(section);
        }
    });
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}
