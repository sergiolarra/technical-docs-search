import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Fragment } from '../../core/models/document.model';

/**
 * Renders search highlights from structured fragments. Text is always interpolated,
 * never inserted as HTML, so document content cannot inject markup.
 */
@Component({
  selector: 'app-highlighted-text',
  template: `@for (fragment of fragments(); track $index) {@if (fragment.match) {<mark>{{ fragment.text }}</mark>} @else {<span>{{ fragment.text }}</span>}}`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HighlightedText {
  readonly fragments = input.required<Fragment[]>();
}
