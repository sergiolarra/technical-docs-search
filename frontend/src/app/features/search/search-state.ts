import { ParamMap, Params } from '@angular/router';

export const PAGE_SIZES = [10, 20, 50] as const;
const DEFAULT_PAGE_SIZE = 10;

export interface SearchState {
  q: string;
  category: string;
  tags: string[];
  page: number;
  size: number;
}

export function parseTags(text: string): string[] {
  return [...new Set(text.split(',').map((tag) => tag.trim().toLowerCase()).filter(Boolean))];
}

/** Reads the search from the URL; invalid values fall back to defaults instead of failing. */
export function stateFromParams(params: ParamMap): SearchState {
  const page = Number(params.get('page'));
  const size = Number(params.get('size'));
  return {
    q: (params.get('q') ?? '').trim(),
    category: (params.get('category') ?? '').trim(),
    tags: parseTags(params.get('tags') ?? ''),
    page: Number.isInteger(page) && page > 0 ? page : 0,
    size: (PAGE_SIZES as readonly number[]).includes(size) ? size : DEFAULT_PAGE_SIZE,
  };
}

/** Defaults are left out so URLs stay short; null removes the parameter. */
export function paramsFromState(state: SearchState): Params {
  return {
    q: state.q || null,
    category: state.category || null,
    tags: state.tags.length ? state.tags.join(',') : null,
    page: state.page > 0 ? state.page : null,
    size: state.size !== DEFAULT_PAGE_SIZE ? state.size : null,
  };
}
