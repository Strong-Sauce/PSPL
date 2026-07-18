# 10 — FINAL PROMPT (Self-Contained AI Rebuild Brief)

> Give the entire prompt below to an AI coding agent. It is **self-contained**: it does not require access to the original source code. It preserves all existing functionality while applying a cleaner architecture.

---

## ROLE
You are a senior full-stack engineer. Build a production-quality web application called **PSPL AMC — Post-Sale Product Lifecycle & Annual Maintenance Contract Management System** from scratch, using the exact stack, behavior, and architecture specified below. Write clean, beginner-friendly, modular code with low coupling and high cohesion. Do not over-engineer. Implement everything; do not leave TODOs.

## TECH STACK (mandatory)
- **Frontend:** Angular 21 (standalone components, signals, functional guards/interceptors, lazy-loaded routes), TypeScript, HTML, CSS. Use `inject()` DI. New control flow (`@if`/`@for`/`@empty`). Do **not** enable SSR.
- **Backend:** Spring Boot 4 (Java 17), Spring Web (MVC), Spring Security, Spring Validation, Spring Data Neo4j, Spring Mail, Lombok.
- **Database:** Neo4j (graph), accessed via Spring Data Neo4j `@Node`/`@Relationship`/`Neo4jRepository`.
- **Auth:** server-side HTTP session (`JSESSIONID` cookie) + BCrypt. **No JWT.** Enable CSRF (cookie→header).
- **Build:** Maven (backend), Angular CLI (frontend).

## DOMAIN MODEL (Neo4j graph)
Nodes (all ids are `String`, `@Id @GeneratedValue`):
- **User** (auth only, no graph relationships): `id, name, email (login, store lowercase), password (BCrypt hash), createdAt, updatedAt, resetToken?, resetTokenExpiresAt?`.
- **Customer**: `custId, custName`, rel `-[:PURCHASED]->(Sale)` (list).
- **Sale**: `saleId, saleDate (LocalDate)`, rel `-[:OF_PRODUCT]->(Product)` (list).
- **Product**: `productId, productName, productSerialNumber`, rel `-[:HAS_WARRANTY]->(Warranty)` (list).
- **Warranty**: `warrantyId, warrantyStartDate (LocalDate), warrantyEndDate (LocalDate)`, rel `-[:EXTENDED_BY]->(AMC)` (list).
- **AMC**: `amcId, amcStartDate (LocalDate), amcEndDate (LocalDate)`, rel `-[:BASED_ON]->(AMCOffer)` (list).
- **AMCOffer**: `offerId, offerType (e.g. "Silver"/"Gold"), offerDurationMonths (Integer), offerPrice (Double), offerTerms (String)`.

Relationship chain (all OUTGOING, no relationship properties):
`Customer -[:PURCHASED]-> Sale -[:OF_PRODUCT]-> Product -[:HAS_WARRANTY]-> Warranty -[:EXTENDED_BY]-> AMC -[:BASED_ON]-> AMCOffer`

**DB setup:** on startup create (IF NOT EXISTS) a **unique constraint on `User.email`**, a unique constraint on `User.resetToken`, and an **index on `Warranty.warrantyEndDate`**.

## ARCHITECTURE RULES
- **Backend:** feature-based packages `com.pspl.amc.{config,common,auth,user,product,warranty,customer,sale,amc,amcoffer}`. Layering: Controller (HTTP + DTOs only) → Service (`@Transactional`, business logic, node↔DTO mapping) → Repository. **Never expose `@Node` entities through controllers — use request/response DTO `record`s.** All REST endpoints under **`/api`**, plural kebab-case nouns.
- **Frontend:** `core/` (auth service+guards, http-error interceptor, generic `ApiCrudService<T>`), `shared/` (reusable UI: alert, paginator, search-box, card, button; and `models/`), `layout/` (navbar, sidebar), `features/` (auth, products, warranties, profile, static). One component per folder, external template+style files. Signals for state. Lazy `loadComponent`.
- Consistent naming: full-word verb methods (`createProduct`), acronyms as words (`Amc`, `AmcOffer`). No dead code. No `any`.

