import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Observable, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * guestGuard prevents already-logged-in users from accessing auth pages
 * (login, signup, forgot-password, reset-password).
 *
 * If the user is already logged in, redirect them to the home page instead.
 */
export const guestGuard: CanActivateFn = (): Observable<boolean> => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Already known to be logged in — redirect to home
  if (authService.currentUser() !== null) {
    void router.navigate(['/']);
    return of(false);
  }

  // Check the backend — maybe there's a session we don't know about yet
  return authService.fetchCurrentUser().pipe(
    map((user) => {
      if (user) {
        // User is actually logged in, send them home
        void router.navigate(['/']);
        return false;
      }
      // Not logged in — allow access to the auth page
      return true;
    })
  );
};

