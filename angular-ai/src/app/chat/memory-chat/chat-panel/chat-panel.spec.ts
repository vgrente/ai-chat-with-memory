import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { ChatPanel } from './chat-panel';
import { ChatMessage, ChatType } from '../../chat-message';
import { ChatStartResponse } from '../../chat';
import MemoryChatService from '../memory-chat-service';

function mockMemoryChatService() {
  return {
    selectedChatId: signal<string | undefined>(undefined),
    chatMessagesResource: { value: signal<ChatMessage[] | undefined>(undefined) },
    chatsResource: { reload: vi.fn() },
    startNewChat: vi.fn(),
    continueChat: vi.fn(),
    selectChat: vi.fn(),
    clearSelection: vi.fn()
  };
}

describe('ChatPanel', () => {
  let fixture: ComponentFixture<ChatPanel>;
  let component: ChatPanel;
  let service: ReturnType<typeof mockMemoryChatService>;

  beforeEach(async () => {
    service = mockMemoryChatService();

    await TestBed.configureTestingModule({
      imports: [ChatPanel],
      providers: [{ provide: MemoryChatService, useValue: service }]
    }).compileComponents();

    fixture = TestBed.createComponent(ChatPanel);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('shows the welcome message when no chat is selected', () => {
    expect(fixture.nativeElement.textContent).toContain('Welcome to AI Chat with Memory');
  });

  describe('sendMessage', () => {
    it('does nothing for empty or whitespace-only input', () => {
      component.userInput = '   ';

      component.sendMessage();

      expect(service.startNewChat).not.toHaveBeenCalled();
      expect(component.messages()).toEqual([]);
    });

    it('does nothing while a request is already in flight', () => {
      component.userInput = 'hello';
      component.isLoading = true;

      component.sendMessage();

      expect(service.startNewChat).not.toHaveBeenCalled();
    });

    it('starts a new chat when none is selected, then selects it and refreshes the sidebar', async () => {
      const response: ChatStartResponse = { chatId: 'chat-1', message: 'hi there', description: 'desc' };
      service.startNewChat.mockReturnValue(of(response));
      component.userInput = 'hello';

      component.sendMessage();
      await fixture.whenStable();

      expect(service.startNewChat).toHaveBeenCalledWith('hello');
      expect(service.selectChat).toHaveBeenCalledWith('chat-1');
      expect(service.chatsResource.reload).toHaveBeenCalled();
      expect(component.isLoading).toBe(false);
      expect(component.userInput).toBe('');
      // The user's own message is added optimistically; the assistant's reply
      // arrives later via chatMessagesResource (syncMessagesEffect), not here.
      expect(component.messages()).toEqual([{ content: 'hello', type: ChatType.USER }]);
    });

    it('continues an existing chat and appends the assistant reply', async () => {
      service.selectedChatId.set('chat-1');
      await fixture.whenStable();
      const reply: ChatMessage = { content: 'hi there', type: ChatType.ASSISTANT };
      service.continueChat.mockReturnValue(of(reply));
      component.userInput = 'hello';

      component.sendMessage();
      await fixture.whenStable();

      expect(service.continueChat).toHaveBeenCalledWith('chat-1', 'hello');
      expect(component.messages()).toEqual([
        { content: 'hello', type: ChatType.USER },
        { content: 'hi there', type: ChatType.ASSISTANT }
      ]);
      expect(component.isLoading).toBe(false);
    });

    it.each([
      [0, 'Unable to connect to the server. Please check your connection.'],
      [404, 'Chat not found. Please start a new conversation.'],
      [500, 'Server error. Please try again later.'],
      [418, 'Sorry, I am unable to process your request at the moment.']
    ])('maps a %i error to a user-friendly message', async (status, expectedMessage) => {
      service.selectedChatId.set('chat-1');
      await fixture.whenStable();
      service.continueChat.mockReturnValue(throwError(() => ({ status })));
      component.userInput = 'hello';

      component.sendMessage();
      await fixture.whenStable();

      const last = component.messages().at(-1);
      expect(last).toEqual({ content: expectedMessage, type: ChatType.ASSISTANT });
      expect(component.isLoading).toBe(false);
    });
  });

  describe('reactive effects', () => {
    it('syncs messages when chatMessagesResource resolves', async () => {
      const loaded: ChatMessage[] = [
        { content: 'hi', type: ChatType.USER },
        { content: 'hello!', type: ChatType.ASSISTANT }
      ];

      service.chatMessagesResource.value.set(loaded);
      await fixture.whenStable();

      expect(component.messages()).toEqual(loaded);
    });

    it('clears messages when the selected chat changes', async () => {
      component.messages.set([{ content: 'stale', type: ChatType.USER }]);

      service.selectedChatId.set('a-different-chat');
      await fixture.whenStable();

      expect(component.messages()).toEqual([]);
    });

    it('keeps the cached history when remounting with a chat already selected', async () => {
      // Simulates navigating away from and back to this route: the singleton
      // service already has a chat selected and its resource already resolved,
      // before this ChatPanel instance is even created.
      const remountedService = mockMemoryChatService();
      remountedService.selectedChatId.set('chat-1');
      remountedService.chatMessagesResource.value.set([
        { content: 'old question', type: ChatType.USER },
        { content: 'old answer', type: ChatType.ASSISTANT }
      ]);

      await TestBed.resetTestingModule().configureTestingModule({
        imports: [ChatPanel],
        providers: [{ provide: MemoryChatService, useValue: remountedService }]
      }).compileComponents();

      const remountedFixture = TestBed.createComponent(ChatPanel);
      await remountedFixture.whenStable();

      expect(remountedFixture.componentInstance.messages()).toEqual([
        { content: 'old question', type: ChatType.USER },
        { content: 'old answer', type: ChatType.ASSISTANT }
      ]);
    });
  });

  describe('onKeyPress', () => {
    it('sends the message on Enter without Shift', () => {
      component.userInput = 'hello';
      service.startNewChat.mockReturnValue(of({ chatId: 'x', message: 'y', description: 'z' }));
      const event = new KeyboardEvent('keypress', { key: 'Enter', shiftKey: false, cancelable: true });

      component.onKeyPress(event);

      expect(event.defaultPrevented).toBe(true);
      expect(service.startNewChat).toHaveBeenCalledWith('hello');
    });

    it('does not send on Shift+Enter, leaving the newline to the textarea', () => {
      component.userInput = 'hello';
      const event = new KeyboardEvent('keypress', { key: 'Enter', shiftKey: true, cancelable: true });

      component.onKeyPress(event);

      expect(event.defaultPrevented).toBe(false);
      expect(service.startNewChat).not.toHaveBeenCalled();
    });
  });
});
