# 07 — Rebuild Blueprint

A clean, beginner-friendly reconstruction plan that **preserves all current functionality** while fixing the audit issues. Same stack (Angular + TypeScript + Spring Boot + Java + Neo4j), no exotic abstractions.

---

## Project Overview

**PSPL AMC** is a post-sale product lifecycle and Annual Maintenance Contract management system. Authenticated users manage **Products** and view **expiring Warranties** on a dashboard, with a graph-based domain (Customer → Sale → Product → Warranty → AMC → AMC Offer) stored in **Neo4j**. Authentication is **session-based** (signup, login, logout, session restore, forgot/reset password).

---

## Functional Requirements (must be preserved)

1. **Auth:** signup (auto-login), login, logout, session restore via `/me`, forgot password (token + email/log), reset password (token + expiry, single-use).
2. **Products:** list (search + paginate), create, view detail, update, delete.
3. **Warranties:** list expiring within 30 days; query warranties by customer (graph traversal).
4. **Customers / Sales / AMCs / AMC Offers:** full CRUD via API (expose UI in rebuild to remove dead code — or explicitly drop if out of scope).
5. **Domain relationships:** ability to record a sale for a customer, attach products to a sale, attach warranties to products, extend warranties with AMCs, base AMCs on offers.
6. **Shell:** navbar, conditional collapsible sidebar (products + expiring warranties), guarded routes, About/Contact static pages, Profile.

---

## Non-Functional Requirements

### Security
- **Keep session auth** (simplest, already working) **but**: enable CSRF protection with a cookie-to-header token (`CookieCsrfTokenRepository`) and have Angular send the `XSRF-TOKEN` header (built-in `HttpClientXsrfModule` support).
- Configure session cookie flags: `HttpOnly`, `Secure` (prod), `SameSite=Lax`.
- Generic auth responses (no user enumeration) on signup/forgot.
- Unique DB constraint on `User.email`; never leak exception messages in 500s.
- Optional but recommended: per-user data ownership (link business data to the creating `User`) for true multi-tenant isolation, or document explicitly that data is shared.
- Basic rate limiting on `/auth/login` and `/auth/forgot-password`.

### Performance
- Server-side pagination + search for `/products` (and lists generally).
- Single source for sidebar/home data (share a service signal cache to avoid duplicate fetches).
- Constrain SDN fetch depth (use projections/DTOs).

### Scalability
- Stateless option documented (JWT) as future path; for now, if scaling horizontally, use a shared session store (e.g. Spring Session + Redis) or sticky sessions.
- DB indexes on hot query fields (`Warranty.warrantyEndDate`).

### Maintainability
- **DTO boundary** for every endpoint (request + response records) — entities never cross the API.
- Consistent naming, lowercase packages, one obvious place for each concern.
- Generic, small reusable pieces (a typed FE CRUD service base, a BE base CRUD service) — only where they reduce real duplication, not for its own sake.

### Accessibility
- Semantic HTML, labels tied to inputs, `aria-*` on icon-only buttons (the hamburger already has `aria-label`), focus states, color-contrast-safe palette.

### Responsiveness
- Mobile-first CSS; sidebar collapses to off-canvas on small screens; card grids reflow.

---

## Recommended Clean Architecture

### Backend (Spring Boot, Java 17)

Layered, DTO-bounded, lowercase packages, **all under a single `/api` prefix**:

```
com.pspl.amc
├─ AmcApplication.java
├─ config/            SecurityConfig, CorsConfig, Neo4jConfig (constraints/indexes), SpaForwardingController
├─ common/            ApiError, GlobalExceptionHandler, ResourceNotFoundException, BadRequestException
├─ auth/              AuthController, AuthService, dto/(SignupRequest, LoginRequest, ...), CurrentUser
├─ user/             User (node), UserRepository
├─ product/          ProductController, ProductService, ProductRepository, Product (node), dto/(ProductRequest, ProductResponse)
├─ warranty/         WarrantyController, WarrantyService, WarrantyRepository, Warranty (node), dto/...
├─ customer/         ... same pattern
├─ sale/             ...
├─ amc/              ...
└─ amcoffer/         ...
```

**Principles**
- **Controller** → validate DTO, call service, return DTO. No business logic.
- **Service** → `@Transactional`, business rules, maps node ↔ DTO (via a small mapper or manual mapping).
- **Repository** → `Neo4jRepository<Node, String>` + explicit Cypher for traversals.
- **Feature-based packages** (vertical slices) so each domain is self-contained and easy to find.
- One unified error envelope; map domain exceptions to status codes centrally.

### Frontend (Angular 21, standalone + signals)

```
src/app/
├─ core/             auth.service, auth.guard, guest.guard, http-error.interceptor, csrf handled by provideHttpClient(withXsrfConfiguration)
├─ shared/           ui components (button, card, alert, paginator, search-box), models/, a generic ApiCrudService<T>
├─ layout/           navbar, sidebar, shell
├─ features/
│   ├─ auth/         login, signup, forgot-password, reset-password (each its own folder)
│   ├─ products/     product-list (home), product-create, product-detail, product.service
│   ├─ warranties/   warranty-list, warranty.service
│   ├─ profile/      profile
│   └─ static/       about, contact
└─ environments/
```

**Principles**
- **Standalone components**, `inject()`, **signals** for state, lazy `loadComponent`.
- **One feature = one folder** (component + template + styles + feature service + types).
- **Shared, reusable UI** (paginator, search box, alert, card) instead of repeating markup.
- **Reactive Forms** for non-trivial forms (validation messages standardised) — template-driven is fine for the tiny ones.
- A typed **base CRUD service** removes the 6 duplicate services.
- Global **HTTP error interceptor** → toast/inline error mapping; remove per-component duplication.
- Drop SSR unless there is a real SEO need (simpler build). If kept, document why.

### Database (Neo4j)
- Keep the graph model (it fits the domain well).
- Add **unique constraints** (`User.email`, each `@Id`), an **index** on `Warranty.warrantyEndDate`, and a startup component that ensures them (`Neo4jConfig` running `CREATE CONSTRAINT/INDEX IF NOT EXISTS`).
- Use **DTO projections** to control fetch depth.

---

## Architecture requirements checklist
- ✅ Beginner friendly: classic controller/service/repository + feature folders; no CQRS, no event sourcing, no hexagonal ceremony.
- ✅ Modular: vertical slices per domain on both ends.
- ✅ Low coupling: DTO boundary; FE types decoupled from node shapes.
- ✅ High cohesion: each folder owns one concern.
- ✅ Reusable components: shared UI + base CRUD services.
- ✅ Clear folder structure: see 08.
- ✅ Consistent naming: see 09.
- ✅ Industry-standard patterns only; ❌ no premature optimization / over-engineering.

---

## Migration order (suggested)
1. Scaffold BE with unified `/api`, DTOs, security (CSRF on), constraints.
2. Port auth (most valuable, already solid) — generic responses, CSRF, cookie flags.
3. Port Product + Warranty (the live UI) with server pagination.
4. Port Customer/Sale/AMC/AMCOffer + relationship endpoints; build the missing UI (or descope).
5. Scaffold FE core/shared/layout; port features one by one.
6. Add tests (unit for services, slice tests for controllers, a few e2e flows).

