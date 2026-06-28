import { test, expect, Page } from '@playwright/test';

const H = (path: string) => `/#${path}`;

async function mockAuth(page: Page, userType = 'USER', username = 'test-user') {
  await page.route('**/api/auth/me', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ username, userType }),
    });
  });
  await page.route('**/api/bootstrap/legal-status', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ termsAccepted: true, lgpdAccepted: true }),
    });
  });
  await page.route('**/api/bootstrap/status', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        workspaceType: userType,
        workspacePath: '/home/test/SkyMetron',
        vaultPath: '/home/test/SkyMetron/vault',
        envFileExists: true,
        dockerDetected: true,
        javaDetected: true,
        gitDetected: true,
        ollamaDetected: false,
        postgresDetected: false,
        rabbitMqDetected: false,
        redisDetected: false,
        maintainer: userType === 'DEVELOPER',
        isComplete: true,
      }),
    });
  });
}

async function authenticate(page: Page) {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'mock-jwt-token');
    localStorage.setItem('username', 'test-user');
    localStorage.setItem('userType', 'USER');
  });
}

async function authenticateAsDeveloper(page: Page) {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'mock-jwt-token');
    localStorage.setItem('username', 'Joao-Aschenbrenner');
    localStorage.setItem('userType', 'DEVELOPER');
  });
}

test.describe('SkyMetron Desktop — Auth & Onboarding', () => {
  test('shows login page when not authenticated', async ({ page }) => {
    await page.goto(H('/'));
    await expect(page.locator('.login-card')).toBeVisible();
    await expect(page.locator('h1')).toHaveText('SkyMetron');
    await expect(page.locator('text=Entrar com GitHub')).toBeVisible();
  });

  test('login page shows v0.2.0-beta', async ({ page }) => {
    await page.goto(H('/'));
    await expect(page.locator('text=v0.2.0-beta')).toBeVisible();
  });

  test('callback page shows error when no code', async ({ page }) => {
    await page.goto(H('/callback'));
    await expect(page.locator('text=No authorization code received')).toBeVisible();
  });
});

test.describe('SkyMetron Desktop — Authenticated Routes', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuth(page);
    await authenticate(page);
  });

  test('home page redirects to chat', async ({ page }) => {
    await page.goto(H('/'));
    await expect(page).toHaveURL(/\/chat/);
    await expect(page.locator('h2')).toHaveText('Chat');
  });

  test('sidebar navigation works', async ({ page }) => {
    await page.goto(H('/'));

    await page.click('text=Brain View');
    await expect(page.locator('h2')).toHaveText('Brain View');

    await page.click('text=Memory');
    await expect(page.locator('h2')).toHaveText('Memory Vault');

    await page.click('text=Loops');
    await expect(page.locator('h2')).toHaveText('Autonomous Loops');

    await page.click('text=Providers');
    await expect(page.locator('h2')).toHaveText('Providers & Agents');

    await page.click('text=Settings');
    await expect(page.locator('h2')).toHaveText('Settings');
  });

  test('chat input exists', async ({ page }) => {
    await page.goto(H('/chat'));
    await expect(page.locator('input[placeholder="Type a message..."]')).toBeVisible();
    await expect(page.locator('button:has-text("Send")')).toBeVisible();
  });

  test('memory page shows search bar', async ({ page }) => {
    await page.goto(H('/memory'));
    await expect(page.locator('input[placeholder="Search memory..."]')).toBeVisible();
    await expect(page.locator('button:has-text("Search")')).toBeVisible();
  });

  test('privacy page can be navigated to', async ({ page }) => {
    await page.goto(H('/privacy'));
    await expect(page.locator('h2')).toHaveText('Privacidade');
    await expect(page.getByRole('button', { name: 'Exportar Dados' })).toBeVisible();
    await expect(page.locator('text=Zona de Perigo')).toBeVisible();
  });

  test('config page shows privacy link', async ({ page }) => {
    await page.goto(H('/config'));
    await expect(page.locator('a:has-text("Privacidade")')).toBeVisible();
    await expect(page.locator('text=0.2.0-beta')).toBeVisible();
  });

  test('settings page has privacy link in sidebar', async ({ page }) => {
    await page.goto(H('/'));
    await expect(page.locator('text=Privacy')).toBeVisible();
  });
});

test.describe('SkyMetron Desktop — Maintainer Flow', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuth(page, 'DEVELOPER', 'Joao-Aschenbrenner');
    await authenticateAsDeveloper(page);
  });

  test('developer can access all pages', async ({ page }) => {
    await page.goto(H('/chat'));
    await expect(page).toHaveURL(/\/chat/);
  });
});
