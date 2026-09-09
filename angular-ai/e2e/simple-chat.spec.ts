import { expect, test } from '@playwright/test';

test.describe('Simple Chat', () => {
  test('sends a message and displays the assistant reply', async ({ page }) => {
    await page.route('**/api/chat', async route => {
      const { message } = JSON.parse(route.request().postData() ?? '{}');
      await route.fulfill({ json: { message: `Echo: ${message}`, isBot: true } });
    });

    await page.goto('/simple-chat');
    await page.getByLabel('Ask anything').fill('Hello there');
    await page.getByRole('button', { name: 'Send' }).click();

    await expect(page.locator('.message-bubble.user')).toHaveText('Hello there');
    await expect(page.locator('.message-bubble').last()).toHaveText('Echo: Hello there');
  });

  test('shows a friendly error message when the request fails', async ({ page }) => {
    await page.route('**/api/chat', route => route.fulfill({ status: 500, json: {} }));

    await page.goto('/simple-chat');
    await page.getByLabel('Ask anything').fill('Hello there');
    await page.getByRole('button', { name: 'Send' }).click();

    await expect(page.locator('.message-bubble').last()).toHaveText(
      'Sorry, I am unable to process your request at the moment.'
    );
  });
});
