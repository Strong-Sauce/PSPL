# 06 — Architecture Audit Report

Issues are categorised and rated **[High] / [Med] / [Low]**. Each entry says **why it exists**. Nothing is fixed here.

---

## 1. Code Smells

| # | Issue | Where | Why it exists |
|---|---|---|---|
| 1 | **Inconsistent API namespacing** — `/api/auth/**` vs bare `/products`, `/customers`… | controllers | Auth was built later/separately with a deliberate `/api` prefix; business controllers predate that convention. Forces 7 proxy entries + per-route SPA forwarding. **[Med]** |
| 2 | **Inconsistent package casing** — `config`/`controllers` vs `Services`/`Repositories`/`Model`/`Exceptions` | backend packages | Hand-created packages without a convention. **[Low]** |
| 3 | **Inconsistent method names** — `createCust`, `updateCus`, `updateProd`, `createSales` | services | Ad-hoc abbreviations. Hurts readability. **[Low]** |
| 4 | **Typos** — `deletedProoduct`, `updateCustomer` method actually updating a Product | `ProductController` | Copy-paste. **[Low]** |
| 5 | **GET with request body** — `GET /sales` consumes a `Customer` body | `SaleController.getAllSalesOfCust` | Misunderstanding of HTTP semantics; many clients/proxies drop GET bodies. **[Med]** |
| 6 | **Semantically wrong query** — `getAllSalesOfCustomer` uses `findAllById([custId])` (treats customer id as sale id) | `SaleService` | Confusion between entity ids. Returns wrong data. **[High]** |
| 7 | **Error message leakage** — catch-all returns `ex.getMessage()` | `GlobalExceptionHandler` | Convenience during dev. Info disclosure. **[Med]** |
| 8 | **Inline templates/styles** for some pages but external files for others | `Profile/About/Contact` vs others | No agreed component-file convention. **[Low]** |

## 2. Anti-Patterns

| # | Issue | Why |
|---|---|---|
| 1 | **Entities used as API request/response models** (`@RequestBody Product`, returning nodes directly). No DTO boundary for business resources. | Speed of development. Couples API contract to DB schema; risks deep-graph over-posting/over-fetching. **[High]** |
| 2 | **Manual session/security-context construction** in `AuthService.createSession` | Custom login instead of `AuthenticationManager`. Works but bypasses standard Spring auth pipeline; brittle. **[Med]** |
| 3 | **User enumeration** on signup ("Email already exists") and forgot-password ("Email not found") | Friendly messages prioritised over privacy. **[Med]** |
| 4 | **CSRF fully disabled** while using cookie-based sessions | Assumed "REST = no CSRF", but cookie auth IS CSRF-susceptible. **[High]** |

## 3. Tight Coupling

| # | Coupling | Why |
|---|---|---|
| 1 | API ⇄ DB schema (no DTO mapping for business entities) | see anti-pattern #1. **[High]** |
| 2 | Frontend models mirror backend node shape incl. nested graphs | Convenience; changes to graph ripple to FE types. **[Med]** |
| 3 | `SpaController` hard-codes every Angular route | No catch-all forwarding strategy; every new route needs a backend edit. **[Med]** |

## 4. Circular Dependencies
- **None found** in Java beans or Angular providers. Node entities reference each other in one direction only (no bidirectional `@Relationship`). **[OK]**

## 5. Duplicate Code

| # | Duplication | Why |
|---|---|---|
| 1 | 4 near-identical CRUD services (`Product/Customer/AMC/AMCOffer`) and controllers | No generic base/abstraction. **[Med]** |
| 2 | 6 near-identical Angular resource services | Same. A generic `CrudService<T>` would remove it. **[Low]** |
| 3 | Repeated auth-form component pattern (signal fields + submit + error) | No shared form helper. **[Low]** |
| 4 | Repeated pagination/search logic could be shared | Only Home uses it now. **[Low]** |

## 6. Dead Code

