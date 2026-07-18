# 01 — Complete Project Analysis

> **Project:** PSPL — Post-Sale Product Lifecycle & AMC Management System
> **Stack:** Angular 21 (standalone, SSR) · Spring Boot 4 · Java 17 · Neo4j · Spring Security (session-based) · Lombok
> **Module / artifact:** `com.postSale:PSPLProject:0.0.1-SNAPSHOT`

This document is a full, evidence-based audit of the codebase **as it exists today**. Nothing here is changed or refactored — it only describes what is present.

---

## 1. High-Level Picture

The application is a small CRUD + dashboard app for tracking **products, warranties, sales, customers, AMCs (Annual Maintenance Contracts) and AMC offers**, built on a **Neo4j graph database**. It has a full **authentication subsystem** (signup, login, logout, session restore, forgot/reset password) built on **server-side HTTP sessions** (not JWT, despite the request mentioning JWT — see note below).

Two deployment shapes are supported:
- **Development:** Angular dev server on `:4200` proxying API calls to Spring Boot on `:8080` (via `proxy.conf.json`).
- **Production:** Angular is built and served as static files from the same origin as Spring Boot (so `apiBaseUrl` is empty and SPA refresh routing is handled by `SpaController`).

> **JWT note:** The task brief lists "JWT flow / token refresh". **There is no JWT in this codebase.** Authentication is implemented with Spring Security `HttpSession` + `JSESSIONID` cookie + a `UsernamePasswordAuthenticationToken` stored in the session. This is documented accurately throughout these deliverables.

---

## 2. Backend Analysis (Spring Boot)

### 2.1 Package layout (`com.postSale.amcProject`)

| Package | Contents |
|---|---|
| `(root)` | `AmcProjectApplication` (entry point, `@EnableNeo4jRepositories`) |
| `config` | `SecurityConfig`, `WebConfig` (CORS), `SpaController` (SPA refresh forwarding), `RestAuthenticationEntryPoint` (JSON 401) |
| `controllers` | `AuthController`, `ProductController`, `CustomerController`, `SaleController`, `WarrantyController`, `AMCController`, `AMCOfferController` |
| `Services` | `AuthService`, `EmailService`, `ProductService`, `CustomerService`, `SaleService`, `WarrantyService`, `AMCService`, `AMCOfferService` |
| `Repositories` | `UserRepository`, `ProductRepository`, `CustomerRepository`, `SaleRepository`, `WarrantyRepository`, `AMCRepository`, `AMCOfferRepository` |
| `Model.nodes` | `User`, `Customer`, `Product`, `Sale`, `Warranty`, `AMC`, `AMCOffer` |
| `Model.Relationships` | `PURCHASED`, `OF_PRODUCT`, `HAS_WARRANTY`, `EXTENDED_BY`, `BASED_ON` — **all fully commented out (dead files)** |
| `Model.dto.auth` | `SignupRequest`, `LoginRequest`, `ForgotPasswordRequest`, `ResetPasswordRequest`, `AuthResponse`, `AuthUserResponse`, `MessageResponse` |
| `Exceptions` | `GlobalExceptionHandler` (`@RestControllerAdvice`), `ResourceNotFoundException` |

> **Naming inconsistency:** package names mix conventions — `config`, `controllers` (lowercase plural) vs `Services`, `Repositories`, `Model`, `Exceptions` (Capitalized). Java convention is all-lowercase package names.

### 2.2 Controllers

All controllers are thin and delegate to a service. URL prefixes are inconsistent (`/api/auth` for auth, but bare `/products`, `/customers`, … for business resources — no `/api` prefix).

| Controller | Base path | Endpoints |
|---|---|---|
| `AuthController` | `/api/auth` | `POST /signup`, `POST /login`, `GET /me`, `POST /forgot-password`, `POST /reset-password`, `POST /logout` |
| `ProductController` | `/products` | `POST /`, `PUT /`, `GET /`, `GET /{id}`, `DELETE /{id}` |
| `CustomerController` | `/customers` | `POST /`, `PUT /`, `GET /`, `GET /{id}`, `DELETE /{id}` |
| `SaleController` | `/sales` | `GET /{id}` (sales for a customer), `POST /`, `GET /` (takes a `Customer` in the **body of a GET** — anti-pattern) |
| `WarrantyController` | `/warranty` | `GET /` (expiring soon), `GET /{id}` (by customer id) |
| `AMCController` | `/amcs` | `POST /`, `GET /`, `GET /{id}`, `PUT /`, `DELETE /{id}` |
| `AMCOfferController` | `/amc-offers` | `POST /`, `GET /`, `GET /{id}`, `PUT /`, `DELETE /{id}` |

See **03-api-documentation.md** for full request/response contracts.

### 2.3 Services

