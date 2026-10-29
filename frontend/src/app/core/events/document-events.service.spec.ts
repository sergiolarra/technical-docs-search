import { TestBed } from '@angular/core/testing';
import { FakeEventSource, fakeEventSourceFactory } from '../../../testing/fake-event-source';
import { DocumentStatusEvent } from '../models/document.model';
import { DocumentEventsService, EVENT_SOURCE_FACTORY } from './document-events.service';

describe('DocumentEventsService', () => {
  let service: DocumentEventsService;

  const indexed: DocumentStatusEvent = {
    documentId: '1',
    title: 'Guía',
    status: 'INDEXADO',
    errorMessage: null,
    occurredAt: '2026-10-29T10:00:00Z',
  };

  beforeEach(() => {
    FakeEventSource.instances = [];
    TestBed.configureTestingModule({
      providers: [{ provide: EVENT_SOURCE_FACTORY, useValue: fakeEventSourceFactory }],
    });
    service = TestBed.inject(DocumentEventsService);
  });

  it('opens a single stream to /api/events', () => {
    service.connect();
    service.connect();

    expect(FakeEventSource.instances.length).toBe(1);
    expect(FakeEventSource.latest().url).toBe('/api/events');
  });

  it('emits document status changes', () => {
    const received: DocumentStatusEvent[] = [];
    service.statusChanges$.subscribe((event) => received.push(event));
    service.connect();

    FakeEventSource.latest().emit('document-status', indexed);

    expect(received).toEqual([indexed]);
  });

  it('tracks the connection state', () => {
    service.connect();
    expect(service.connected()).toBe(false);

    FakeEventSource.latest().emit('connected', {});
    expect(service.connected()).toBe(true);

    FakeEventSource.latest().fail();
    expect(service.connected()).toBe(false);
  });

  it('signals reconnections but not the first connection', () => {
    let reconnections = 0;
    service.reconnected$.subscribe(() => reconnections++);
    service.connect();

    FakeEventSource.latest().emit('connected', {});
    expect(reconnections).toBe(0);

    FakeEventSource.latest().fail();
    FakeEventSource.latest().emit('connected', {});
    expect(reconnections).toBe(1);
  });
});
