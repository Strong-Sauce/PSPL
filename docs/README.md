# PSPL AMC — Project Reconstruction Blueprint (Documentation Index)

This `docs/` folder is a complete, evidence-based audit of the existing codebase and a clean-rebuild plan. **No application code was modified** to produce these documents — they describe the project exactly as it exists today and recommend (but do not yet implement) a cleaner architecture.

## What this project is
**PSPL AMC** — a Post-Sale Product Lifecycle & Annual Maintenance Contract management system.
**Stack:** Angular 21 (standalone, signals, SSR-scaffolded) · Spring Boot 4 / Java 17 · Neo4j (graph) · Spring Security **session-based** auth (BCrypt) · Lombok.

> **Note on JWT:** the brief mentioned JWT/token-refresh, but the actual implementation uses **server-side HTTP sessions (`JSESSIONID`)**, not JWT. The docs reflect the real implementation.

## Deliverables

| # | Document | Contents |
|---|---|---|
| 01 | [Complete Project Analysis](./01-project-analysis.md) | Full backend + frontend audit; every component/service/controller |
| 02 | [Feature Inventory](./02-feature-inventory.md) | Every feature + user/business/admin/hidden workflows + edge cases |
| 03 | [API Documentation](./03-api-documentation.md) | Every endpoint: URL, method, request/response, errors |
| 04 | [Database Documentation](./04-database-documentation.md) | Nodes, relationships, properties, constraints, ER diagram |
| 05 | [User Flow Documentation](./05-user-flow-documentation.md) | Step-by-step flows for auth, products, warranties, shell |
| 06 | [Architecture Audit Report](./06-architecture-audit-report.md) | Code smells, anti-patterns, coupling, dead code, security, perf |
| 07 | [Rebuild Blueprint](./07-rebuild-blueprint.md) | Overview, functional & non-functional reqs, clean architecture |
| 08 | [Recommended Folder Structure](./08-recommended-folder-structure.md) | Copy-ready backend + frontend layout |
| 09 | [Coding Standards Guide](./09-coding-standards-guide.md) | Naming, layering, security, FE/BE conventions |
| 10 | [FINAL REBUILD PROMPT](./10-final-rebuild-prompt.md) | Self-contained prompt to rebuild the whole app from scratch |

## Key findings at a glance
- **Solid:** modern standalone Angular + signals; clean controller/service/repository layering; well-fitting Neo4j graph model; complete session-auth subsystem.
- **Needs work:** no DTO boundary for business endpoints (entities exposed directly); CSRF disabled with cookie auth; user enumeration on signup/forgot; no DB constraints/indexes; inconsistent `/api` prefixing and naming; dead code (commented relationship classes, unused FE services); a broken sidebar link (`/warranties/:id` route missing); SSR scaffolded but unused; no real tests.
- **Domain completeness:** UI currently covers auth + products + warranty-read; Customer/Sale/AMC/AMC-Offer are API-only, and no UI builds the graph relationships.

Start with **01** for understanding, jump to **10** to rebuild.

