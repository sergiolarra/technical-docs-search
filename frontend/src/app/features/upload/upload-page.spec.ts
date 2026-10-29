import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { fakeEventSourceFactory } from '../../../testing/fake-event-source';
import { EVENT_SOURCE_FACTORY } from '../../core/events/document-events.service';
import { UploadPage } from './upload-page';

describe('UploadPage tags', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: EVENT_SOURCE_FACTORY, useValue: fakeEventSourceFactory },
      ],
    });
  });

  async function setup() {
    const fixture = TestBed.createComponent(UploadPage);
    await fixture.whenStable();
    const root: HTMLElement = fixture.nativeElement;
    const tagInput = root.querySelector<HTMLInputElement>('mat-chip-grid input')!;
    const type = (text: string) => {
      tagInput.value = text;
      tagInput.dispatchEvent(new Event('input'));
    };
    const chips = () => Array.from(root.querySelectorAll('mat-chip-row')).map((chip) => chip.textContent?.replace('×', '').trim());
    return { fixture, root, tagInput, type, chips };
  }

  it('adds a tag when Enter is pressed', async () => {
    const { fixture, tagInput, type, chips } = await setup();

    type('Docker');
    tagInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', keyCode: 13, bubbles: true }));
    await fixture.whenStable();

    expect(chips()).toEqual(['docker']);
  });

  it('keeps a typed tag when the field loses focus without pressing Enter', async () => {
    const { fixture, tagInput, type, chips } = await setup();

    type('kubernetes');
    tagInput.dispatchEvent(new Event('blur'));
    await fixture.whenStable();

    expect(chips()).toEqual(['kubernetes']);
  });

  it('sends every tag as a repeated form field', async () => {
    const { fixture, root, tagInput, type } = await setup();
    const http = TestBed.inject(HttpTestingController);

    type('docker');
    tagInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', keyCode: 13, bubbles: true }));
    type('helm');
    tagInput.dispatchEvent(new Event('blur'));
    fixture.componentInstance['form'].patchValue({ title: 'Guide', author: 'Ana', category: 'DevOps', version: '1.0' });
    fixture.componentInstance['files'].set([new File(['hello'], 'guide.txt', { type: 'text/plain' })]);
    root.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();

    const request = http.expectOne('/api/documents');
    expect((request.request.body as FormData).getAll('tags')).toEqual(['docker', 'helm']);
  });
});
