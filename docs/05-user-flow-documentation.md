# 05 — User Flow Documentation

Diagrams use simple arrows; "FE" = Angular, "BE" = Spring Boot, "DB" = Neo4j.

---

## 1. App bootstrap / session restore

```
Browser loads app
  → App constructor calls AuthService.initSession()
  → FE: GET /api/auth/me (cookie sent via withCredentials)
  → BE: Authentication from session?
        ├─ yes → 200 {id,name,email} → currentUser signal set
        └─ no  → 401 → currentUser = null (guest)
  → Router activates target route → guard runs (see below)
```

## 2. Route guard decision

```
authGuard (protected routes):
  currentUser() set? ── yes ──▶ allow
        │ no
        ▼
  GET /api/auth/me ── user? ── yes ──▶ allow
                          └─ no ──▶ navigate /login, block

guestGuard (auth pages):
  currentUser() set? ── yes ──▶ navigate /, block
        │ no
        ▼
  GET /api/auth/me ── user? ── yes ──▶ navigate /, block
                          └─ no ──▶ allow
```

## 3. Signup flow

```
SignupComponent form (name,email,password)
  → client check: all fields present
  → AuthService.signup() → POST /api/auth/signup
      BE: validate (@Valid) → existsByEmail? 
          ├─ yes → 400 "Email already exists"
          └─ no  → hash password (BCrypt) → save User → createSession()
                   → 201 {message,user}
  → FE: currentUser.set(user) → navigate '/'
  (errors → red alert with err.error.message)
```

## 4. Login flow

```
LoginComponent (email,password)
  → POST /api/auth/login
      BE: findByEmail → matches(password,hash)?
          ├─ no  → 400 "Invalid credentials"
          └─ yes → createSession() → 200 {message,user}
  → currentUser.set(user) → navigate '/'
```

## 5. Logout flow

```
Navbar/Profile "Logout"
  → POST /api/auth/logout → session.invalidate() + clear context → 200
  → currentUser.set(null) → navigate '/login'
  (even on API error → clear + redirect)
```

## 6. Forgot password flow

```
ForgotPasswordComponent (email)
  → POST /api/auth/forgot-password
      BE: findByEmail
          ├─ not found → 400 "Email not found"   ⚠ enumeration
          └─ found → generate token (32B base64) + expiry(now+30m)
                     → save on User
                     → EmailService.send (or log URL if mail unconfigured)
                     → 200 "Password reset link has been sent..."
  → show message
```

## 7. Reset password flow

```
User clicks email link → /reset-password?token=XYZ
  → ResetPasswordComponent reads token from query param
  → user enters newPassword → POST /api/auth/reset-password
      BE: findByResetToken
          ├─ not found → 400 "Invalid reset token"
          ├─ expired   → 400 "Expired reset token"
          └─ valid → hash newPassword, clear token → 200
  → show success → setTimeout 2s → navigate '/login'
```

## 8. Product CRUD flows

**List/search (Home):**
```
HomeComponent.ngOnInit → GET /products, GET /warranty
  → store in signals → computed filter (search) → computed paginate(6)
  → render cards + pagination controls
```

**Create:**
```
ProductCreateComponent → validate name+serial
  → POST /products → 200 {productId,...}
  → navigate ['/products', productId]
```

**Detail / Edit / Delete:**
```
ProductDetailComponent.ngOnInit → GET /products/{id} → fill signals
  Edit:   toggle editing → PUT /products (full object) → update signal
  Delete: confirm() → DELETE /products/{id} → navigate '/'
```

## 9. Sidebar load

```
SidebarComponent.ngOnInit
  → GET /products → products signal
  → GET /warranty → warranties signal
  (warranty items link to /warranties/:id → NO ROUTE → broken nav)
```

## 10. Shell / sidebar visibility

```
showSidebar = isAuthenticated() AND not on (/login|/signup|/forgot-password|/reset-password)
NavigationEnd events update currentPath signal → recompute
```

---

## Flow gaps (documented, not fixed)
- Sidebar warranty link → missing `/warranties/:id` route.
- Customer / Sale / AMC / AMC-Offer have **no UI flow** (API-only).
- No "create relationship" UI flow (e.g. attach a warranty to a product, or record a sale for a customer).
- No email-verification or account-confirmation flow.