- **CRUD services** (`Product`, `Customer`, `AMC`, `AMCOffer`) follow an identical template: constructor-injected repository, `@Transactional` writes, `@Transactional(readOnly=true)` reads, `existsById` guard before update/delete throwing `ResourceNotFoundException`. This is repetitive (see audit).
- **`SaleService`** uses a custom Cypher query to find a customer's sales; `getAllSalesOfCustomer(Customer)` is questionable (uses `findAllById` on a single customer id — semantically wrong, returns Sales by saleId not by customer).
- **`WarrantyService`** exposes "expiring soon" (≤ 30 days) and "by customer id" reads only — no create/update.
- **`AuthService`** is the largest and most important service: signup, login, current-user, forgot/reset password, logout, plus private helpers (`createSession`, `normalizeEmail`, `toDto`, `generateSecureToken`). It manually builds a Spring Security context and persists it into the HTTP session.
- **`EmailService`** sends reset emails via `JavaMailSender` if configured, otherwise logs the reset URL to the console. `JavaMailSender` is `@Autowired(required=false)` so the app boots without mail config.

### 2.4 Repositories

All extend `Neo4jRepository<T, String>`. Custom queries:
- `UserRepository`: `findByEmail`, `existsByEmail`, `findByResetToken` (derived queries).
- `SaleRepository`: `findAllSalesByCustomerId(customerId)` — Cypher `MATCH (c:Customer)-[:PURCHASED]->(s:Sale)`.
- `WarrantyRepository`: `findWarrantiesExpiringSoon()` (≤ `date() + P30D`) and `findWarrantiesByCustomerId(custId)` — Cypher traversal `Customer→Sale→Product→Warranty`.
- The rest (`Product`, `Customer`, `AMC`, `AMCOffer`) are empty marker interfaces.

### 2.5 Security / config

- **`SecurityConfig`** — CSRF disabled, CORS enabled (delegated to `WebConfig`), HTTP Basic & form login disabled, `SessionCreationPolicy.IF_REQUIRED`, custom JSON `RestAuthenticationEntryPoint` for 401. Public: OPTIONS + the four unauthenticated auth endpoints. Authenticated: all six business resource prefixes. Everything else permitted (static files, `/api/auth/me`, `/api/auth/logout`).
- **`WebConfig`** — CORS mapping for `/**`, allowed origins from `app.cors.allowed-origins`, credentials allowed.
- **`SpaController`** — `@GetMapping` listing every known Angular route, forwarding to `index.html` so browser refresh works. `/api/**` is intentionally excluded.
- **`RestAuthenticationEntryPoint`** — writes a hand-built JSON 401 body.

### 2.6 Exception handling

`GlobalExceptionHandler` maps:
- `ResourceNotFoundException` → 404
- `IllegalArgumentException` → 400 (used for "Email already exists", "Invalid credentials", "Email not found", "Invalid/Expired reset token")
- `MethodArgumentNotValidException` → 400 (first field error message)
- `NoSuchElementException` → 404
- `Exception` (catch-all) → 500 (leaks `ex.getMessage()` — minor info disclosure)

### 2.7 Configuration (`application.properties`)

- Port from `${PORT:8080}`.
- Neo4j URI/username/password from env vars (no defaults → app fails fast if unset).
- CORS origins, base URL, mail-from all env-driven with sensible local defaults.
- Mail is optional/commented.
- Logging: `org.springframework.data.neo4j.cypher=DEBUG`.

---

## 3. Frontend Analysis (Angular 21)

### 3.1 Architecture style

- **Standalone components everywhere** (no NgModules).
- **Signals** for all local/reactive state (`signal`, `computed`).
- **Functional guards & interceptor** (`CanActivateFn`, `HttpInterceptorFn`).
- **Lazy-loaded routes** via `loadComponent`.
- **SSR enabled** (`@angular/ssr`, `server.ts`, `app.config.server.ts`) but all routes render `RenderMode.Client`.
- `inject()`-based DI rather than constructor params.

### 3.2 Routing (`app.routes.ts`)

| Path | Guard | Component (lazy) |
|---|---|---|
| `/login` | `guestGuard` | `LoginComponent` |
| `/signup` | `guestGuard` | `SignupComponent` |
| `/forgot-password` | `guestGuard` | `ForgotPasswordComponent` |
| `/reset-password` | none | `ResetPasswordComponent` |
| `/` | `authGuard` | `HomeComponent` |
| `/products/new` | `authGuard` | `ProductCreateComponent` |
| `/products/:id` | `authGuard` | `ProductDetailComponent` |
| `/about` | `authGuard` | `AboutComponent` |
| `/contact` | `authGuard` | `ContactComponent` |
| `/profile` | `authGuard` | `ProfileComponent` |
| `**` | — | redirect → `/` |

### 3.3 State management

There is **no NgRx / store**. State is held in services using signals:
- **`AuthService`** is the single source of truth for auth: `currentUser = signal<User|null>` and `isAuthenticated = computed(...)`. Login/signup set the signal; logout clears it; `fetchCurrentUser()` restores it from `/api/auth/me`.
- Feature components hold their own signals (lists, search, pagination, edit state).

