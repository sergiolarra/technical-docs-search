import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { FakeEventSource, fakeEventSourceFactory } from '../../../testing/fake-event-source';
import { DocumentApiService } from '../api/document-api.service';
import { DocumentEventsService, EVENT_SOURCE_FACTORY } from '../events/document-events.service';
import { DocumentDetail, DocumentStatus, DocumentStatusEvent } from '../models/document.model';
import { TrackedUpload, UploadTracker } from './upload-tracker.service';

describe('UploadTracker', () => {
  const api = { getById: vi.fn() };
  let tracker: UploadTracker;
  let stream: FakeEventSource;

  const upload = { id: 'doc-1', title: 'Guía', originalFilename: 'guia.md', status: 'PROCESANDO' as const };

  function statusEvent(status: DocumentStatus, documentId = 'doc-1'): DocumentStatusEvent {
    return {
      documentId,
      title: 'Guía',
      status,
      errorMessage: status === 'ERROR' ? 'PDF corrupto' : null,
      occurredAt: '2026-10-29T10:00:00Z',
    };
  }

  beforeEach(() => {
    sessionStorage.clear();
    FakeEventSource.instances = [];
    api.getById.mockReset();
    TestBed.configureTestingModule({
      providers: [
        { provide: EVENT_SOURCE_FACTORY, useValue: fakeEventSourceFactory },
        { provide: DocumentApiService, useValue: api },
      ],
    });
    TestBed.inject(DocumentEventsService).connect();
    tracker = TestBed.inject(UploadTracker);
    stream = FakeEventSource.latest();
    stream.emit('connected', {});
  });

  it('updates a tracked upload when its event arrives', () => {
    const finished: TrackedUpload[] = [];
    tracker.finished$.subscribe((item) => finished.push(item));
    tracker.track(upload);

    stream.emit('document-status', statusEvent('INDEXADO'));

    expect(tracker.uploads()[0].status).toBe('INDEXADO');
    expect(tracker.pendingCount()).toBe(0);
    expect(finished.map((item) => item.id)).toEqual(['doc-1']);
  });

  it('applies an event that arrived before the upload response', () => {
    stream.emit('document-status', statusEvent('INDEXADO'));

    tracker.track(upload);

    expect(tracker.uploads()[0].status).toBe('INDEXADO');
  });

  it('keeps the error message of failed documents', () => {
    tracker.track(upload);

    stream.emit('document-status', statusEvent('ERROR'));

    expect(tracker.uploads()[0]).toMatchObject({ status: 'ERROR', errorMessage: 'PDF corrupto' });
  });

  it('ignores documents uploaded from other tabs or users', () => {
    const finished: TrackedUpload[] = [];
    tracker.finished$.subscribe((item) => finished.push(item));
    tracker.track(upload);

    stream.emit('document-status', statusEvent('INDEXADO', 'someone-else'));

    expect(finished).toEqual([]);
    expect(tracker.uploads()[0].status).toBe('PROCESANDO');
  });

  it('re-reads pending uploads after a reconnection', () => {
    api.getById.mockReturnValue(of({ id: 'doc-1', status: 'INDEXADO', errorMessage: null } as DocumentDetail));
    tracker.track(upload);

    stream.fail();
    stream.emit('connected', {});

    expect(api.getById).toHaveBeenCalledWith('doc-1');
    expect(tracker.uploads()[0].status).toBe('INDEXADO');
  });
});
