import { test, expect } from '@playwright/test';

const H = (path: string) => `/#${path}`;

test.describe('SkyMetron Desktop', () => {
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

  test('loops page shows mode buttons', async ({ page }) => {
    await page.goto(H('/loops'));
    await expect(page.locator('h2')).toHaveText('Autonomous Loops');
  });
});
