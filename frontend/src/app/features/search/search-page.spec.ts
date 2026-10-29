import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { FakeEventSource, fakeEventSourceFactory } from '../../../testing/fake-event-source';
import { DocumentApiService } from '../../core/api/document-api.service';
import { DocumentEventsService, EVENT_SOURCE_FACTORY } from '../../core/events/document-events.service';
import { SearchResponse } from '../../core/models/document.model';
import { SearchPage } from './search-page';

describe('SearchPage', () => {
  const api = { search: vi.fn() };

  const response: SearchResponse = {
    query: 'busqueda',
    page: 0,
    size: 10,
    totalHits: 1,
    totalPages: 1,
    tookMs: 12.5,
    results: [
      {
        id: 'doc-1',
        title: 'Guía de búsqueda',
        titleHighlight: [
          { text: 'Guía de ', match: false },
          { text: 'búsqueda', match: true },
        ],
        author: 'Ana',
        category: 'Backend',
        tags: ['java'],
        version: '1.0',
        format: 'MARKDOWN',
        score: 0.42,
        snippet: [
          { text: 'La ', match: false },
          { text: 'búsqueda', match: true },
          { text: ' rápida', match: false },
        ],
        createdAt: '2026-10-29T10:00:00Z',
      },
    ],
  };

  beforeEach(() => {
    FakeEventSource.instances = [];
    api.search.mockReset();
    api.search.mockReturnValue(of(response));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'search', component: SearchPage }]),
        { provide: DocumentApiService, useValue: api },
        { provide: EVENT_SOURCE_FACTORY, useValue: fakeEventSourceFactory },
      ],
    });
  });

  it('searches with the parameters in the URL and highlights matches', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/search?q=busqueda&category=backend&tags=java,rest&page=1&size=20', SearchPage);
    await harness.fixture.whenStable();

    expect(api.search).toHaveBeenCalledWith(
      expect.objectContaining({ q: 'busqueda', category: 'backend', tags: ['java', 'rest'], page: 1, size: 20 }),
    );
    const element = harness.routeNativeElement!;
    expect(element.querySelector('.summary')?.textContent).toContain('1 result');
    expect([...element.querySelectorAll('.result mark')].map((mark) => mark.textContent)).toEqual([
      'búsqueda',
      'búsqueda',
    ]);
  });

  it('shows search tips and does not call the API without a query', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/search', SearchPage);
    await harness.fixture.whenStable();

    expect(api.search).not.toHaveBeenCalled();
    expect(harness.routeNativeElement!.querySelector('.empty')).not.toBeNull();
  });

  it('offers to refresh the results when new documents are indexed', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/search?q=busqueda', SearchPage);
    TestBed.inject(DocumentEventsService).connect();

    FakeEventSource.latest().emit('document-status', {
      documentId: 'doc-2',
      title: 'Nuevo',
      status: 'INDEXADO',
      errorMessage: null,
      occurredAt: '2026-10-29T10:00:00Z',
    });
    await harness.fixture.whenStable();
    const element = harness.routeNativeElement!;
    expect(element.querySelector('.banner')).not.toBeNull();

    (element.querySelector('.banner button') as HTMLButtonElement).click();
    await harness.fixture.whenStable();
    expect(api.search).toHaveBeenCalledTimes(2);
    expect(element.querySelector('.banner')).toBeNull();
  });
});
