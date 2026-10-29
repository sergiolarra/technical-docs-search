import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe, Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Title } from '@angular/platform-browser';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MarkdownComponent } from 'ngx-markdown';
import { Observable, Subject, catchError, filter, map, merge, of, startWith, switchMap } from 'rxjs';
import { DocumentApiService } from '../../core/api/document-api.service';
import { DocumentEventsService } from '../../core/events/document-events.service';
import { DocumentDetail } from '../../core/models/document.model';
import { formatSize } from '../../shared/format';
import { StatusBadge } from '../../shared/status-badge/status-badge';

/** Beyond this, rendering the whole text at once would freeze the page (content can reach millions of characters). */
const CONTENT_PREVIEW_CHARS = 100_000;

type ViewState =
  | { kind: 'loading' }
  | { kind: 'loaded'; document: DocumentDetail }
  | { kind: 'not-found' }
  | { kind: 'error' };

@Component({
  selector: 'app-document-viewer-page',
  imports: [
    DatePipe,
    DecimalPipe,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MarkdownComponent,
    StatusBadge,
  ],
  templateUrl: './document-viewer-page.html',
  styleUrl: './document-viewer-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentViewerPage {
  /** Bound from the :id route parameter (withComponentInputBinding). */
  readonly id = input.required<string>();

  private readonly api = inject(DocumentApiService);
  private readonly events = inject(DocumentEventsService);
  private readonly titleService = inject(Title);
  private readonly location = inject(Location);
  private readonly router = inject(Router);
  private readonly reloads = new Subject<void>();

  protected readonly previewChars = CONTENT_PREVIEW_CHARS;
  protected readonly formatSize = formatSize;
  protected readonly showFullContent = signal(false);

  protected readonly view = toSignal(
    merge(
      // A new id shows the loading state; a reload of the same document keeps the current view until it arrives
      toObservable(this.id).pipe(map((id) => ({ id, showLoading: true }))),
      this.reloads.pipe(map(() => ({ id: this.id(), showLoading: false }))),
    ).pipe(switchMap(({ id, showLoading }) => this.load(id, showLoading))),
    { initialValue: { kind: 'loading' } as ViewState },
  );

  protected readonly document = computed(() => {
    const view = this.view();
    return view.kind === 'loaded' ? view.document : null;
  });
  protected readonly contentLength = computed(() => this.document()?.content?.length ?? 0);
  protected readonly isTruncated = computed(() => this.contentLength() > CONTENT_PREVIEW_CHARS && !this.showFullContent());
  protected readonly visibleContent = computed(() => {
    const content = this.document()?.content ?? '';
    return this.isTruncated() ? content.slice(0, CONTENT_PREVIEW_CHARS) : content;
  });

  constructor() {
    // HU-04: a document still being processed refreshes itself when its event arrives
    this.events.statusChanges$
      .pipe(
        filter((event) => event.documentId === this.id()),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.reloads.next());

    // Events sent while the stream was down are lost: re-read a document that was still processing
    this.events.reconnected$
      .pipe(
        filter(() => this.document()?.status === 'PROCESANDO'),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.reloads.next());

    effect(() => {
      this.id();
      untracked(() => this.showFullContent.set(false));
    });

    effect(() => {
      const document = this.document();
      if (document) {
        this.titleService.setTitle(`${document.title} · Technical Docs Search`);
      }
    });
  }

  protected retry(): void {
    this.reloads.next();
  }

  /** Goes back to the previous screen (e.g. the same search results); opened from a shared link, goes to search. */
  protected back(): void {
    if (window.history.length > 1) {
      this.location.back();
    } else {
      this.router.navigate(['/search']);
    }
  }

  private load(id: string, showLoading: boolean): Observable<ViewState> {
    const request = this.api.getById(id).pipe(
      map((document): ViewState => ({ kind: 'loaded', document })),
      catchError((error: unknown) => of(toErrorState(error))),
    );
    return showLoading ? request.pipe(startWith<ViewState>({ kind: 'loading' })) : request;
  }
}

function toErrorState(error: unknown): ViewState {
  // 400 = malformed id in the URL: for the user it is just a document that does not exist
  if (error instanceof HttpErrorResponse && (error.status === 404 || error.status === 400)) {
    return { kind: 'not-found' };
  }
  return { kind: 'error' };
}