### 3.4 Services (one per backend resource)

`AuthService`, `ProductService`, `CustomerService`, `SaleService`, `WarrantyService`, `AmcService`, `AmcOfferService`. All use `HttpClient`, build their URL from `environment.apiBaseUrl + '/resource'`, and return `Observable<T>`. Re-exported via `services/index.ts` barrel.

### 3.5 Guards & interceptor

- **`authGuard`** — allow if `currentUser()` set, else `fetchCurrentUser()`; redirect to `/login` if no session.
- **`guestGuard`** — redirect logged-in users to `/`; else allow.
- **`credentialsInterceptor`** — clones every request with `withCredentials: true` so the `JSESSIONID` cookie is sent cross-origin in dev.

### 3.6 Models / interfaces

Mirror the backend nodes: `User`, `Product`, `Customer`, `Sale`, `Warranty`, `AMC`, `AMCOffer`, plus auth DTO interfaces (`SignupRequest`, `LoginRequest`, `ForgotPasswordRequest`, `ResetPasswordRequest`, `AuthResponse`, `MessageResponse`). Dates are typed as `string` (ISO). Optional `id` and nested relation arrays. Re-exported via `models/index.ts`.

### 3.7 Components (purpose / inputs / outputs / deps / APIs / logic)

| Component | Purpose | Inputs | Outputs | Key deps | APIs consumed | Business logic |
|---|---|---|---|---|---|---|
| `App` (root) | Shell: navbar + conditional sidebar + outlet | — | — | `AuthService`, `Router` | (via auth init) `GET /api/auth/me` | Restores session on boot; computes `showSidebar` (logged-in & not on auth page); toggles sidebar collapse |
| `NavbarComponent` | Top bar, nav links, profile/logout or login/signup | — | `toggleSidebar: output<void>` | `AuthService` | `POST /api/auth/logout` | Reactive `isAuthenticated`/`currentUser`; emits sidebar toggle; logout |
| `SidebarComponent` | Lists products & expiring warranties | `collapsed: input<boolean>` | — | `ProductService`, `WarrantyService` | `GET /products`, `GET /warranty` | Loads two lists on init; collapsible sections (note: warranty links to `/warranties/:id` which **has no route**) |
| `HomeComponent` | Dashboard: products + expiring warranties with search + client pagination | — | — | `ProductService`, `WarrantyService` | `GET /products`, `GET /warranty` | Client-side filter (`computed`), 6/page pagination, search resets page |
| `ProductCreateComponent` | Create a product | — | — | `ProductService`, `Router` | `POST /products` | Validates name+serial; on success navigates to detail page |
| `ProductDetailComponent` | View / inline-edit / delete a product + show warranties | — (reads `:id` from route) | — | `ProductService`, `ActivatedRoute`, `Router` | `GET /products/{id}`, `PUT /products`, `DELETE /products/{id}` | Load by id, edit toggle, save (spread + override), delete with `confirm()` |
| `LoginComponent` | Email/password login | — | — | `AuthService`, `Router` | `POST /api/auth/login` | Client validation, submit, error from `err.error.message`, redirect `/` |
| `SignupComponent` | Register | — | — | `AuthService`, `Router` | `POST /api/auth/signup` | Same pattern as login |
| `ForgotPasswordComponent` | Request reset email | — | — | `AuthService` | `POST /api/auth/forgot-password` | Shows success/error message |
| `ResetPasswordComponent` | Set new password using token | — (reads `?token=`) | — | `AuthService`, `ActivatedRoute`, `Router` | `POST /api/auth/reset-password` | Reads token from query, submits, redirect to `/login` after 2s |
| `ProfileComponent` | Show user info + logout | — | — | `AuthService` | `POST /api/auth/logout` | Inline template/styles; reads `currentUser` signal |
| `AboutComponent` | Static about page | — | — | — | — | Inline template |
| `ContactComponent` | Static contact page | — | — | — | — | Inline template |

---

## 4. Cross-Cutting Observations

- **Inconsistent API prefixes:** `/api/auth/**` vs `/products`, `/customers`, etc. Frontend `proxy.conf.json` must therefore proxy 7 separate path prefixes.
- **Dead code:** all five `Model/Relationships/*.java` files are entirely commented out.
- **The graph is write-shallow:** Controllers accept whole node graphs as JSON (`@RequestBody Product`, `Sale`, etc.) and `save()` them directly — there is no DTO layer for business entities and no service to *create the relationships* between Customer→Sale→Product→Warranty→AMC→AMCOffer beyond whatever the client posts. The warranty "by customer" query assumes those relationships exist, but no endpoint explicitly builds them.
- **No tests of substance** (only the default generated test scaffolding under `src/test`).
- **No pagination/filtering on the backend** — Home does it all client-side after fetching everything.

See **06-architecture-audit.md** for the full issue catalogue.

