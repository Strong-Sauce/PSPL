import { HttpInterceptorFn } from '@angular/common/http';

/**
 * credentialsInterceptor adds withCredentials: true to every HTTP request.
 *
 * This tells the browser to include cookies (specifically the JSESSIONID session cookie)
 * when making API calls, even in development when the frontend (localhost:4200) and
 * backend (localhost:8080) are on different origins.
 *
 * Without this, the browser would not send the session cookie and every request
 * would appear to be from a guest.
 */
export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  const modifiedReq = req.clone({ withCredentials: true });
  return next(modifiedReq);
};

