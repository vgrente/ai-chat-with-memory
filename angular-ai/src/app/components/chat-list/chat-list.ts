import MemoryChatService from '../../chat/memory-chat/memory-chat-service';
import {Component, inject} from '@angular/core';
import {MatSidenavModule} from '@angular/material/sidenav';
import {MatCardModule} from '@angular/material/card';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatListModule} from '@angular/material/list';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {ChatPanel} from '../../chat/memory-chat/chat-panel/chat-panel';

@Component({
  selector: 'app-chat-list',
  imports: [MatSidenavModule, MatCardModule, MatToolbarModule, MatListModule, MatIconModule, MatButtonModule, ChatPanel],
  templateUrl: './chat-list.html',
  styleUrl: './chat-list.scss'
})
export class ChatList {

  readonly memoryChatService = inject(MemoryChatService);
  chats = this.memoryChatService.chatsResource;  // Reactive resource that loads all chats

  /**
   * Selects a chat from the list
   * This triggers the service to load that chat's messages
   */
  selectChat(chatId: string) {
    this.memoryChatService.selectChat(chatId);
  }

  /**
   * Clears the selection to start a new chat
   * When no chat is selected, the panel shows a welcome message
   */
  createNewChat() {
    this.memoryChatService.clearSelection();
  }

  /**
   * Deletes a chat (placeholder implementation)
   * event.stopPropagation() prevents the chat from being selected when clicking delete
   */
  deleteChat(chatId: string, event: Event) {
    event.stopPropagation();
    console.log('Delete chat:', chatId);
  }
}
