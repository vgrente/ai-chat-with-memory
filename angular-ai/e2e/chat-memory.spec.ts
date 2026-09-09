import { expect, test } from '@playwright/test';

const CHATS = [
  { id: 'chat-1', description: 'Trip planning' },
  { id: 'chat-2', description: 'Recipe ideas' }
];

const MESSAGES_CHAT_1 = [
  { content: 'What should I pack for Japan?', type: 'USER' },
  { content: 'Bring layers, it varies by season.', type: 'ASSISTANT' }
];

test.describe('Chat with Memory', () => {
  test("loads the chat list and displays a selected chat's history", async ({ page }) => {
    await page.route('**/api/chat-memory', route => route.fulfill({ json: CHATS }));
    await page.route('**/api/chat-memory/chat-1', route => route.fulfill({ json: MESSAGES_CHAT_1 }));

    await page.goto('/chat-memory');

    await expect(page.getByText('Trip planning')).toBeVisible();
    await expect(page.getByText('Recipe ideas')).toBeVisible();

    await page.getByText('Trip planning').click();

    await expect(page.getByText('What should I pack for Japan?')).toBeVisible();
    await expect(page.getByText('Bring layers, it varies by season.')).toBeVisible();
  });

  test('starts a new chat end-to-end and refreshes the sidebar', async ({ page }) => {
    let listCallCount = 0;
    await page.route('**/api/chat-memory', route => {
      if (route.request().method() !== 'GET') {
        return route.continue();
      }
      listCallCount++;
      const chats = listCallCount === 1 ? [] : [{ id: 'chat-new', description: 'New trip' }];
      return route.fulfill({ json: chats });
    });
    await page.route('**/api/chat-memory/start', route =>
      route.fulfill({
        json: { chatId: 'chat-new', message: 'Sure, here is a plan.', description: 'New trip' }
      })
    );
    await page.route('**/api/chat-memory/chat-new', route =>
      route.fulfill({
        json: [
          { content: 'Plan a weekend trip', type: 'USER' },
          { content: 'Sure, here is a plan.', type: 'ASSISTANT' }
        ]
      })
    );

    await page.goto('/chat-memory');
    await expect(page.getByText('No chats available')).toBeVisible();

    await page.getByLabel('Type your message...').fill('Plan a weekend trip');
    await page.getByRole('button', { name: 'Send message' }).click();

    await expect(page.getByText('Plan a weekend trip')).toBeVisible();
    await expect(page.getByText('Sure, here is a plan.')).toBeVisible();
    await expect(page.getByText('New trip')).toBeVisible();
  });
});
