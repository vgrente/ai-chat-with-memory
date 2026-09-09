import { expect, test } from '@playwright/test';

test.describe('Navigation', () => {
  test('moves between Simple Chat and Chat with Memory via the toolbar', async ({ page }) => {
    await page.route('**/api/chat-memory', route => route.fulfill({ json: [] }));

    await page.goto('/');
    await expect(page).toHaveURL(/\/simple-chat$/);
    await expect(page.getByLabel('Ask anything')).toBeVisible();

    await page.getByRole('button', { name: 'Chat with Memory' }).click();
    await expect(page).toHaveURL(/\/chat-memory$/);
    await expect(page.getByText('Welcome to AI Chat with Memory')).toBeVisible();

    await page.getByRole('button', { name: 'Simple Chat' }).click();
    await expect(page).toHaveURL(/\/simple-chat$/);
    await expect(page.getByLabel('Ask anything')).toBeVisible();
  });

  test('redirects unknown routes to Simple Chat', async ({ page }) => {
    await page.goto('/does-not-exist');

    await expect(page).toHaveURL(/\/simple-chat$/);
  });
});
