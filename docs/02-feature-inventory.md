# 02 — Complete Feature Inventory

Each feature lists: **Description · Components involved · APIs involved · Database entities involved.**

> Legend: 🟢 fully working end-to-end · 🟡 partial / has a gap · 🔴 referenced but broken/missing.

---

## A. Authentication & Account

### A1. Sign up 🟢
- **Description:** Create an account with name, email, password. Password is BCrypt-hashed. User is auto-logged-in (session created) on success.
- **Components:** `SignupComponent`, `AuthService` (FE), `AuthController.signup`, `AuthService.signup` (BE).
- **APIs:** `POST /api/auth/signup`.
- **Entities:** `User` (created).
- **Edge cases handled:** duplicate email → 400 "Email already exists"; bean validation (name 2–100, valid email, password ≥ 8).

### A2. Log in 🟢
- **Description:** Authenticate by email + password, create server session.
- **Components:** `LoginComponent`, `AuthService` / `AuthController.login`, `AuthService.login`.
- **APIs:** `POST /api/auth/login`.
- **Entities:** `User` (read).
- **Edge cases:** wrong email or password → generic 400 "Invalid credentials" (no user enumeration on login).

### A3. Session restore on refresh 🟢
- **Description:** On app boot and inside guards, `GET /api/auth/me` rehydrates `currentUser` from the `JSESSIONID` session cookie so a refresh doesn't log the user out.
- **Components:** `App` (`initSession`), `authGuard`, `guestGuard`, `AuthService.fetchCurrentUser` / `AuthController.me`.
- **APIs:** `GET /api/auth/me`.
- **Entities:** `User` (read).
- **Edge cases:** no session → 401 → `currentUser` set to null (treated as guest).

### A4. Log out 🟢
- **Description:** Invalidate the HTTP session, clear security context, clear FE signal, redirect to `/login`.
- **Components:** `NavbarComponent`, `ProfileComponent`, `AuthService.logout` / `AuthController.logout`.
- **APIs:** `POST /api/auth/logout`.
- **Entities:** none.
- **Edge cases:** API failure still clears FE state and redirects.

### A5. Forgot password 🟡
- **Description:** Generate a secure random token (32-byte URL-safe Base64, 30-min expiry), store it on the `User`, email a reset link (or log it if mail unconfigured).
- **Components:** `ForgotPasswordComponent`, `AuthService.forgotPassword` / `EmailService`.
- **APIs:** `POST /api/auth/forgot-password`.
- **Entities:** `User` (updated: `resetToken`, `resetTokenExpiresAt`).
- **Gap / edge case:** **leaks account existence** — returns 400 "Email not found" for unknown emails (user enumeration). Best practice is a generic success message regardless.

### A6. Reset password 🟢
- **Description:** Validate token + expiry, set a new BCrypt password, clear the token.
- **Components:** `ResetPasswordComponent`, `AuthService.resetPassword`.
- **APIs:** `POST /api/auth/reset-password`.
- **Entities:** `User` (updated).
- **Edge cases:** invalid token → 400 "Invalid reset token"; expired → 400 "Expired reset token"; token single-use (cleared after use).

---

## B. Product Management

### B1. List products (dashboard) 🟢
- **Description:** Home shows all products with client-side search (name/serial) and pagination (6/page).
- **Components:** `HomeComponent`, `SidebarComponent`, `ProductService`.
- **APIs:** `GET /products`.
- **Entities:** `Product` (+ nested `warrantyList` count).

### B2. Create product 🟢
- **Description:** Add a product (name + serial number).
- **Components:** `ProductCreateComponent`, `ProductService`.
- **APIs:** `POST /products`.
- **Entities:** `Product` (created).

### B3. View product detail 🟢
- **Description:** Show a single product, its props, and associated warranties.
- **Components:** `ProductDetailComponent`, `ProductService`.
- **APIs:** `GET /products/{id}`.
- **Entities:** `Product`, nested `Warranty`, `AMC`.

### B4. Update product 🟢
- **Description:** Inline edit of name + serial; PUT full product.
- **APIs:** `PUT /products`. **Entities:** `Product`.
- **Edge case:** update of non-existent product → 404 `ResourceNotFoundException`.