## AUTHENTICATION — implement exactly this behavior
Endpoints under `/api/auth`:
1. `POST /signup` — body `{name(2–100), email(valid), password(8–100)}`. Reject duplicate email with a **generic** message ("Unable to create account") — do not reveal existence. Hash password with BCrypt. Create session (auto-login). Return 201 `{message, user:{id,name,email}}`.
2. `POST /login` — body `{email, password}`. On invalid email OR password return 400 generic "Invalid credentials". On success create session, return 200 `{message, user}`.
3. `GET /me` — return current user `{id,name,email}` from session, else 401 (JSON body).
4. `POST /forgot-password` — body `{email}`. Always return 200 with a **generic** message ("If that email exists, a reset link has been sent") regardless of whether the user exists. If user exists: generate a cryptographically secure URL-safe token (32 random bytes, Base64 no padding), set `resetToken` + `resetTokenExpiresAt = now+30min`, send email via Spring Mail with a link `${app.base-url}/reset-password?token=...`; if mail is not configured, **log the URL** instead.
5. `POST /reset-password` — body `{token, newPassword(8–100)}`. Validate token exists and not expired; set new BCrypt password; clear token (single-use). 400 on invalid/expired token. 200 on success.
6. `POST /logout` — invalidate session + clear security context. 200 `{message}`.

**Security config:** CSRF **enabled** via `CookieCsrfTokenRepository.withHttpOnlyFalse()` (Angular auto-sends `X-XSRF-TOKEN`). Disable HTTP Basic & form login. `SessionCreationPolicy.IF_REQUIRED`. Custom `AuthenticationEntryPoint` returning JSON 401. Public: `/api/auth/signup|login|forgot-password|reset-password` + OPTIONS. Authenticated: all other `/api/**` business endpoints. Permit static assets and SPA forwards. Session cookie: `HttpOnly`, `SameSite=Lax`, `Secure` in prod. Password encoder: `BCryptPasswordEncoder`. Grant a single `ROLE_USER`.

## BUSINESS API — implement all (DTO in/out, validated)
For each resource use plural kebab path under `/api`. Standard CRUD unless noted.
- **Products** `/api/products`: `GET` (list, **server-side pagination + search** by name/serial via query params `page,size,q`), `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`. 404 when missing.
- **Warranties** `/api/warranties`: `GET /expiring` (warranties with `warrantyEndDate <= date()+P30D`), `GET /by-customer/{customerId}` (graph traversal `Customer->Sale->Product->Warranty`), plus `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`.
- **Customers** `/api/customers`: full CRUD.
- **Sales** `/api/sales`: `POST`, `GET /by-customer/{customerId}` (Cypher `Customer-[:PURCHASED]->Sale`), `GET /{id}`. **Do not** create GET-with-body endpoints.
- **AMCs** `/api/amcs`: full CRUD.
- **AMC Offers** `/api/amc-offers`: full CRUD.
- **Relationship endpoints** (to make the domain usable from the UI): `POST /api/customers/{customerId}/sales` (record a sale for a customer), `POST /api/sales/{saleId}/products` (attach product(s)), `POST /api/products/{productId}/warranties` (attach a warranty), `POST /api/warranties/{warrantyId}/amcs`, `POST /api/amcs/{amcId}/offers`. Each creates the relationship described in the domain model.

**Error handling:** one `@RestControllerAdvice` → consistent envelope `{timestamp,status,error,message,path}`. Not found → 404; validation/bad input → 400 (first field message); unauthenticated → 401; uncaught → 500 with a **generic** message (log the real exception, never return `ex.getMessage()`).

## FRONTEND — pages & behavior (preserve all of this)
Routing (`app.routes.ts`), all lazy:
- Guest-guarded (redirect logged-in users to `/`): `/login`, `/signup`, `/forgot-password`.
- No guard: `/reset-password` (reads `?token=`).
- Auth-guarded (redirect to `/login` if not logged in): `/` (dashboard), `/products/new`, `/products/:id`, `/warranties/:id`, `/about`, `/contact`, `/profile`.
- `**` → redirect `/`.

Guards & auth:
- `AuthService` holds `currentUser = signal<User|null>` and `isAuthenticated = computed`. On app boot call `GET /api/auth/me` to restore session. `authGuard` allows if user set, else fetches `/me`. `guestGuard` redirects logged-in users away from auth pages.
- HTTP: `provideHttpClient(withFetch(), withXsrfConfiguration(...), withInterceptors([httpErrorInterceptor]))` and send credentials (cookies) on every request. A global error interceptor surfaces server messages.

