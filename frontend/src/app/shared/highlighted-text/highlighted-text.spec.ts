import { TestBed } from '@angular/core/testing';
import { HighlightedText } from './highlighted-text';

describe('HighlightedText', () => {
  beforeEach(() => TestBed.configureTestingModule({ imports: [HighlightedText] }));

  it('wraps only matching fragments in <mark>', async () => {
    const fixture = TestBed.createComponent(HighlightedText);
    fixture.componentRef.setInput('fragments', [
      { text: 'La ', match: false },
      { text: 'búsqueda', match: true },
      { text: ' rápida', match: false },
    ]);
    await fixture.whenStable();

    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toBe('La búsqueda rápida');
    expect([...element.querySelectorAll('mark')].map((mark) => mark.textContent)).toEqual(['búsqueda']);
  });

  it('never renders document text as HTML', async () => {
    const fixture = TestBed.createComponent(HighlightedText);
    fixture.componentRef.setInput('fragments', [{ text: '<img src=x onerror=alert(1)>', match: true }]);
    await fixture.whenStable();

    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('img')).toBeNull();
    expect(element.textContent).toContain('<img');
  });
});
