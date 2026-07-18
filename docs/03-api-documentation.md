# 03 — API Documentation

Base origins:
- **Dev:** Angular `http://localhost:4200` proxies to Spring Boot `http://localhost:8080` (`proxy.conf.json`).
- **Prod:** same origin (frontend served by Spring Boot).

**Auth model:** server-side session. Every request must send the `JSESSIONID` cookie (the FE `credentialsInterceptor` adds `withCredentials: true`). No `Authorization` header / no JWT.

**Standard error envelope** (from `GlobalExceptionHandler` / `RestAuthenticationEntryPoint`):
```json
{ "timestamp": "2026-06-15T10:00:00", "status": 400, "error": "Bad Request", "message": "Email already exists" }
```

---

## 1. Authentication — `/api/auth` (public except `/me`, `/logout`)

### POST `/api/auth/signup`
- **Purpose:** Register + auto-login.
- **Request:**
```json
{ "name": "John Doe", "email": "john@example.com", "password": "secret123" }
```
- **Validation:** name 2–100, valid email, password 8–100.
- **Response 201:**
```json
{ "message": "Signup successful", "user": { "id": "…", "name": "John Doe", "email": "john@example.com" } }
```
- **Errors:** 400 "Email already exists"; 400 validation message.
- **Side effect:** creates session cookie.

### POST `/api/auth/login`
- **Purpose:** Authenticate, create session.
- **Request:** `{ "email": "...", "password": "..." }`
- **Response 200:** `{ "message": "Login successful", "user": { id, name, email } }`
- **Errors:** 400 "Invalid credentials".

### GET `/api/auth/me`
- **Purpose:** Return current user from session (used for restore + guards).
- **Request:** none (cookie only).
- **Response 200:** `{ "id": "...", "name": "...", "email": "..." }`
- **Errors:** 401 (empty body) when no/anonymous session.

### POST `/api/auth/forgot-password`
- **Purpose:** Issue reset token + email link (or log it).
- **Request:** `{ "email": "..." }`
- **Response 200:** `{ "message": "Password reset link has been sent to your email" }`
- **Errors:** 400 "Email not found" *(leaks existence — see audit)*.
- **Side effect:** sets `resetToken`, `resetTokenExpiresAt` (now + 30 min) on the user.

### POST `/api/auth/reset-password`
- **Purpose:** Set a new password with a valid token.
- **Request:** `{ "token": "...", "newPassword": "newSecret123" }` (newPassword 8–100)
- **Response 200:** `{ "message": "Password has been reset successfully. Please log in." }`
- **Errors:** 400 "Invalid reset token" / "Expired reset token".

### POST `/api/auth/logout`
- **Purpose:** Invalidate session.
- **Request:** empty body.
- **Response 200:** `{ "message": "Logout successful" }`

---

## 2. Products — `/products` (authenticated)

| Method | Path | Body | Response | Notes |
|---|---|---|---|---|
| GET | `/products` | — | `Product[]` | all products |
| GET | `/products/{id}` | — | `Product` / 404 | by id |
| POST | `/products` | `Product` | `Product` | create (id generated) |
| PUT | `/products` | `Product` (with `productId`) | `Product` / 404 | full update |
| DELETE | `/products/{id}` | — | 204 / 404 | delete |

**Product JSON:**
```json
{ "productId": "…", "productName": "Router X", "productSerialNumber": "SN-001", "warrantyList": [ /* Warranty[] */ ] }
```

## 3. Customers — `/customers` (authenticated)

| Method | Path | Body | Response |
|---|---|---|---|
| GET | `/customers` | — | `Customer[]` |
| GET | `/customers/{id}` | — | `Customer` / 404 |
| POST | `/customers` | `Customer` | `Customer` |
| PUT | `/customers` | `Customer` (with `custId`) | `Customer` / 404 |
| DELETE | `/customers/{id}` | — | 204 |

**Customer JSON:** `{ "custId": "…", "custName": "Acme", "purchases": [ /* Sale[] */ ] }`

## 4. Sales — `/sales` (authenticated)

| Method | Path | Body | Response | Notes |
|---|---|---|---|---|
| GET | `/sales/{id}` | — | `Sale[]` | sales for customer id (Cypher `Customer-[:PURCHASED]->Sale`) |
| POST | `/sales` | `Sale` | `Sale` | create |
| GET | `/sales` | `Customer` ⚠️ | `Sale[]` | **anti-pattern: GET with body**; uses `findAllById([custId])` (semantically wrong — treats custId as saleId) |

**Sale JSON:** `{ "saleId": "…", "saleDate": "2026-01-01", "productList": [ /* Product[] */ ] }`

## 5. Warranty — `/warranty` (authenticated)

| Method | Path | Body | Response | Notes |
|---|---|---|---|---|
| GET | `/warranty` | — | `Warranty[]` | expiring within 30 days |
| GET | `/warranty/{id}` | — | `Warranty[]` | by **customer** id (full graph traversal) |

**Warranty JSON:** `{ "warrantyId": "…", "warrantyStartDate": "2025-01-01", "warrantyEndDate": "2026-01-01", "amcList": [ /* AMC[] */ ] }`

## 6. AMCs — `/amcs` (authenticated)

| Method | Path | Body | Response |
|---|---|---|---|
| GET | `/amcs` | — | `AMC[]` |
| GET | `/amcs/{id}` | — | `AMC` / 404 |
| POST | `/amcs` | `AMC` | `AMC` |
| PUT | `/amcs` | `AMC` (with `amcId`) | `AMC` / 404 |
| DELETE | `/amcs/{id}` | — | 204 |

**AMC JSON:** `{ "amcId": "…", "amcStartDate": "2026-01-01", "amcEndDate": "2027-01-01", "amcOfferList": [ /* AMCOffer[] */ ] }`

## 7. AMC Offers — `/amc-offers` (authenticated)

| Method | Path | Body | Response |
|---|---|---|---|
| GET | `/amc-offers` | — | `AMCOffer[]` |
| GET | `/amc-offers/{id}` | — | `AMCOffer` / 404 |
| POST | `/amc-offers` | `AMCOffer` | `AMCOffer` |
| PUT | `/amc-offers` | `AMCOffer` (with `offerId`) | `AMCOffer` / 404 |
| DELETE | `/amc-offers/{id}` | — | 204 |

**AMCOffer JSON:** `{ "offerId": "…", "offerType": "Gold", "offerDurationMonths": 12, "offerPrice": 1999.0, "offerTerms": "…" }`

---

## Status / error code summary

| Code | When |
|---|---|
| 200 | Successful GET/PUT/POST(login etc.) |
| 201 | Signup |
| 204 | Successful DELETE |
| 400 | Validation failure, bad business input (`IllegalArgumentException`) |
| 401 | Unauthenticated access to protected resource / `/me` with no session |
| 404 | Resource not found |
| 500 | Uncaught exception (message leaked) |

## Security exposure notes
- Business endpoints (`/products`, `/customers`, …) are **not** namespaced under `/api`, so SPA forwarding and proxying must list each prefix individually.
- No per-user data scoping: any authenticated user can read/modify **all** products/customers/etc. (no ownership relationship between `User` and business data).

