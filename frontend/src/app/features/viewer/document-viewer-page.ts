import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-document-viewer-page',
  template: `<h1>Documento {{ id() }}</h1>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentViewerPage {
  /** Bound from the :id route parameter (withComponentInputBinding). */
  readonly id = input.required<string>();
}
