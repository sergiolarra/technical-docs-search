import { convertToParamMap } from '@angular/router';
import { paramsFromState, parseTags, stateFromParams } from './search-state';

describe('search state', () => {
  it('reads the search from the URL', () => {
    const state = stateFromParams(
      convertToParamMap({ q: ' docker ', category: 'DevOps', tags: 'Java, rest,java', page: '2', size: '20' }),
    );
    expect(state).toEqual({ q: 'docker', category: 'DevOps', tags: ['java', 'rest'], page: 2, size: 20 });
  });

  it('falls back to defaults for missing or invalid values', () => {
    const state = stateFromParams(convertToParamMap({ page: '-3', size: '1000' }));
    expect(state).toEqual({ q: '', category: '', tags: [], page: 0, size: 10 });
  });

  it('leaves defaults out of the URL', () => {
    expect(paramsFromState({ q: 'docker', category: '', tags: [], page: 0, size: 10 })).toEqual({
      q: 'docker',
      category: null,
      tags: null,
      page: null,
      size: null,
    });
  });

  it('normalizes comma separated tags', () => {
    expect(parseTags(' Java ,, REST, java ')).toEqual(['java', 'rest']);
  });
});
