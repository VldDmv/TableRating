/**
 * Dual-handle score range slider (1-100).
 *
 * Wires two overlaid <input type="range"> handles so they can't cross, keeps
 * the numeric label and filled track in sync on every drag, and reports the
 * selected range on release. Values at the extremes (1 / 100) are reported as
 * empty strings so "no filter" sends no query param.
 */
export function initScoreRange(onChange) {
    const minInput = document.querySelector('#minScore');
    const maxInput = document.querySelector('#maxScore');
    if (!minInput || !maxInput) return;

    const minLabel = document.querySelector('#minScoreVal');
    const maxLabel = document.querySelector('#maxScoreVal');
    const fill = document.querySelector('#scoreFill');

    const MIN = 1;
    const MAX = 100;

    function syncVisuals() {
        const lo = Number(minInput.value);
        const hi = Number(maxInput.value);
        if (minLabel) minLabel.textContent = lo;
        if (maxLabel) maxLabel.textContent = hi;
        if (fill) {
            const left = ((lo - MIN) / (MAX - MIN)) * 100;
            const right = ((hi - MIN) / (MAX - MIN)) * 100;
            fill.style.left = `${left}%`;
            fill.style.width = `${right - left}%`;
        }
    }

    function clamp() {
        let lo = Number(minInput.value);
        let hi = Number(maxInput.value);
        if (lo > hi) {
            // Push the handle being dragged back to the other one.
            if (document.activeElement === minInput) {
                lo = hi;
                minInput.value = lo;
            } else {
                hi = lo;
                maxInput.value = hi;
            }
        }
    }

    function report() {
        const lo = Number(minInput.value);
        const hi = Number(maxInput.value);
        onChange({
            minScore: lo === MIN ? '' : String(lo),
            maxScore: hi === MAX ? '' : String(hi),
        });
    }

    [minInput, maxInput].forEach((input) => {
        input.addEventListener('input', () => {
            clamp();
            syncVisuals();
        });
        input.addEventListener('change', report);
    });

    syncVisuals();
}