| # | Dead code | Why |
|---|---|---|
| 1 | **All 5 `Model/Relationships/*.java`** fully commented out | Abandoned attempt at `@RelationshipProperties`. **[Low]** |
| 2 | `CustomerService`, `SaleService`, `AmcService`, `AmcOfferService` (FE) — defined, exported, **never used by a component** | Built ahead of UI. **[Low]** |
| 3 | `GET /sales` (body) and `GET /warranty/{id}` — no caller | Built ahead of UI. **[Low]** |
| 4 | Sidebar `/warranties/:id` link → route doesn't exist | Placeholder never wired. **[Low]** |

## 7. Over-Engineering
| # | Issue | Why |
|---|---|---|
| 1 | **SSR enabled** (`@angular/ssr`, server.ts) but every route is `RenderMode.Client` | Scaffolded by `ng new` with SSR; provides no benefit here, adds build/runtime complexity. **[Low]** |

## 8. Under-Engineering
| # | Issue | Why |
|---|---|---|
| 1 | No DTO/validation layer on business endpoints | Skipped. **[High]** |
| 2 | No DB constraints/indexes/migrations | Relied on app checks. **[Med]** |
| 3 | No real tests | Time. **[Med]** |
| 4 | No pagination/filtering server-side | Dataset assumed small. **[Med]** |
| 5 | No relationship-management endpoints (attach warranty↔product, record sale↔customer) | Incomplete domain layer. **[High]** |
| 6 | No global FE error/toast handling or HTTP error interceptor (only inline messages) | **[Low]** |

## 9. Naming Inconsistencies
- Package casing (#2.2), service method abbreviations (#1.3), `AMC` vs `Amc` (class `AMC`, FE service `AmcService`), `/warranty` (singular) vs `/amcs` (plural), `warrantyList`/`amcList`/`amcOfferList` vs `productList`/`purchases`. **[Low]**

## 10. Security Issues

| # | Issue | Why | Severity |
|---|---|---|---|
| 1 | **CSRF disabled with cookie sessions** | Misconception. State-changing endpoints are CSRF-vulnerable. | **High** |
| 2 | **No authorization granularity / no data ownership** — any logged-in user can CRUD all business data | No `User`↔data relationship, single `ROLE_USER`. | **High** |
| 3 | **User enumeration** (signup + forgot) | Friendly errors. | Med |
| 4 | **Exception message leakage** (500) | Dev convenience. | Med |
| 5 | **No rate limiting / lockout** on login & forgot-password | Not implemented. | Med |
| 6 | **No unique DB constraint on email** (race on signup) | App-level check only. | Med |
| 7 | **CORS `allowCredentials(true)` with split origins** — must be tightly pinned in prod | Acceptable if `CORS_ALLOWED_ORIGINS` is set correctly; risky if widened. | Low |
| 8 | **Session cookie flags** (Secure/SameSite/HttpOnly) not explicitly configured | Relies on container defaults. | Med |

## 11. Scalability Issues
- **Deep-graph fetch/persist** via SDN can pull/write large subgraphs (#5 data risks). **[Med]**
- **No server pagination** — `GET /products` returns everything. **[Med]**
- **Stateful sessions** limit horizontal scaling without sticky sessions / shared session store. **[Med]**

## 12. Performance Issues
- Home fetches **all** products and warranties, then filters/pages client-side. **[Med]**
- Sidebar independently re-fetches products + warranties (duplicate of Home's calls). **[Low]**
- Missing index on `Warranty.warrantyEndDate` for the expiring-soon scan. **[Low]**

---

## Summary scorecard

| Area | Verdict |
|---|---|
| Frontend architecture (standalone + signals + guards) | **Good, modern, consistent** |
| Backend layering (controller/service/repo) | **Good shape, but no DTO boundary for business** |
| Security | **Needs work** (CSRF, authz, enumeration, constraints) |
| Domain completeness | **Partial** (UI only covers auth + products + warranty read) |
| Data integrity | **Weak** (no constraints/indexes/migrations) |
| Tests | **Absent** |
| Consistency / naming | **Mixed** |

