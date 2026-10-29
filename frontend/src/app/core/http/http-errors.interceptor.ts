import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';
import { problemOf } from './problem-detail';

/**
 * Global feedback for errors no screen can fix (backend down, 5xx).
 * 4xx errors are left to each screen, which knows which field or action failed.
 */
export const httpErrorsInterceptor: HttpInterceptorFn = (request, next) => {
  const snackBar = inject(MatSnackBar);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && (error.status === 0 || error.status >= 500)) {
        const message =
          error.status === 0
            ? 'No hay conexión con el servidor'
            : (problemOf(error).detail ?? 'Error inesperado del servidor');
        snackBar.open(message, 'Cerrar', { duration: 6000 });
      }
      return throwError(() => error);
    }),
  );
};
