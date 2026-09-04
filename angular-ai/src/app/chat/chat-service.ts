import { Service } from '@angular/core';

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ChatResponse } from './chat-response';

@Injectable({
  providedIn: 'root'
})
export class ChatService {

  private readonly API = '/api/chat';
  private readonly http = inject(HttpClient);

  sendChatMessage(message: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(this.API, { message });
  }
}
