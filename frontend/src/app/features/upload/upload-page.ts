import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-upload-page',
  template: `<h1>Cargar documentos</h1>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UploadPage {}
