import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-search-page',
  template: `<h1>Buscar documentos</h1>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchPage {}
