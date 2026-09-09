import { TestBed } from '@angular/core/testing';
import MemoryChatService from './memory-chat-service';

describe('MemoryChatService', () => {
  let service: MemoryChatService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MemoryChatService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
