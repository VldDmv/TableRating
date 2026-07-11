import { test, expect } from '@playwright/test';

/**
 * Browser E2E: real Chromium against the real app (e2e profile, in-memory
 * H2). Covers the flows the Jest unit tests cannot see end to end —
 * registration, adding items, search filtering and inline score editing.
 */

const unique = (prefix) => `${prefix}${Date.now().toString(36)}${Math.floor(Math.random() * 1000)}`;

async function registerAndLogin(page) {
    const username = unique('e2e');
    await page.goto('/index');
    await page.click('#show-register');
    await page.fill('#register-name', username);
    await page.fill('#register-password', 'password123');
    await page.fill('#register-confirm-password', 'password123');
    await page.click('#register-button');
    await page.waitForURL('**/dashboard');
    return username;
}

async function addGame(page, name, score) {
    // the add form is collapsed behind the "+ Add Game" toggle
    if (!(await page.locator('#itemName').isVisible())) {
        await page.getByRole('button', { name: /Add Game/ }).click();
    }
    await page.fill('#itemName', name);
    await page.fill('#gameScore', String(score));
    // Submit with Enter instead of clicking the button: typing a real game
    // name opens the autocomplete dropdown (live Steam suggestions on CI),
    // which can cover the submit button and swallow the click.
    await page.locator('#gameScore').press('Enter');
    await expect(page.locator('#gamesBody tr').filter({ hasText: name })).toBeVisible();
}

test('registration logs the user in and lands on the dashboard', async ({ page }) => {
    await registerAndLogin(page);
    await expect(page).toHaveURL(/\/dashboard$/);
});

test('a game added through the form appears in the table', async ({ page }) => {
    await registerAndLogin(page);
    await page.goto('/games');

    await addGame(page, 'Hollow Knight', 95);

    const row = page.locator('#gamesBody tr').filter({ hasText: 'Hollow Knight' });
    await expect(row.locator('.score-cell')).toHaveText('95');
});

test('search box filters the table down to matching rows', async ({ page }) => {
    await registerAndLogin(page);
    await page.goto('/games');
    await addGame(page, 'Celeste', 90);
    await addGame(page, 'Stardew Valley', 85);

    await page.fill('#searchBox', 'Celeste');

    // search is debounced — poll until only the matching row remains
    await expect(page.locator('#gamesBody tr')).toHaveCount(1);
    await expect(page.locator('#gamesBody tr')).toContainText('Celeste');
});

test('profile comparison shows taste compatibility between two users', async ({
    page,
    browser,
    baseURL,
}) => {
    // User A rates two games and makes their profile public.
    const userA = await registerAndLogin(page);
    await page.goto('/games');
    await addGame(page, 'Celeste', 90);
    await addGame(page, 'Hades', 80);
    await page.goto(`/profile?username=${userA}`);
    await page.check('#privacy-checkbox'); // submits the privacy form
    await page.waitForURL('**/profile**');

    // User B (separate session) rates one game they share with A.
    const contextB = await browser.newContext({ baseURL });
    const pageB = await contextB.newPage();
    await registerAndLogin(pageB);
    await pageB.goto('/games');
    await addGame(pageB, 'Celeste', 70);

    // B opens A's profile and compares: one common item, |70-90| = 20 -> 80%.
    await pageB.goto(`/profile?username=${userA}`);
    await pageB.click('#compareBtn');

    const section = pageB.locator('#compare-section');
    await expect(section.locator('.compat-badge--big')).toHaveText(/80%/);
    await expect(section).toContainText('Celeste');
    await expect(section.locator('.compare-diff').first()).toHaveText('−20');

    await contextB.close();
});

test('inline score edit persists across a reload', async ({ page }) => {
    await registerAndLogin(page);
    await page.goto('/games');
    await addGame(page, 'Outer Wilds', 80);

    // Locate the row by the action button's data-item-name: in edit mode the
    // name cell becomes an <input>, so a hasText filter would stop matching.
    const row = page
        .locator('#gamesBody tr')
        .filter({ has: page.locator('.edit-button[data-item-name="Outer Wilds"]') });

    await row.locator('.edit-button').click();
    await expect(row).toHaveClass(/is-editing/);
    await row.locator('input.edit-score-input').fill('99');
    await row.locator('.edit-button').click(); // same button saves

    await expect(row.locator('.score-cell')).toHaveText('99');

    await page.reload();
    await expect(row.locator('.score-cell')).toHaveText('99');
});
