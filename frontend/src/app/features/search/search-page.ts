import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-search-page',
  template: `<h1>Search documents</h1>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchPage {}
