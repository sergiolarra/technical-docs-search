import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import {
  Observable,
  Subject,
  catchError,
  debounceTime,
  distinctUntilChanged,
  filter,
  finalize,
  map,
  merge,
  of,
  switchMap,
} from 'rxjs';
import { DocumentApiService } from '../../core/api/document-api.service';
import { DocumentEventsService } from '../../core/events/document-events.service';
import { problemOf } from '../../core/http/problem-detail';
import { SearchResponse } from '../../core/models/document.model';
import { HighlightedText } from '../../shared/highlighted-text/highlighted-text';
import { PAGE_SIZES, SearchState, paramsFromState, parseTags, stateFromParams } from './search-state';

@Component({
  selector: 'app-search-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    DatePipe,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    HighlightedText,
  ],
  templateUrl: './search-page.html',
  styleUrl: './search-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(DocumentApiService);
  private readonly events = inject(DocumentEventsService);
  private readonly refreshRequests = new Subject<void>();

  protected readonly pageSizes = [...PAGE_SIZES];
  protected readonly queryControl = new FormControl('', { nonNullable: true });
  protected readonly categoryControl = new FormControl('', { nonNullable: true });
  protected readonly tagsControl = new FormControl('', { nonNullable: true });

  /** The URL is the source of truth: links can be shared, and back/forward or reload keep the search. */
  private readonly state$ = this.route.queryParamMap.pipe(map(stateFromParams));
  protected readonly state = toSignal(this.state$, { requireSync: true });

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly newDocuments = signal(false);

  protected readonly response = toSignal(
    merge(this.state$, this.refreshRequests.pipe(map(() => this.state()))).pipe(
      // switchMap cancels the previous request: an outdated response never overwrites a newer one
      switchMap((state) => this.search(state)),
    ),
    { initialValue: null },
  );

  constructor() {
    // Typing updates the URL after a short pause; the URL change triggers the search
    this.queryControl.valueChanges
      .pipe(debounceTime(300), map((value) => value.trim()), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((q) => this.navigate({ q, page: 0 }));

    merge(this.categoryControl.valueChanges, this.tagsControl.valueChanges)
      .pipe(debounceTime(400), takeUntilDestroyed())
      .subscribe(() =>
        this.navigate({ category: this.categoryControl.value.trim(), tags: parseTags(this.tagsControl.value), page: 0 }),
      );

    // Keeps the inputs in sync when the URL changes from outside (back/forward, shared link)
    effect(() => {
      const state = this.state();
      syncControl(this.queryControl, state.q, (value) => value.trim());
      syncControl(this.categoryControl, state.category, (value) => value.trim());
      syncControl(this.tagsControl, state.tags.join(', '), (value) => parseTags(value).join(', '));
    });

    // HU-04: newly indexed documents may change the current results
    this.events.statusChanges$
      .pipe(
        filter((event) => event.status === 'INDEXADO' && this.state().q !== ''),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.newDocuments.set(true));
  }

  protected onPage(event: PageEvent): void {
    this.navigate({ page: event.pageIndex, size: event.pageSize }, false);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected filterByTag(tag: string): void {
    const tags = this.state().tags;
    if (!tags.includes(tag)) {
      this.navigate({ tags: [...tags, tag], page: 0 }, false);
    }
  }

  protected refresh(): void {
    this.refreshRequests.next();
  }

  private search(state: SearchState): Observable<SearchResponse | null> {
    this.newDocuments.set(false);
    this.error.set(null);
    if (!state.q) {
      this.loading.set(false);
      return of(null);
    }
    this.loading.set(true);
    return this.api.search(state).pipe(
      catchError((error: unknown) => {
        this.error.set(searchErrorMessage(error));
        return of(null);
      }),
      finalize(() => this.loading.set(false)),
    );
  }

  /** Typing replaces the history entry; pagination and tag clicks add one, so Back returns to them. */
  private navigate(changes: Partial<SearchState>, replaceUrl = true): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: paramsFromState({ ...this.state(), ...changes }),
      replaceUrl,
    });
  }
}

/** Only writes when the value really differs, so the caret does not jump while typing. */
function syncControl(control: FormControl<string>, value: string, normalize: (value: string) => string): void {
  if (normalize(control.value) !== value) {
    control.setValue(value, { emitEvent: false });
  }
}

function searchErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status === 400) {
    return problemOf(error).detail ?? 'Invalid search';
  }
  return 'Search is temporarily unavailable. Please try again.';
}
