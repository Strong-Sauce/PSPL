## Bug ID
BUG-XXX

### Date
2026-07-21

### Version
Unknown

### Category
Backend / Spring Boot / Routing / Angular Integration

### Severity
High

### Status
Resolved

### Detection Method
Runtime Logs

### Symptoms
- GET requests to `/products/{id}` returned HTTP 500.
- Spring Boot reported: `Ambiguous handler methods mapped for '/products/{id}'`.
- Product details could not be retrieved.

### Immediate Cause
- Both `ProductController` and `SpaController` registered a GET mapping for `/products/{id}`, causing Spring MVC to be unable to determine the correct handler.

### Root Cause
- Frontend SPA routes and backend REST API endpoints shared the same URL namespace (`/products/**`).
- The SPA fallback controller attempted to handle an Angular route that conflicted with an existing REST endpoint.

### Investigation Summary
- Reviewed the exception message, which explicitly listed both conflicting handler methods.
- Compared the mappings in `ProductController` and `SpaController`.
- Confirmed both resolved to the identical route `/products/{id}`.
- Determined that the issue was caused by overlapping frontend and backend URL namespaces rather than a controller implementation error.

### Fix Implemented
- Moved backend REST endpoints under the `/api` namespace (e.g., `/api/products/**`).
- Updated the Angular API base URL from `http://localhost:8080` to `http://localhost:8080/api`.
- Retained `/products/**` exclusively for Angular client-side routing.

### Files Changed
- `ProductController.java`
- Angular API configuration (base URL/environment/service configuration)
- Any other REST controllers updated to use `/api/**` (if applicable)

### Verification
- Successfully retrieved products using `/api/products/{id}`.
- Angular navigation to `/products/{id}` continued to function correctly.
- Browser refresh on Angular routes no longer conflicted with REST endpoints.
- Confirmed the ambiguous mapping exception no longer occurred.

### Regression Risk
Low

Reason:
- The fix introduces a clear separation between frontend routes and backend API endpoints, reducing the likelihood of future routing conflicts. Only API consumers and configuration required updates.

### Prevention
- Reserve `/api/**` exclusively for backend REST endpoints.
- Reserve all non-API routes for the SPA.
- Prefer a generic SPA fallback that forwards all non-API GET requests to `index.html` to avoid maintaining a manual list of frontend routes.
- Establish a routing convention early in the project to prevent namespace collisions.

### Lessons Learned
- SPA routes and REST endpoints should never share the same URL namespace.
- DispatcherServlet resolves routes before Angular executes, making namespace separation essential.
- Explicit API prefixes improve maintainability, deployment, reverse-proxy configuration, and debugging.

### Related Bugs
None