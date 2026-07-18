import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Observable, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * authGuard protects pages that require the user to be logged in.
 *
 * Flow:
 * 1. If user signal is already set (e.g. just logged in) → allow
 * 2. Otherwise call /api/auth/me to check for an existing session
 * 3. If session exists → allow
 * 4. If not → redirect to /login
 */
export const authGuard: CanActivateFn = (): Observable<boolean> => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Already known to be logged in — allow immediately
  if (authService.currentUser() !== null) {
    return of(true);
  }

  // Try to restore session from backend
  return authService.fetchCurrentUser().pipe(
    map((user) => {
      if (user) {
        return true; // Session valid
      }
      // Not logged in — redirect to login page
      void router.navigate(['/login']);
      return false;
    })
  );
};

