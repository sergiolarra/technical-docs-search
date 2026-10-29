import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { DocumentEventsService } from './core/events/document-events.service';
import { TrackedUpload, UploadTracker } from './core/uploads/upload-tracker.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  private readonly events = inject(DocumentEventsService);
  private readonly uploads = inject(UploadTracker);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly connected = this.events.connected;
  protected readonly pendingUploads = this.uploads.pendingCount;

  constructor() {
    this.events.connect();
    this.uploads.finished$.pipe(takeUntilDestroyed()).subscribe((upload) => this.announce(upload));
  }

  /** Shown on any screen when one of this tab's uploads finishes processing. */
  private announce(upload: TrackedUpload): void {
    const message =
      upload.status === 'INDEXADO'
        ? `"${upload.title}" is indexed and ready to search`
        : `"${upload.title}" could not be processed`;
    this.snackBar
      .open(message, 'View', { duration: 6000 })
      .onAction()
      .subscribe(() => this.router.navigate(['/documents', upload.id]));
  }
}
