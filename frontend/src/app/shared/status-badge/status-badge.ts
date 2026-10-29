import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DocumentStatus } from '../../core/models/document.model';

const LABELS: Record<DocumentStatus, string> = {
  PROCESANDO: 'Processing',
  INDEXADO: 'Indexed',
  ERROR: 'Error',
};

@Component({
  selector: 'app-status-badge',
  template: `<span class="badge" [class]="'badge ' + status().toLowerCase()" role="status">{{ label() }}</span>`,
  styles: `
    .badge { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
    .procesando { background: #fff4d6; color: #7a5600; }
    .indexado { background: #e3f4e4; color: #1d5f22; }
    .error { background: #fde8e8; color: #9b1c1c; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusBadge {
  readonly status = input.required<DocumentStatus>();
  protected readonly label = computed(() => LABELS[this.status()]);
}
