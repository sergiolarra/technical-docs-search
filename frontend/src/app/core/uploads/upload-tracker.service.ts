import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Observable, Subject, catchError, forkJoin, of, switchMap, tap } from 'rxjs';
import { DocumentApiService } from '../api/document-api.service';
import { DocumentEventsService } from '../events/document-events.service';
import { DocumentStatus, DocumentStatusEvent } from '../models/document.model';

export interface TrackedUpload {
  id: string;
  title: string;
  originalFilename: string;
  status: DocumentStatus;
  errorMessage: string | null;
}

const STORAGE_KEY = 'tds.recent-uploads';
const MAX_TRACKED = 50;
const MAX_REMEMBERED_EVENTS = 500;

/**
 * Uploads made from this tab and their live status. The SSE stream carries every user's events,
 * so only documents uploaded here are tracked and announced.
 */
@Injectable({ providedIn: 'root' })
export class UploadTracker {
  private readonly api = inject(DocumentApiService);
  private readonly events = inject(DocumentEventsService);

  private readonly items = signal<TrackedUpload[]>(loadFromSession());
  /** Last status seen per document: covers events that arrive before the upload response. */
  private readonly seenEvents = new Map<string, DocumentStatusEvent>();
  private readonly finishedSubject = new Subject<TrackedUpload>();

  /** Newest first. */
  readonly uploads = this.items.asReadonly();
  readonly pendingCount = computed(() => this.items().filter((upload) => upload.status === 'PROCESANDO').length);
  /** Emits when a tracked upload reaches INDEXADO or ERROR. */
  readonly finished$ = this.finishedSubject.asObservable();

  constructor() {
    effect(() => saveToSession(this.items()));

    this.events.statusChanges$.pipe(takeUntilDestroyed()).subscribe((event) => {
      this.remember(event);
      this.apply(event.documentId, event.status, event.errorMessage);
    });

    // Events sent while the stream was down are lost: re-read what is still pending
    this.events.reconnected$.pipe(takeUntilDestroyed(), switchMap(() => this.refreshPending())).subscribe();

    // After a page reload, documents may have finished while nobody was listening
    this.refreshPending().subscribe();
  }

  track(upload: Pick<TrackedUpload, 'id' | 'title' | 'originalFilename' | 'status'>): void {
    this.items.update((list) => [{ ...upload, errorMessage: null }, ...list].slice(0, MAX_TRACKED));
    const early = this.seenEvents.get(upload.id);
    if (early) {
      this.apply(early.documentId, early.status, early.errorMessage);
    }
  }

  clearFinished(): void {
    this.items.update((list) => list.filter((upload) => upload.status === 'PROCESANDO'));
  }

  private apply(id: string, status: DocumentStatus, errorMessage: string | null): void {
    const current = this.items().find((upload) => upload.id === id);
    if (status === 'PROCESANDO' || !current || current.status !== 'PROCESANDO') {
      return;
    }
    const finished: TrackedUpload = { ...current, status, errorMessage };
    this.items.update((list) => list.map((upload) => (upload.id === id ? finished : upload)));
    this.finishedSubject.next(finished);
  }

  private remember(event: DocumentStatusEvent): void {
    this.seenEvents.set(event.documentId, event);
    if (this.seenEvents.size > MAX_REMEMBERED_EVENTS) {
      const oldest = this.seenEvents.keys().next().value;
      if (oldest) {
        this.seenEvents.delete(oldest);
      }
    }
  }

  private refreshPending(): Observable<unknown> {
    const pending = this.items().filter((upload) => upload.status === 'PROCESANDO');
    if (pending.length === 0) {
      return EMPTY;
    }
    return forkJoin(pending.map((upload) => this.api.getById(upload.id).pipe(catchError(() => of(null))))).pipe(
      tap((documents) => documents.forEach((document) => document && this.apply(document.id, document.status, document.errorMessage))),
    );
  }
}

function loadFromSession(): TrackedUpload[] {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as TrackedUpload[]) : [];
  } catch {
    return [];
  }
}

function saveToSession(items: TrackedUpload[]): void {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  } catch {
    // Storage unavailable (private mode, quota): the list lives in memory only
  }
}
