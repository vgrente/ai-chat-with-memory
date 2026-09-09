import {ChatMessage, ChatType} from '../../chat-message';
import {catchError, of} from 'rxjs';
import {ChatStartResponse} from '../../chat';
import {Component, effect, ElementRef, inject, signal, viewChild} from '@angular/core';
import {MatCardModule} from '@angular/material/card';
import {MatInputModule} from '@angular/material/input';
import {MatButtonModule} from '@angular/material/button';
import {FormsModule} from '@angular/forms';
import {MatIconModule} from '@angular/material/icon';
import {MarkdownToHtmlPipe} from '../../../shared/mark-down-to-html.pipe';
import MemoryChatService from '../memory-chat-service';

@Component({
  selector: 'app-chat-panel',
  imports: [MatCardModule, MatInputModule, MatButtonModule, FormsModule, MatIconModule, MarkdownToHtmlPipe],
  templateUrl: './chat-panel.html',
  styleUrl: './chat-panel.scss'
})
export class ChatPanel {

  private readonly chatHistory = viewChild<ElementRef>('chatHistory');
  protected readonly memoryChatService = inject(MemoryChatService);

  userInput = '';
  isLoading = false;
  messages = signal<ChatMessage[]>([]);

  /**
   * Effect: Sync messages from service resource
   *
   * When a user selects a chat from the list, the service loads that chat's messages.
   * This effect automatically updates our local messages signal with the loaded data.
   *
   * Why? Keeps the UI in sync with the service's data without manual subscriptions.
   */
  private readonly syncMessagesEffect = effect(() => {
    const resourceMessages = this.memoryChatService.chatMessagesResource.value();
    if (resourceMessages) {
      this.messages.set(resourceMessages);
    }
  });

  /**
   * Effect: Auto-scroll to bottom when messages change
   *
   * Whenever new messages are added, we want to scroll to show the latest one.
   * setTimeout ensures the DOM has updated before we scroll.
   *
   * Why? Better UX - users always see the newest message without manual scrolling.
   */
  private readonly autoScrollEffect = effect(() => {
    this.messages(); // Read the signal to track changes
    setTimeout(() => this.scrollToBottom(), 0);
  });

  /**
   * Tracks the previously seen chat id, captured at construction time so it
   * reflects whatever was already selected on mount (e.g. after navigating
   * away from and back to this route, where the singleton service still
   * holds the previous selection).
   */
  private previousChatId = this.memoryChatService.selectedChatId();

  /**
   * Effect: Clear messages when switching chats
   *
   * When the user clicks "New chat" or switches to a different conversation,
   * we clear the current messages to avoid showing the wrong history.
   *
   * Why? Prevents visual glitches where old messages briefly appear.
   * Only clears on an actual change - not on the effect's initial run -
   * so remounting this component doesn't wipe out messages that
   * syncMessagesEffect just restored from the already-cached resource.
   */
  private readonly clearMessagesEffect = effect(() => {
    const chatId = this.memoryChatService.selectedChatId();
    if (chatId !== this.previousChatId) {
      this.previousChatId = chatId;
      this.messages.set([]);
    }
  });

  /**
   * Sends a user message to the AI
   *
   * Flow:
   * 1. Validates and trims input
   * 2. Adds user message to UI immediately (optimistic update)
   * 3. Sets loading state
   * 4. Calls sendChatMessage() to handle the API request
   */
  sendMessage(): void {
    this.trimUserMessage();
    if (this.userInput !== '' && !this.isLoading) {
      this.updateMessages(this.userInput);
      this.isLoading = true;
      this.sendChatMessage();
    }
  }

  /**
   * Handles the API call for sending messages
   *
   * Two scenarios:
   * 1. If a chat is selected: Continue existing conversation (POST /api/chat-memory/{chatId})
   * 2. If no chat selected: Start new conversation (POST /api/chat-memory/start)
   *
   * After response: Updates messages and refreshes chat list (for new chats)
   */
  private sendChatMessage() {
    const currentChatId = this.memoryChatService.selectedChatId();
    const message = this.userInput;

    if (currentChatId) {
      // Continue existing chat
      this.memoryChatService.continueChat(currentChatId, message)
        .pipe(catchError((error) => this.handleError(error)))
        .subscribe((response: ChatMessage) => {
          if (response) {
            this.updateMessages(response.content, ChatType.ASSISTANT);
          }
          this.finishMessage();
        });
    } else {
      // Start new chat
      this.memoryChatService.startNewChat(message)
        .pipe(catchError((error) => this.handleError(error)))
        .subscribe((response: ChatStartResponse) => {
          if (response) {
            this.memoryChatService.selectChat(response.chatId);
            this.memoryChatService.chatsResource.reload();  // Refresh sidebar to show new chat
          }
          this.finishMessage();
        });
    }
  }

  /**
   * Handles API errors gracefully
   *
   * Instead of crashing, we show user-friendly error messages based on HTTP status:
   * - 0: Network/connection issues
   * - 404: Chat not found (possibly deleted)
   * - 500: Server error
   * - Default: Generic error message
   *
   * Returns an empty Observable to complete the stream gracefully.
   */
  private handleError(error?: any) {
    console.error('Chat error:', error);

    let errorMessage = 'Sorry, I am unable to process your request at the moment.';

    if (error?.status === 0) {
      errorMessage = 'Unable to connect to the server. Please check your connection.';
    } else if (error?.status === 404) {
      errorMessage = 'Chat not found. Please start a new conversation.';
    } else if (error?.status === 500) {
      errorMessage = 'Server error. Please try again later.';
    }

    this.updateMessages(errorMessage, ChatType.ASSISTANT);
    this.isLoading = false;
    return of();
  }

  /**
   * Cleans up after sending a message
   * Clears the input field and resets loading state
   */
  private finishMessage() {
    this.userInput = '';
    this.isLoading = false;
  }

  /**
   * Adds a message to the conversation
   * Uses signal.update() for immutable updates - creates new array with added message
   *
   * @param content - The message text
   * @param type - USER or ASSISTANT (defaults to USER)
   */
  private updateMessages(content: string, type: ChatType = ChatType.USER) {
    this.messages.update((messages: ChatMessage[]) => [...messages, { content, type }]);
  }

  /**
   * Removes whitespace from user input
   * Prevents sending messages that are only spaces
   */
  private trimUserMessage(): void {
    this.userInput = this.userInput.trim();
  }

  /**
   * Handles Enter key to send messages
   * Enter alone: Send message
   * Shift+Enter: New line (default textarea behavior)
   */
  onKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  /**
   * Scrolls the chat container to the bottom
   *
   * Called by the autoScrollEffect whenever messages change.
   * Uses scrollHeight (total content height) to scroll to the very bottom.
   *
   * Why the try/catch? Prevents errors if the component is destroyed
   * while scrolling is in progress.
   */
  private scrollToBottom(): void {
    try {
      const chatElement = this.chatHistory();
      if (chatElement?.nativeElement) {
        chatElement.nativeElement.scrollTop = chatElement.nativeElement.scrollHeight;
      }
    } catch (err) {
      console.error('Failed to scroll chat history:', err);
    }
  }
}
