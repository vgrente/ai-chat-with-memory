import {inject, Pipe, PipeTransform, SecurityContext} from '@angular/core';
import { marked, Tokens } from 'marked';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Pipe({
  name: 'markdownToHtml',
  standalone: true
})
export class MarkdownToHtmlPipe implements PipeTransform {

  readonly sanitizer = inject(DomSanitizer);

  transform(value: string): any {
    if (!value) {
      return value;
    }

    try {
      const html = marked.parse(value) as string;
      return this.sanitizer.sanitize(SecurityContext.HTML, html);
    } catch (error) {
      // Fallback to original value if parsing fails
      console.error('Markdown parsing error:', error);
      return this.sanitizer.sanitize(SecurityContext.HTML, value);
    }
  }
}
