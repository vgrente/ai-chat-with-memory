import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ChatList } from './chat-list';
import { Chat } from '../../chat/chat';
import { ChatMessage } from '../../chat/chat-message';
import MemoryChatService from '../../chat/memory-chat/memory-chat-service';

function mockMemoryChatService(initialChats: Chat[] = []) {
  return {
    selectedChatId: signal<string | undefined>(undefined),
    chatsResource: {
      status: signal<'idle' | 'loading' | 'error' | 'resolved'>('resolved'),
      value: signal<Chat[] | undefined>(initialChats),
      reload: vi.fn()
    },
    chatMessagesResource: { value: signal<ChatMessage[] | undefined>(undefined) },
    startNewChat: vi.fn(),
    continueChat: vi.fn(),
    selectChat: vi.fn(),
    clearSelection: vi.fn()
  };
}

describe('ChatList', () => {
  let fixture: ComponentFixture<ChatList>;
  let component: ChatList;
  let service: ReturnType<typeof mockMemoryChatService>;

  async function setup(chats: Chat[] = []) {
    service = mockMemoryChatService(chats);

    await TestBed.configureTestingModule({
      imports: [ChatList],
      providers: [{ provide: MemoryChatService, useValue: service }]
    }).compileComponents();

    fixture = TestBed.createComponent(ChatList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  }

  it('shows the header', async () => {
    await setup();

    expect(fixture.nativeElement.textContent).toContain('Chat with Memory');
  });

  it('shows a loading indicator while chats are loading', async () => {
    await setup();
    service.chatsResource.status.set('loading');
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('Loading chats...');
  });

  it('shows an error state with a retry button', async () => {
    await setup();
    service.chatsResource.status.set('error');
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('Error loading chats');

    const retryButton: HTMLButtonElement = fixture.nativeElement.querySelector('mat-list-item button');
    retryButton.click();

    expect(service.chatsResource.reload).toHaveBeenCalled();
  });

  it('shows "No chats available" when the resolved list is empty', async () => {
    await setup([]);

    expect(fixture.nativeElement.textContent).toContain('No chats available');
  });

  it('renders each chat with its description', async () => {
    await setup([
      { id: 'chat-1', description: 'Trip planning' },
      { id: 'chat-2', description: 'Recipe ideas' }
    ]);

    const items: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('mat-drawer mat-list-item a'));
    expect(items.map(el => el.textContent?.trim())).toEqual(['Trip planning', 'Recipe ideas']);
  });

  it('falls back to "New Chat" when a chat has no description', async () => {
    await setup([{ id: 'chat-1', description: '' }]);

    const item: HTMLElement = fixture.nativeElement.querySelector('mat-drawer mat-list-item a');
    expect(item.textContent?.trim()).toBe('New Chat');
  });

  it('selects a chat when clicked', async () => {
    await setup([{ id: 'chat-1', description: 'Trip planning' }]);

    const item: HTMLElement = fixture.nativeElement.querySelector('mat-drawer mat-list-item');
    item.click();

    expect(service.selectChat).toHaveBeenCalledWith('chat-1');
  });

  it('marks the selected chat with the "selected" class', async () => {
    await setup([
      { id: 'chat-1', description: 'Trip planning' },
      { id: 'chat-2', description: 'Recipe ideas' }
    ]);
    service.selectedChatId.set('chat-2');
    await fixture.whenStable();

    const items: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('mat-drawer mat-list-item'));
    expect(items[0].classList.contains('selected')).toBe(false);
    expect(items[1].classList.contains('selected')).toBe(true);
  });

  it('deletes a chat without selecting it', async () => {
    await setup([{ id: 'chat-1', description: 'Trip planning' }]);
    const logSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

    const deleteButton: HTMLButtonElement = fixture.nativeElement.querySelector(
      'mat-drawer mat-list-item button'
    );
    deleteButton.click();

    expect(service.selectChat).not.toHaveBeenCalled();
    expect(logSpy).toHaveBeenCalledWith('Delete chat:', 'chat-1');

    logSpy.mockRestore();
  });

  it('starts a new chat when "New chat" is clicked', async () => {
    await setup([{ id: 'chat-1', description: 'Trip planning' }]);

    const toolbarButtons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('mat-toolbar button')
    );
    const newChatButton = toolbarButtons.find(btn => btn.textContent?.includes('New chat'))!;
    newChatButton.click();

    expect(service.clearSelection).toHaveBeenCalled();
  });
});