### B5. Delete product 🟢
- **Description:** Delete with JS `confirm()`, redirect home.
- **APIs:** `DELETE /products/{id}`. **Entities:** `Product`.
- **Edge case:** delete non-existent → 404.

---

## C. Warranty (read-only in UI)

### C1. Expiring warranties dashboard 🟢
- **Description:** List warranties expiring within 30 days.
- **Components:** `HomeComponent`, `SidebarComponent`, `WarrantyService`.
- **APIs:** `GET /warranty`.
- **Entities:** `Warranty` (+ nested `AMC`).

### C2. Warranties by customer 🟡 (backend-only)
- **Description:** Cypher traversal `Customer→Sale→Product→Warranty` for one customer.
- **APIs:** `GET /warranty/{id}` (id = customerId). **No UI consumes this.**
- **Entities:** `Customer`, `Sale`, `Product`, `Warranty`.

### C3. Sidebar warranty link 🔴 (broken)
- **Description:** Sidebar renders `[routerLink]="['/warranties', warranty.warrantyId]"` but **no `/warranties/:id` route exists** → dead navigation.

---

## D. Customer / Sale / AMC / AMC Offer (backend CRUD, little/no UI)

### D1. Customer CRUD 🟡 (backend-only)
- **APIs:** `POST/PUT/GET /customers`, `GET /customers/{id}`, `DELETE /customers/{id}`.
- **Entities:** `Customer`. **No FE component** uses `CustomerService` (service exists, unused).

### D2. Sale create / list 🟡 (backend-only, partly buggy)
- **APIs:** `POST /sales`, `GET /sales/{id}` (customer's sales), `GET /sales` (takes Customer body — anti-pattern, semantically wrong).
- **Entities:** `Sale`, `Customer`. **No FE component** uses `SaleService`.

### D3. AMC CRUD 🟡 (backend-only)
- **APIs:** `POST/PUT/GET /amcs`, `GET /amcs/{id}`, `DELETE /amcs/{id}`.
- **Entities:** `AMC`, nested `AMCOffer`. **No FE component** uses `AmcService`.

### D4. AMC Offer CRUD 🟡 (backend-only)
- **APIs:** `POST/PUT/GET /amc-offers`, `GET /amc-offers/{id}`, `DELETE /amc-offers/{id}`.
- **Entities:** `AMCOffer`. **No FE component** uses `AmcOfferService`.

---

## E. Navigation & Shell

### E1. Conditional sidebar 🟢
- Sidebar shows only when authenticated AND not on an auth page; collapsible.
- **Components:** `App`, `NavbarComponent`, `SidebarComponent`.

### E2. Static pages 🟢
- **About** and **Contact** are static informational pages (auth-guarded).

---

## Workflow Summaries

### User workflow (happy path)
1. Visit app → guard redirects to `/login`.
2. Sign up or log in → session cookie set → redirect to `/` (Home).
3. Browse products & expiring warranties (search/paginate).
4. Add a product → land on its detail page.
5. Edit / delete a product.
6. Visit Profile → log out.

### Business workflow (intended, partially unimplemented in UI)
Customer purchases (Sale) → Sale is OF_PRODUCT(s) → Product HAS_WARRANTY → Warranty EXTENDED_BY AMC → AMC BASED_ON AMC Offer. The **graph model fully supports** this chain, and `WarrantyRepository.findWarrantiesByCustomerId` traverses it, but **no UI builds these relationships** — they'd have to be created by posting fully-formed nested JSON.

### Admin workflow
None — there are **no roles** beyond the single hard-coded `ROLE_USER` granted to every session. No admin screens.

### Hidden / non-obvious workflows
- `GET /sales` with a request body (Customer) — reachable only by a non-standard client.
- `GET /warranty/{id}` by customer — no UI.
- All Customer/Sale/AMC/AMCOffer CRUD — reachable only via direct API calls.
- Password-reset URL logged to server console when mail is unconfigured (dev backdoor to reset link).

### Edge cases explicitly handled
- Duplicate email, invalid credentials, invalid/expired reset token, missing resource (404), bean-validation failures (400), 401 JSON for unauthenticated access, SPA refresh on deep links, logout resilience to API failure.

