# 09 — Coding Standards Guide

Consistent rules for the rebuild. Chosen to match the project's existing modern style (standalone Angular + signals; layered Spring Boot) while fixing the inconsistencies found in the audit.

---

## 1. General
- **One responsibility per file/class/function.**
- **No dead code** — delete, don't comment out (use git history).
- **No magic numbers** — name constants (e.g. `RESET_TOKEN_EXPIRY_MINUTES`, `PAGE_SIZE`).
- Comments explain **why**, not **what**.
- Fail fast and validate at boundaries (DTO validation, guards).

---

## 2. Backend (Java / Spring Boot)

### Packages & naming
- **Packages:** all lowercase, feature-based (`com.pspl.amc.product`).
- **Classes:** PascalCase; suffixes by role: `*Controller`, `*Service`, `*Repository`, `*Request`, `*Response`.
- **Methods:** full words, verb-first: `createProduct`, `updateProduct`, `deleteProduct` (no `createCust`/`updateProd` abbreviations).
- **REST paths:** plural nouns, lowercase, hyphenated, **all under `/api`**: `/api/products`, `/api/amc-offers`, `/api/warranties`.
- **Acronyms:** treat as words in identifiers — `Amc`, `AmcOffer` (class + everywhere), not `AMC`.

### Layering rules
- **Controllers**: only HTTP concerns — accept/return **DTOs**, call one service, no business logic, no entity exposure.
- **Services**: business logic + `@Transactional`; map node ↔ DTO; throw domain exceptions.
- **Repositories**: data access only; explicit `@Query` for traversals.
- **Never** accept or return `@Node` entities from controllers.

### DTOs & validation
- Use Java `record`s for DTOs.
- Validate every request DTO with Jakarta Validation (`@NotBlank`, `@Email`, `@Size`) + `@Valid` in controllers.
- Response DTOs never include secrets (no password hash, no reset token).

### Errors
- One `@RestControllerAdvice` → consistent `ApiError`.
- Map: not-found → 404, bad input → 400, unauthenticated → 401, unexpected → 500 with a **generic** message (log the real cause, don't return it).
- Auth responses must be **non-enumerating** (generic success/fail).

### Security
- Session auth with **CSRF enabled** (cookie→header). Cookie flags `HttpOnly`, `Secure` (prod), `SameSite=Lax`.
- BCrypt for passwords.
- Pin CORS origins from config; `allowCredentials(true)` only with explicit origins.

### Persistence
- Define DB constraints/indexes in a startup config (`IF NOT EXISTS`).
- Prefer DTO projections to bound graph fetch depth.

### Tests
- Unit-test services (mock repositories).
- `@WebMvcTest` for controllers, `@DataNeo4jTest` for repository queries.

---

## 3. Frontend (TypeScript / Angular)

### Structure & naming
- **Standalone components** only; `selector` prefix `app-`.
- File names kebab-case: `product-detail.component.ts`.
- Classes PascalCase with role suffix: `ProductDetailComponent`, `ProductService`, `authGuard`.
- **One component per folder**; always external `templateUrl` + `styleUrl` (no inline templates).
- Feature code under `features/`, singletons under `core/`, reusable UI/types under `shared/`.

### State & reactivity
- Use **signals** for component/service state; `computed` for derivations; avoid manual subscriptions to signals.
- `AuthService` is the single source of truth for auth state.
- Prefer `inject()` over constructor injection for consistency.

### HTTP & services
- One typed service per resource; or extend a generic `ApiCrudService<T>`.
- Build URLs from `environment.apiBaseUrl`.
- A global **HTTP error interceptor** centralises error handling; components show user-friendly messages.
- Let Angular handle CSRF via `withXsrfConfiguration` + send credentials.

### Forms
- Reactive Forms for anything beyond trivial; standardised validation-message rendering.
- Disable submit while in-flight (`submitting` signal), show inline errors.

### Templates
- New control-flow syntax (`@if`, `@for`, `@empty`) with `track`.
- Always `track` a stable id in `@for`.
- Accessibility: `<label for>`, `aria-label` on icon buttons, focus-visible styles.

### Types
- Interfaces in `shared/models`; dates as ISO `string`; optional ids marked `?`.
- Avoid `any`.

---

## 4. CSS
- Mobile-first; consistent design tokens (colors/spacing) — extract to CSS variables in `styles.css`.
- BEM-ish or component-scoped class names; avoid deep selectors.
- Ensure WCAG AA contrast.

---

## 5. Git & commits
- Conventional commits (`feat:`, `fix:`, `refactor:`, `docs:`, `test:`).
- Small, focused PRs; no commented-out code merged.

---

## 6. Definition of Done
- DTO-validated endpoint + service logic + repository.
- Consistent naming & folder placement.
- Error paths handled (FE + BE).
- Accessible, responsive UI.
- At least one test for new service/endpoint.
- No dead code, no leaked secrets, no enumeration.

