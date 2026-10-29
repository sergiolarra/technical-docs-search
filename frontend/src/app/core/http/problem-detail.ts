import { HttpErrorResponse } from '@angular/common/http';

/** RFC 7807 error body returned by the backend. */
export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}

export function problemOf(error: unknown): ProblemDetail {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error === 'object') {
    return error.error as ProblemDetail;
  }
  return { title: 'Error', detail: 'The operation could not be completed' };
}
