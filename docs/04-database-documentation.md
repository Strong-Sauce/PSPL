# 04 — Database Documentation (Neo4j)

Spring Data Neo4j (OGM via `@Node`, `@Relationship`, `@Id @GeneratedValue`). IDs are **String UUIDs auto-generated** by Neo4j (`@GeneratedValue` with the default UUID strategy). Connection comes from `spring.neo4j.uri / username / password` env vars.

> **Important:** Relationships are declared **inline on the node classes** via `@Relationship`. The dedicated `@RelationshipProperties` classes in `Model/Relationships/*` are **all commented out** — there are **no relationship properties** in the live model; relationships carry only their type.

---

## 1. Node Labels & Properties

### `(:User)` — account
| Property | Type | Notes |
|---|---|---|
| `id` | String | `@Id @GeneratedValue` |
| `name` | String | |
| `email` | String | login identifier (normalized lowercase in service) |
| `password` | String | **BCrypt hash** |
| `createdAt` | LocalDateTime | |
| `updatedAt` | LocalDateTime | |
| `resetToken` | String? | null unless reset pending |
| `resetTokenExpiresAt` | LocalDateTime? | 30-min expiry |

> `User` has **no relationships** — it is isolated from the business graph (auth-only).

### `(:Customer)`
| Property | Type |
|---|---|
| `custId` | String (`@Id @GeneratedValue`) |
| `custName` | String |
| **rel** `purchases` | `-[:PURCHASED]->(:Sale)` (OUTGOING, list) |

### `(:Sale)`
| Property | Type |
|---|---|
| `saleId` | String (id) |
| `saleDate` | LocalDate |
| **rel** `productList` | `-[:OF_PRODUCT]->(:Product)` (OUTGOING, list) |

### `(:Product)`
| Property | Type |
|---|---|
| `productId` | String (id) |
| `productName` | String |
| `productSerialNumber` | String |
| **rel** `warrantyList` | `-[:HAS_WARRANTY]->(:Warranty)` (OUTGOING, list) |

### `(:Warranty)`
| Property | Type |
|---|---|
| `warrantyId` | String (id) |
| `warrantyStartDate` | LocalDate |
| `warrantyEndDate` | LocalDate |
| **rel** `amcList` | `-[:EXTENDED_BY]->(:AMC)` (OUTGOING, list) |

### `(:AMC)`
| Property | Type |
|---|---|
| `amcId` | String (id) |
| `amcStartDate` | LocalDate |
| `amcEndDate` | LocalDate |
| **rel** `amcOfferList` | `-[:BASED_ON]->(:AMCOffer)` (OUTGOING, list) |

### `(:AMCOffer)`
| Property | Type |
|---|---|
| `offerId` | String (id) |
| `offerType` | String (e.g. "Silver" / "Gold") |
| `offerDurationMonths` | Integer |
| `offerPrice` | Double |
| `offerTerms` | String |

---

## 2. Relationships

| Type | From → To | Cardinality (model) | Why it exists |
|---|---|---|---|
| `PURCHASED` | `Customer` → `Sale` | 1 customer → many sales | A customer makes purchase transactions; each purchase is a `Sale`. |
| `OF_PRODUCT` | `Sale` → `Product` | 1 sale → many products | A sale can include one or more products (a basket). |
| `HAS_WARRANTY` | `Product` → `Warranty` | 1 product → many warranties | A purchased product is covered by a warranty (possibly multiple over time). |
| `EXTENDED_BY` | `Warranty` → `AMC` | 1 warranty → many AMCs | After warranty, coverage continues via Annual Maintenance Contracts. |
| `BASED_ON` | `AMC` → `AMCOffer` | 1 AMC → many offers | An AMC instance is created from a catalog offer/plan (Silver/Gold). |

All relationships are **OUTGOING** and have **no properties** (the `@RelationshipProperties` variants are dead/commented).

---

## 3. ER-style relationship diagram

```
(:User)   ── isolated (authentication only; no link to business graph)

(:Customer) ──[:PURCHASED]──▶ (:Sale) ──[:OF_PRODUCT]──▶ (:Product)
                                                              │
                                                       [:HAS_WARRANTY]
                                                              ▼
                                                        (:Warranty)
                                                              │
                                                       [:EXTENDED_BY]
                                                              ▼
                                                          (:AMC)
                                                              │
                                                        [:BASED_ON]
                                                              ▼
                                                       (:AMCOffer)
```

**End-to-end traversal (used by `findWarrantiesByCustomerId`):**
`(:Customer)-[:PURCHASED]->(:Sale)-[:OF_PRODUCT]->(:Product)-[:HAS_WARRANTY]->(:Warranty)`

---

## 4. Constraints & Indexes

**None are defined in code.** There are:
- No `@Index` / unique-constraint annotations.
- No migration/CQL bootstrap files.
- No explicit uniqueness on `User.email` (uniqueness is enforced only by the application's `existsByEmail` check — racy under concurrency).

**Recommended (not present):**
- Unique constraint on `User.email`.
- Unique constraint on `User.resetToken`.
- Index on `Warranty.warrantyEndDate` (the "expiring soon" query filters on it).
- Unique constraints on each `@Id` (Neo4j auto-creates an internal index for generated ids, but explicit constraints are advisable).

---

## 5. Notable queries (Cypher)

```cypher
// Sales for a customer
MATCH (c:Customer)-[:PURCHASED]->(s:Sale) WHERE c.custId = $customerId RETURN s

// Warranties expiring within 30 days
MATCH (w:Warranty) WHERE w.warrantyEndDate <= date() + duration('P30D') RETURN w

// Warranties for a customer (full chain)
MATCH (c:Customer)-[:PURCHASED]->(s:Sale)-[:OF_PRODUCT]->(p:Product)-[:HAS_WARRANTY]->(w:Warranty)
WHERE c.custId = $custId RETURN w
```

## 6. Data-model risks
- **Deep auto-persist:** saving a `Customer` with nested `purchases` cascades the entire subgraph; posting partial graphs can detach/overwrite relationships unexpectedly (SDN save semantics).
- **No ownership link** between `User` and business nodes → no multi-tenant isolation.
- **Unbounded fetch depth** can pull large subgraphs into memory.