Pages:
- **Login / Signup / Forgot / Reset:** card forms, signal fields, `submitting` disable, inline error/success alerts, server-message display, redirects (signup/login → `/`; reset → success then `/login` after 2s).
- **Dashboard (`/`)**: two sections — **Products** and **Expiring Warranties** — each with a search box and pagination (6 per page). Product cards link to `/products/:id`; "Add Product" → `/products/new`. (Server pagination is preferred; the original did it client-side — either is acceptable but keep search + 6/page UX.)
- **Product create:** name + serial, validate, `POST`, navigate to new detail page.
- **Product detail:** load by id; inline edit (name+serial) via `PUT`; delete via confirm → `DELETE` → home; show associated warranties.
- **Warranty detail (`/warranties/:id`)**: NEW page (the original had a broken link) — show a warranty and its AMC extensions.
- **Profile:** show `{name,email,id}` + logout.
- **About / Contact:** static info pages.

Layout/shell:
- **Navbar:** logo, center nav (Home/About/Contact) when authenticated, profile link + logout when authenticated else Login/Sign Up; emits a `toggleSidebar` output; hamburger has `aria-label`.
- **Sidebar:** collapsible; lists products (link `/products/:id`) and expiring warranties (link `/warranties/:id`); loaded from services. Show only when authenticated and not on an auth page (`computed`).

Models (`shared/models`): `User, Product, Customer, Sale, Warranty, Amc, AmcOffer` + auth DTO interfaces. Dates as ISO `string`. Optional ids `?`.

## NON-FUNCTIONAL REQUIREMENTS
- **Security:** CSRF on, BCrypt, generic auth messages (no user enumeration), unique email constraint, no secret/exception leakage, pinned CORS via env, rate-limit `/auth/login` and `/auth/forgot-password` (simple in-memory limiter is fine).
- **Performance:** server-side pagination/search for lists; index on `warrantyEndDate`; avoid duplicate fetches (share data via a service signal where sidebar+dashboard overlap).
- **Scalability:** stateless-ready code; document that horizontal scaling needs sticky sessions or shared session store.
- **Maintainability:** DTO boundaries, feature folders, generic CRUD base (BE service base + FE `ApiCrudService<T>`) only where it removes real duplication.
- **Accessibility:** labels tied to inputs, `aria-*` on icon buttons, focus-visible, AA contrast.
- **Responsiveness:** mobile-first; sidebar becomes off-canvas on small screens; card grids reflow.

## CONFIGURATION
- Backend `application.yml` (env-driven): `server.port=${PORT:8080}`, Neo4j `uri/username/password` from `NEO4J_*`, `app.cors.allowed-origins` (default `http://localhost:4200`), `app.base-url`, `app.mail.from`, optional `spring.mail.*`. If mail unconfigured, log reset URL.
- Frontend `environment.ts`/`environment.prod.ts` with `apiBaseUrl` (empty = same origin). Dev `proxy.conf.json` forwarding `/api` → `http://localhost:8080`.
- **SPA refresh:** a single backend catch-all `@GetMapping` (excluding `/api/**` and static asset paths) forwards to `index.html` so deep-link refresh works.

## DELIVERABLES
1. Complete, runnable backend (Maven) and frontend (Angular CLI) projects with the folder structures above.
2. All endpoints, pages, guards, interceptors, DTOs, entities, repositories, security, exception handling, and DB constraints/indexes implemented.
3. Seed/utility for local dev (optional): a small CommandLineRunner that creates sample data demonstrating the full graph chain.
4. Unit tests for services, `@WebMvcTest` controller slices, and `@DataNeo4jTest` for custom queries.
5. A `README.md` documenting environment variables and how to run dev (proxy) and build prod (single-origin).

## ACCEPTANCE CRITERIA (must all pass)
- A user can sign up, be auto-logged-in, refresh the page and stay logged in, log out, and reset a forgotten password (link logged to console when mail is off).
- Unauthenticated access to any `/api` business endpoint returns JSON 401; auth pages redirect logged-in users to `/`.
- Products: list with search + 6/page pagination, create, view, edit, delete — all working end-to-end.
- Expiring warranties (≤30 days) appear on dashboard and sidebar; `/warranties/:id` page works (no broken links).
- All Customer/Sale/AMC/AMC-Offer CRUD and the relationship endpoints work and have at least minimal UI or are reachable/tested.
- CSRF is enforced (state-changing requests require the token); no user enumeration; no exception messages leaked; `User.email` is unique at the DB level.
- Consistent naming, feature-based folders, DTO boundaries, no dead code.

Build it now, file by file, and ensure both apps compile and run.

