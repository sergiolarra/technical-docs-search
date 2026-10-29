import { DestroyRef, Injectable, InjectionToken, inject, signal } from '@angular/core';
import { Subject } from 'rxjs';
import { DocumentStatusEvent } from '../models/document.model';

/** Creates the EventSource; tests replace it with a fake. */
export const EVENT_SOURCE_FACTORY = new InjectionToken<(url: string) => EventSource>('EVENT_SOURCE_FACTORY', {
  providedIn: 'root',
  factory: () => (url: string) => new EventSource(url),
});

/**
 * Single SSE connection for the whole app (one per tab). The browser reconnects by itself using the
 * retry delay sent by the server; after a reconnection, views re-read any state they may have missed.
 */
@Injectable({ providedIn: 'root' })
export class DocumentEventsService {
  private readonly createEventSource = inject(EVENT_SOURCE_FACTORY);
  private readonly destroyRef = inject(DestroyRef);

  private readonly statusChangesSubject = new Subject<DocumentStatusEvent>();
  private readonly reconnectedSubject = new Subject<void>();
  private source?: EventSource;
  private hasConnectedBefore = false;

  readonly connected = signal(false);
  readonly statusChanges$ = this.statusChangesSubject.asObservable();
  readonly reconnected$ = this.reconnectedSubject.asObservable();

  connect(): void {
    if (this.source) {
      return;
    }
    const source = this.createEventSource('/api/events');
    source.addEventListener('connected', () => {
      this.connected.set(true);
      if (this.hasConnectedBefore) {
        this.reconnectedSubject.next();
      }
      this.hasConnectedBefore = true;
    });
    source.addEventListener('document-status', (event) => {
      this.statusChangesSubject.next(JSON.parse((event as MessageEvent<string>).data) as DocumentStatusEvent);
    });
    // EventSource retries on its own; this only updates the indicator
    source.onerror = () => this.connected.set(false);

    this.source = source;
    this.destroyRef.onDestroy(() => source.close());
  }
}
