import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatChipInputEvent, MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Observable, finalize, map } from 'rxjs';
import { DocumentApiService } from '../../core/api/document-api.service';
import { problemOf } from '../../core/http/problem-detail';
import { BatchUploadItem } from '../../core/models/document.model';
import { UploadTracker } from '../../core/uploads/upload-tracker.service';
import { ALLOWED_EXTENSIONS, UploadMode, titleFromFileName, validateFiles } from './file-validation';
import { RecentUploads } from './recent-uploads';

type TextField = 'title' | 'author' | 'category' | 'version';

@Component({
  selector: 'app-upload-page',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    RecentUploads,
  ],
  templateUrl: './upload-page.html',
  styleUrl: './upload-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UploadPage {
  private readonly api = inject(DocumentApiService);
  private readonly tracker = inject(UploadTracker);
  private readonly destroyRef = inject(DestroyRef);
  private autoTitle = '';

  protected readonly separatorKeys = [ENTER, COMMA] as const;
  protected readonly accept = ALLOWED_EXTENSIONS.map((extension) => `.${extension}`).join(',');

  protected readonly mode = signal<UploadMode>('single');
  protected readonly files = signal<File[]>([]);
  protected readonly fileProblems = computed(() => validateFiles(this.files(), this.mode()));
  protected readonly submitted = signal(false);
  protected readonly submitting = signal(false);
  protected readonly dragging = signal(false);
  protected readonly serverError = signal<string | null>(null);
  protected readonly batchResults = signal<BatchUploadItem[] | null>(null);
  protected readonly uploads = this.tracker.uploads;

  protected readonly form = new FormGroup({
    title: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(255)] }),
    author: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(150)] }),
    category: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    version: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(50)] }),
    tags: new FormControl<string[]>([], { nonNullable: true, validators: [Validators.maxLength(20)] }),
  });

  protected setMode(mode: UploadMode): void {
    this.mode.set(mode);
    this.files.set([]);
    this.submitted.set(false);
    this.batchResults.set(null);
    this.serverError.set(null);
    // In batch mode each title comes from its file name
    if (mode === 'batch') {
      this.form.controls.title.disable();
    } else {
      this.form.controls.title.enable();
    }
  }

  protected onFileInput(input: HTMLInputElement): void {
    this.selectFiles(Array.from(input.files ?? []));
    input.value = ''; // lets the same file be selected again
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    this.selectFiles(Array.from(event.dataTransfer?.files ?? []));
  }

  protected removeFile(file: File): void {
    this.files.update((list) => list.filter((candidate) => candidate !== file));
    if (this.mode() === 'single') {
      this.suggestTitle(null);
    }
  }

  protected addTag(event: MatChipInputEvent): void {
    const tag = event.value.trim().toLowerCase();
    const tags = this.form.controls.tags;
    if (tag && !tags.value.includes(tag)) {
      tags.setValue([...tags.value, tag]);
    }
    event.chipInput.clear();
  }

  protected removeTag(tag: string): void {
    const tags = this.form.controls.tags;
    tags.setValue(tags.value.filter((candidate) => candidate !== tag));
  }

  protected errorFor(field: TextField): string {
    const errors = this.form.controls[field].errors;
    if (errors?.['required']) {
      return 'Required';
    }
    if (errors?.['maxlength']) {
      return `At most ${errors['maxlength'].requiredLength} characters`;
    }
    return errors?.['server'] ?? '';
  }

  protected formatSize(bytes: number): string {
    return bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  }

  protected submit(): void {
    this.submitted.set(true);
    this.form.markAllAsTouched();
    if (this.form.invalid || this.files().length === 0 || this.fileProblems().length > 0) {
      return;
    }
    this.submitting.set(true);
    this.serverError.set(null);
    this.batchResults.set(null);

    const { title, ...shared } = this.form.getRawValue();
    const request: Observable<void> =
      this.mode() === 'single'
        ? this.api.upload(this.files()[0], { title, ...shared }).pipe(map((response) => this.tracker.track(response)))
        : this.api.uploadBatch(this.files(), shared).pipe(map((results) => this.trackBatch(results)));

    request.pipe(finalize(() => this.submitting.set(false)), takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.resetAfterUpload(),
      error: (error: unknown) => this.showServerError(error),
    });
  }

  protected clearFinished(): void {
    this.tracker.clearFinished();
  }

  private selectFiles(selected: File[]): void {
    if (selected.length === 0) {
      return;
    }
    this.serverError.set(null);
    this.batchResults.set(null);
    this.files.set(this.mode() === 'single' ? selected.slice(0, 1) : [...this.files(), ...selected]);
    if (this.mode() === 'single') {
      this.suggestTitle(selected[0].name);
    }
  }

  private suggestTitle(fileName: string | null): void {
    const title = this.form.controls.title;
    if (title.value && title.value !== this.autoTitle) {
      return; // the user typed their own title
    }
    this.autoTitle = fileName ? titleFromFileName(fileName) : '';
    title.setValue(this.autoTitle);
  }

  private trackBatch(results: BatchUploadItem[]): void {
    for (const item of results) {
      if (item.accepted && item.id && item.status) {
        this.tracker.track({
          id: item.id,
          title: titleFromFileName(item.originalFilename),
          originalFilename: item.originalFilename,
          status: item.status,
        });
      }
    }
    this.batchResults.set(results);
  }

  /** Keeps author, category, version and tags: consecutive uploads usually share them. */
  private resetAfterUpload(): void {
    this.files.set([]);
    this.submitted.set(false);
    this.autoTitle = '';
    this.form.controls.title.reset('');
  }

  private showServerError(error: unknown): void {
    // Connection errors and 5xx are already shown globally by the interceptor
    if (!(error instanceof HttpErrorResponse) || error.status === 0 || error.status >= 500) {
      return;
    }
    const problem = problemOf(error);
    this.serverError.set(problem.detail ?? problem.title ?? 'The document could not be uploaded');
    for (const [field, message] of Object.entries(problem.errors ?? {})) {
      this.form.get(field)?.setErrors({ server: message });
    }
  }
}
