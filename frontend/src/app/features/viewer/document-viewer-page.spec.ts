import { provideHttpClient } from '@angular/common/http';
import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideMarkdown } from 'ngx-markdown';
import { of, throwError } from 'rxjs';
import { FakeEventSource, fakeEventSourceFactory } from '../../../testing/fake-event-source';
import { DocumentApiService } from '../../core/api/document-api.service';
import { DocumentEventsService, EVENT_SOURCE_FACTORY } from '../../core/events/document-events.service';
import { DocumentDetail } from '../../core/models/document.model';
import { DocumentViewerPage } from './document-viewer-page';

describe('DocumentViewerPage', () => {
  const api = { getById: vi.fn() };

  const indexed: DocumentDetail = {
    id: 'doc-1',
    title: 'Manual de despliegue',
    author: 'Luis Gómez',
    category: 'DevOps',
    tags: ['docker'],
    version: '2.3',
    originalFilename: 'manual.txt',
    format: 'TXT',
    contentType: 'text/plain',
    size: 2048,
    status: 'INDEXADO',
    errorMessage: null,
    content: 'Paso 1\nPaso 2',
    createdAt: '2026-10-29T10:00:00Z',
    updatedAt: '2026-10-29T10:00:05Z',
  };

  beforeEach(() => {
    FakeEventSource.instances = [];
    api.getById.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'documents/:id', component: DocumentViewerPage }], withComponentInputBinding()),
        provideHttpClient(),
        provideMarkdown(),
        { provide: DocumentApiService, useValue: api },
        { provide: EVENT_SOURCE_FACTORY, useValue: fakeEventSourceFactory },
      ],
    });
  });

  async function open(id = 'doc-1'): Promise<RouterTestingHarness> {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(`/documents/${id}`, DocumentViewerPage);
    await harness.fixture.whenStable();
    return harness;
  }

  it('shows the metadata and the plain text content', async () => {
    api.getById.mockReturnValue(of(indexed));
    const element = (await open()).routeNativeElement!;

    expect(api.getById).toHaveBeenCalledWith('doc-1');
    expect(element.querySelector('h1')?.textContent).toBe('Manual de despliegue');
    expect(element.querySelector('dl')?.textContent).toContain('DevOps');
    expect(element.querySelector('pre.plain')?.textContent).toBe('Paso 1\nPaso 2');
  });

  it('renders Markdown documents with the Markdown component', async () => {
    api.getById.mockReturnValue(of({ ...indexed, format: 'MARKDOWN', content: '# Guía' }));
    const element = (await open()).routeNativeElement!;

    expect(element.querySelector('markdown')).not.toBeNull();
    expect(element.querySelector('pre.plain')).toBeNull();
  });

  it('shows only a preview of very large documents', async () => {
    api.getById.mockReturnValue(of({ ...indexed, content: 'a'.repeat(100_001) }));
    const harness = await open();
    const element = harness.routeNativeElement!;

    expect(element.querySelector('pre.plain')?.textContent?.length).toBe(100_000);

    (element.querySelector('.truncated button') as HTMLButtonElement).click();
    await harness.fixture.whenStable();
    expect(element.querySelector('pre.plain')?.textContent?.length).toBe(100_001);
  });

  it('shows a not found message for unknown documents', async () => {
    api.getById.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    const element = (await open('missing')).routeNativeElement!;

    expect(element.querySelector('h1')?.textContent).toBe('Document not found');
  });

  it('refreshes itself when a document being processed finishes', async () => {
    api.getById
      .mockReturnValueOnce(of({ ...indexed, status: 'PROCESANDO', content: null }))
      .mockReturnValueOnce(of(indexed));
    const harness = await open();
    const element = harness.routeNativeElement!;
    expect(element.querySelector('.notice')?.textContent).toContain('still being processed');

    TestBed.inject(DocumentEventsService).connect();
    FakeEventSource.latest().emit('document-status', {
      documentId: 'doc-1',
      title: 'Manual de despliegue',
      status: 'INDEXADO',
      errorMessage: null,
      occurredAt: '2026-10-29T10:00:05Z',
    });
    await harness.fixture.whenStable();

    expect(api.getById).toHaveBeenCalledTimes(2);
    expect(element.querySelector('pre.plain')?.textContent).toBe('Paso 1\nPaso 2');
  });
});
