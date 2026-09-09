import { HttpEventType, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SseClient, SseEvent } from './sse-client';

describe('SseClient', () => {
  let client: SseClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    client = TestBed.inject(SseClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function collect(url: string, body: unknown = {}): { events: SseEvent[]; done: boolean } {
    const result = { events: [] as SseEvent[], done: false };
    client.post(url, body).subscribe({
      next: e => result.events.push(e),
      complete: () => (result.done = true)
    });
    return result;
  }

  it('parses a single complete frame from the final response', () => {
    const result = collect('/stream');
    const req = httpMock.expectOne('/stream');

    // flush() emits the final HttpResponse *and* completes the observable —
    // event() alone only emits, it never completes.
    req.flush('event: message\ndata: {"content":"hi"}\n\n');

    expect(result.events).toEqual([{ event: 'message', data: { content: 'hi' } }]);
    expect(result.done).toBe(true);
  });

  it('reassembles a frame split across multiple progressive chunks', () => {
    const result = collect('/stream');
    const req = httpMock.expectOne('/stream');

    // First chunk ends mid-frame — nothing should be emitted yet.
    req.event({
      type: HttpEventType.DownloadProgress,
      loaded: 10,
      partialText: 'event: message\ndata: {"content"'
    } as any);
    expect(result.events).toEqual([]);

    // Second chunk completes the frame.
    req.event({
      type: HttpEventType.DownloadProgress,
      loaded: 20,
      partialText: 'event: message\ndata: {"content":"hi"}\n\n'
    } as any);
    expect(result.events).toEqual([{ event: 'message', data: { content: 'hi' } }]);

    req.flush('event: message\ndata: {"content":"hi"}\n\n');
    // No duplicate emission for the already-processed frame.
    expect(result.events).toEqual([{ event: 'message', data: { content: 'hi' } }]);
  });

  it('emits each frame exactly once across several progress chunks', () => {
    const result = collect('/stream');
    const req = httpMock.expectOne('/stream');

    req.event({
      type: HttpEventType.DownloadProgress,
      loaded: 10,
      partialText: 'data:one\n\n'
    } as any);
    req.event({
      type: HttpEventType.DownloadProgress,
      loaded: 20,
      partialText: 'data:one\n\ndata:two\n\n'
    } as any);
    req.flush('data:one\n\ndata:two\n\n');

    expect(result.events).toEqual([
      { event: 'message', data: 'one' },
      { event: 'message', data: 'two' }
    ]);
  });

  it('defaults the event name to "message" when no event: line is present', () => {
    const result = collect('/stream');
    const req = httpMock.expectOne('/stream');

    req.flush('data:plain\n\n');

    expect(result.events).toEqual([{ event: 'message', data: 'plain' }]);
  });

  it('returns the raw string when the data payload is not valid JSON', () => {
    const result = collect('/stream');
    const req = httpMock.expectOne('/stream');

    req.flush('data:not json at all\n\n');

    expect(result.events).toEqual([{ event: 'message', data: 'not json at all' }]);
  });

  it('does not trim the data payload', () => {
    const result = collect('/stream');
    const req = httpMock.expectOne('/stream');

    // Spring writes "data:" with no padding space, so leading/trailing spaces
    // here are meaningful word-boundary whitespace from a streamed answer,
    // not formatting — the client must preserve them exactly.
    req.flush('data: leading and trailing \n\n');

    expect(result.events).toEqual([{ event: 'message', data: ' leading and trailing ' }]);
  });

  it('propagates request errors to the subscriber', () => {
    let caughtError: unknown;
    client.post('/stream', {}).subscribe({ error: err => (caughtError = err) });
    const req = httpMock.expectOne('/stream');

    req.error(new ProgressEvent('error'), { status: 500, statusText: 'Server Error' });

    expect(caughtError).toBeDefined();
  });
});
