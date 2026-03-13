import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';
import { Router } from '@angular/router';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notificationService = inject(NotificationService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'Wystąpił błąd. Spróbuj ponownie później.';

      if (error.error instanceof ErrorEvent) {
        errorMessage = `Błąd: ${error.error.message}`;
      } else {
        switch (error.status) {
          case 400:
            errorMessage = 'Nieprawidłowe dane. Sprawdź formularz.';
            break;
          case 401:
            errorMessage = 'Nieprawidłowy email lub hasło.';
            void router.navigate(['/login']);
            break;
          case 403:
            errorMessage = 'Brak uprawnień do tej operacji.';
            void router.navigate(['/no-permissions']);
            break;
          case 404:
            errorMessage = 'Nie znaleziono zasobu.';
            break;
          case 500:
            errorMessage = 'Błąd serwera. Spróbuj ponownie później.';
            break;
          default:
            errorMessage = `Błąd: ${error.message}`;
        }
      }

      notificationService.error(errorMessage);

      return throwError(() => error);
    })
  );
};
