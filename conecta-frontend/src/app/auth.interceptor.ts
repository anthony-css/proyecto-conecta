import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { environment } from '../environments/environment';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(`${environment.apiUrl}/admin/`)) return next(request);

  const auth = inject(AuthService);
  const router = inject(Router);
  const authorization = auth.authorization();
  if (!authorization) return next(request);

  return next(request.clone({ setHeaders: { Authorization: authorization } })).pipe(
    tap({
      error: (error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 401 &&
            auth.authorization() === authorization) {
          auth.logout();
          void router.navigate(['/login'], { queryParams: { motivo: 'sesion' } });
        }
      },
    }),
  );
};
