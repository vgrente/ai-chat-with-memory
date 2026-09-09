import { TestBed } from '@angular/core/testing';
import { marked } from 'marked';
import { MarkdownToHtmlPipe } from './mark-down-to-html.pipe';

describe('MarkdownToHtmlPipe', () => {
  let pipe: MarkdownToHtmlPipe;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    pipe = TestBed.runInInjectionContext(() => new MarkdownToHtmlPipe());
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('returns the value unchanged when it is an empty string', () => {
    expect(pipe.transform('')).toBe('');
  });

  it('returns the value unchanged when it is null or undefined', () => {
    expect(pipe.transform(null as unknown as string)).toBeNull();
    expect(pipe.transform(undefined as unknown as string)).toBeUndefined();
  });

  it('converts basic markdown to HTML', () => {
    const result = pipe.transform('**bold** and *italic*');

    expect(result).toContain('<strong>bold</strong>');
    expect(result).toContain('<em>italic</em>');
  });

  it('converts a markdown list to an HTML list', () => {
    const result = pipe.transform('- one\n- two');

    expect(result).toContain('<ul>');
    expect(result).toContain('<li>one</li>');
    expect(result).toContain('<li>two</li>');
  });

  it('converts a fenced code block to a <pre><code> block', () => {
    const result = pipe.transform('```\nconst x = 1;\n```');

    expect(result).toContain('<pre>');
    expect(result).toContain('const x = 1;');
  });

  it('sanitizes raw HTML embedded in the markdown', () => {
    const result = pipe.transform('hello <script>alert("xss")</script> world');

    expect(result).not.toContain('<script');
    expect(result).not.toContain('alert(');
  });

  it('strips disallowed event-handler attributes from raw HTML', () => {
    const result = pipe.transform('<img src="x" onerror="alert(1)">');

    expect(result).not.toContain('onerror');
  });

  it('falls back to the sanitized original value when markdown parsing throws', () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    vi.spyOn(marked, 'parse').mockImplementation(() => {
      throw new Error('boom');
    });

    const result = pipe.transform('plain text');

    expect(result).toBe('plain text');
    expect(consoleErrorSpy).toHaveBeenCalledWith('Markdown parsing error:', expect.any(Error));
  });
});
