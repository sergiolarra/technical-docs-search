import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TrackedUpload } from '../../core/uploads/upload-tracker.service';
import { StatusBadge } from '../../shared/status-badge/status-badge';

@Component({
  selector: 'app-recent-uploads',
  imports: [RouterLink, MatButtonModule, MatCardModule, MatProgressSpinnerModule, StatusBadge],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Recent uploads</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        @if (uploads().length === 0) {
          <p class="empty">You haven't upload documents in this session</p>
        } @else {
          <!-- aria-live: screen readers announce status changes -->
          <ul class="uploads" aria-live="polite">
            @for (upload of uploads(); track upload.id) {
              <li>
                <div class="info">
                  <span class="title">{{ upload.title }}</span>
                  <span class="file">{{ upload.originalFilename }}</span>
                  @if (upload.errorMessage) {
                    <span class="error">{{ upload.errorMessage }}</span>
                  }
                </div>
                <div class="state">
                  @if (upload.status === 'PROCESANDO') {
                    <mat-spinner [diameter]="16" aria-hidden="true" />
                  }
                  <app-status-badge [status]="upload.status" />
                  @if (upload.status === 'INDEXADO') {
                    <a mat-button [routerLink]="['/documents', upload.id]">View</a>
                  }
                </div>
              </li>
            }
          </ul>
          <button mat-button type="button" (click)="clear.emit()" [disabled]="!hasFinished()">Clear terminated</button>
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    .empty { color: rgba(0, 0, 0, 0.6); }
    .uploads { list-style: none; padding: 0; margin: 0 0 8px; }
    .uploads li { display: flex; justify-content: space-between; gap: 12px; padding: 10px 0; border-bottom: 1px solid rgba(0, 0, 0, 0.08); }
    .info { display: flex; flex-direction: column; min-width: 0; }
    .title { font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .file { font-size: 12px; color: rgba(0, 0, 0, 0.6); }
    .error { font-size: 12px; color: #9b1c1c; }
    .state { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecentUploads {
  readonly uploads = input.required<TrackedUpload[]>();
  readonly clear = output<void>();
  protected readonly hasFinished = computed(() => this.uploads().some((upload) => upload.status !== 'PROCESANDO'));
}
