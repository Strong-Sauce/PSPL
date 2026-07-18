# 08 — Recommended Folder Structure

Concrete, copy-ready layout for the clean rebuild. Feature-based (vertical slices) on both ends.

---

## Backend — `src/main/java/com/pspl/amc/`

```
com/pspl/amc/
├─ AmcApplication.java
│
├─ config/
│   ├─ SecurityConfig.java          # session auth, CSRF ON (cookie→header), cookie flags, authz rules
│   ├─ CorsConfig.java              # allowed origins from env
│   ├─ Neo4jConfig.java             # CREATE CONSTRAINT/INDEX IF NOT EXISTS on startup
│   └─ SpaForwardingController.java # single catch-all forward for non-/api GET routes
│
├─ common/
│   ├─ GlobalExceptionHandler.java  # @RestControllerAdvice → ApiError
│   ├─ ApiError.java                # {timestamp,status,error,message,path}
│   ├─ ResourceNotFoundException.java
│   └─ BadRequestException.java
│
├─ auth/
│   ├─ AuthController.java          # /api/auth/**
│   ├─ AuthService.java
│   └─ dto/
│       ├─ SignupRequest.java
│       ├─ LoginRequest.java
│       ├─ ForgotPasswordRequest.java
│       ├─ ResetPasswordRequest.java
│       ├─ AuthResponse.java
│       ├─ UserResponse.java
│       └─ MessageResponse.java
│
├─ user/
│   ├─ User.java                    # @Node
│   └─ UserRepository.java
│
├─ product/
│   ├─ ProductController.java       # /api/products
│   ├─ ProductService.java
│   ├─ ProductRepository.java
│   ├─ Product.java                 # @Node
│   └─ dto/{ProductRequest.java, ProductResponse.java}
│
├─ warranty/
│   ├─ WarrantyController.java      # /api/warranties
│   ├─ WarrantyService.java
│   ├─ WarrantyRepository.java
│   ├─ Warranty.java
│   └─ dto/{WarrantyResponse.java}
│
├─ customer/  (same pattern)
├─ sale/      (same pattern, incl. relationship endpoints)
├─ amc/       (same pattern)
└─ amcoffer/  (same pattern)
```

**Resources**
```
src/main/resources/
├─ application.yml                  # prefer YAML; env-driven
├─ application-dev.yml
└─ application-prod.yml
```

**Tests** mirror the package tree under `src/test/java/...` (service unit tests + `@WebMvcTest` controller slices + a couple of `@DataNeo4jTest`).

---

## Frontend — `frontend/src/app/`

```
app/
├─ app.config.ts                    # provideRouter, provideHttpClient(withFetch, withXsrfConfiguration, withInterceptors)
├─ app.routes.ts                    # all routes, lazy loadComponent, guards
├─ app.ts / app.html / app.css      # shell root
│
├─ core/                            # singletons, app-wide
│   ├─ auth/
│   │   ├─ auth.service.ts          # signals: currentUser, isAuthenticated
│   │   ├─ auth.guard.ts
│   │   └─ guest.guard.ts
│   ├─ http/
│   │   └─ http-error.interceptor.ts
│   └─ api/
│       └─ api-crud.service.ts      # generic base CRUD<T>
│
├─ shared/                          # reusable, stateless
│   ├─ ui/
│   │   ├─ alert/ alert.component.ts
│   │   ├─ paginator/ paginator.component.ts
│   │   ├─ search-box/ search-box.component.ts
│   │   ├─ card/ card.component.ts
│   │   └─ button/ button.component.ts
│   └─ models/                      # FE interfaces (User, Product, Warranty, ... + auth DTOs)
│
├─ layout/
│   ├─ navbar/ navbar.component.{ts,html,css}
│   └─ sidebar/ sidebar.component.{ts,html,css}
│
└─ features/
    ├─ auth/
    │   ├─ login/ login.component.{ts,html,css}
    │   ├─ signup/ signup.component.{ts,html,css}
    │   ├─ forgot-password/ ...
    │   └─ reset-password/ ...
    ├─ products/
    │   ├─ product-list/ (the home dashboard)
    │   ├─ product-create/
    │   ├─ product-detail/
    │   └─ product.service.ts
    ├─ warranties/
    │   ├─ warranty-list/
    │   ├─ warranty-detail/        # add the missing /warranties/:id route+page
    │   └─ warranty.service.ts
    ├─ profile/ profile.component.{ts,html,css}
    └─ static/ {about, contact}
```

**Conventions**
- **Always external** template & style files for components (no inline `template:`/`styles:` mixing) — pick one rule and keep it.
- One component per folder; folder name = feature name.
- `core/` = app-singletons & cross-cutting; `shared/` = dumb reusable UI + types; `features/` = pages.
- Barrels (`index.ts`) only at `shared/models` and `shared/ui` to keep imports tidy.

---

## Root layout
```
amcProject/
├─ backend/ (or src/ + pom.xml)   # Spring Boot
├─ frontend/                      # Angular
├─ docs/                          # these documents
└─ README.md                      # run/build/deploy instructions
```

