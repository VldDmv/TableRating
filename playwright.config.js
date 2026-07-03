import { defineConfig, devices } from '@playwright/test';

/**
 * Browser end-to-end tests. The webServer boots the real application on the
 * test classpath with the e2e profile (in-memory H2, see E2eTestApplication),
 * so no MySQL or other external services are needed.
 *
 * Local run:  npm run e2e
 * CI runs the e2e job in .github/workflows/ci.yml.
 *
 * PW_CHROMIUM (optional) points at a system Chromium binary for environments
 * where Playwright's own browser download is unavailable.
 */
export default defineConfig({
    testDir: './src/test/e2e',
    fullyParallel: false,
    workers: 1,
    retries: process.env.CI ? 1 : 0,
    reporter: [['list']],
    timeout: 30_000,

    use: {
        baseURL: 'http://localhost:8080',
        trace: 'retain-on-failure',
        ...(process.env.PW_CHROMIUM
            ? { launchOptions: { executablePath: process.env.PW_CHROMIUM } }
            : {}),
    },

    projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],

    webServer: {
        command: 'mvn -q -B spring-boot:test-run -Dskip.installnodenpm -Dskip.npm',
        url: 'http://localhost:8080/index',
        reuseExistingServer: !process.env.CI,
        timeout: 240_000,
    },
});
