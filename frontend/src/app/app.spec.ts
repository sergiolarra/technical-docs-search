import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { FakeEventSource, fakeEventSourceFactory } from '../testing/fake-event-source';
import { App } from './app';
import { EVENT_SOURCE_FACTORY } from './core/events/document-events.service';

describe('App', () => {
  beforeEach(async () => {
    FakeEventSource.instances = [];
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), { provide: EVENT_SOURCE_FACTORY, useValue: fakeEventSourceFactory }],
    }).compileComponents();
  });

  it('shows the main navigation', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const links = [...fixture.nativeElement.querySelectorAll('nav a')].map((a: HTMLElement) => a.textContent?.trim());
    expect(links).toEqual(['Buscar', 'Cargar']);
  });

  it('opens the event stream and shows when it is live', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const indicator = (): string => fixture.nativeElement.querySelector('.live').textContent.trim();

    expect(indicator()).toBe('Reconectando…');

    FakeEventSource.latest().emit('connected', {});
    await fixture.whenStable();
    expect(indicator()).toBe('En vivo');
  });
});
