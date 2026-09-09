import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'simple-chat', pathMatch: 'full' },
  { path: 'simple-chat',
    loadComponent: () => import('./chat/simple-chat/simple-chat').then(c => c.SimpleChat)
  },
  { path: 'chat-memory',
    loadComponent: () => import('./components/chat-list/chat-list').then(c => c.ChatList)
  },
  { path: '**', redirectTo: 'simple-chat' }
];
