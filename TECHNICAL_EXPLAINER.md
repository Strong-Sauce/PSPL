# 📚 Technical Explainer — How the PSPL System Works

> A beginner-friendly deep-dive into the code architecture, explaining **how** and **why** each part of the system is built the way it is.

---

## 📖 Table of Contents

1. [The Backend Flow: Controller → Service → Repository](#1-the-backend-flow-controller--service--repository)
2. [Spring Data Neo4j vs. Raw Neo4j Driver](#2-spring-data-neo4j-vs-raw-neo4j-driver)
3. [Breaking Down Our Cypher Queries](#3-breaking-down-our-cypher-queries)
4. [The Angular Fixed Layout (Navbar + Sidebar)](#4-the-angular-fixed-layout-navbar--sidebar)
5. [Reactive State with Angular Signals](#5-reactive-state-with-angular-signals)
6. [Frontend-Backend Communication & CORS](#6-frontend-backend-communication--cors)
7. [Error Handling Across the Stack](#7-error-handling-across-the-stack)

---

## 1. The Backend Flow: Controller → Service → Repository

When the Angular frontend makes an HTTP request, it travels through **three layers** in our Spring Boot backend. Let's trace the journey of a `GET /products` request:

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Browser   │────▶│ Controller  │────▶│   Service   │────▶│ Repository  │
│  (Angular)  │     │   (REST)    │     │  (Business) │     │   (Neo4j)   │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
```

### Layer 1: The Controller

**What it does:** Receives HTTP requests and returns HTTP responses. It's the "front door" of your API.

**File:** `ProductController.java`

```java
@RestController                          // ① Marks this class as a REST API controller
@RequestMapping("api/products")             // ② All endpoints start with /products
public class ProductController {

    private final ProductService productService;  // ③ Dependency injection

    public ProductController(ProductService productService) {
        this.productService = productService;     // ④ Spring injects the service
    }

    @GetMapping                          // ⑤ Handles GET /products
    public List<Product> getAllProducts(){
        return productService.getAllProducts();   // ⑥ Delegates to service layer
    }

    @GetMapping("/{id}")                 // ⑦ Handles GET /products/{id}
    public ResponseEntity<Product> getProduct(@PathVariable String id){
        return productService.getProductById(id)
                .map(ResponseEntity::ok)          // ⑧ If found → 200 OK
                .orElse(ResponseEntity.notFound().build());  // ⑨ If not → 404
    }

    @PostMapping                         // ⑩ Handles POST /products
    public Product createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }
}
```

**Key annotations explained:**

| Annotation                        | What it does |
|-----------------------------------|--------------|
| `@RestController`                 | Combines `@Controller` + `@ResponseBody`. Returns JSON automatically. |
| `@RequestMapping("api/products")` | Base URL path for all methods in this controller |
| `@GetMapping` / `@PostMapping`    | Maps HTTP verbs to methods |
| `@PathVariable`                   | Extracts `{id}` from the URL path |
| `@RequestBody`                    | Deserializes JSON body into a Java object |

> 💡 **Why separate from Service?** Controllers should only handle HTTP concerns (request/response). Business logic belongs in Services. This makes testing easier and keeps code organized.

---

### Layer 2: The Service

**What it does:** Contains business logic, validation, and orchestrates calls to the repository.

**File:** `ProductService.java`

```java
@Service                                 // ① Marks this as a Spring-managed service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)      // ② Read-only transaction (optimization)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);   // ③ Returns Optional (may be empty)
    }

    @Transactional                       // ④ Write transaction
    public Product createProduct(Product product) {
        return productRepository.save(product);  // ⑤ SDN generates the Cypher
    }

    @Transactional
    public Product updateProd(Product product) {
        // ⑥ Validate existence before updating
        if (!productRepository.existsById(product.getProductId())) {
            throw new ResourceNotFoundException("Product", product.getProductId());
        }
        return productRepository.save(product);
    }

    @Transactional
    public boolean deleteProduct(String id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        productRepository.deleteById(id);
        return true;
    }
}
```

**Key concepts:**

| Concept | Explanation |
|---------|-------------|
| `@Transactional` | Wraps the method in a database transaction. If anything fails, changes are rolled back. |
| `readOnly = true` | Tells Neo4j this won't modify data, allowing optimizations. |
| `Optional<T>` | Java's way of saying "this might be null" — forces you to handle the empty case. |

> 💡 **Why throw exceptions here?** Services validate business rules. If a product doesn't exist, that's a business error. The exception bubbles up to the `GlobalExceptionHandler`.

---

### Layer 3: The Repository

**What it does:** Communicates with Neo4j. Spring Data Neo4j generates most queries for you.

**File:** `ProductRepository.java`

```java
@Repository
public interface ProductRepository extends Neo4jRepository<Product, String> {
    // That's it! Spring Data Neo4j provides:
    // - save(Product)
    // - findById(String)
    // - findAll()
    // - deleteById(String)
    // - existsById(String)
    // ...and more, automatically!
}
```

**How does this magic work?**

When you extend `Neo4jRepository<Product, String>`:
- `Product` = the entity type (maps to a Neo4j `:Product` node)
- `String` = the ID type (our `productId` field)

Spring Data Neo4j reads the `@Node` and `@Id` annotations on the `Product` class and generates Cypher queries at runtime:

```java
@Node                                    // Maps to (:Product) node in Neo4j
public class Product {
    @Id @GeneratedValue                  // Auto-generated unique ID
    private String productId;
    
    private String productName;
    private String productSerialNumber;
    
    @Relationship(type = "HAS_WARRANTY", direction = Relationship.Direction.OUTGOING)
    private List<Warranty> warrantyList; // Creates HAS_WARRANTY edges
}
```

When you call `productRepository.findAll()`, SDN generates:

```cypher
MATCH (p:Product) RETURN p
```

When you call `productRepository.save(product)`, SDN generates:

```cypher
CREATE (p:Product {productId: $id, productName: $name, productSerialNumber: $serial})
RETURN p
```

---

## 2. Spring Data Neo4j vs. Raw Neo4j Driver

You might see tutorials using the **Neo4j Java Driver** directly with `Session` and `Transaction` objects. Here's the difference:

### Raw Neo4j Driver (Low-Level)

```java
// You'd have to write this yourself:
try (Session session = driver.session()) {
    Result result = session.run(
        "MATCH (p:Product) WHERE p.productId = $id RETURN p",
        Map.of("id", productId)
    );
    // Manually map result to Java object...
}
```

### Spring Data Neo4j (High-Level)

```java
// Just call this:
productRepository.findById(productId);
```

**When to use which?**

| Use Case | Approach |
|----------|----------|
| Simple CRUD operations | `Neo4jRepository` methods (automatic) |
| Complex traversals across many relationships | Custom `@Query` annotations |
| Dynamic query building | Raw driver with manual Cypher |

In this project, we use **Spring Data Neo4j** because it handles 90% of our needs. For complex queries, we add `@Query` annotations (see next section).

---

## 3. Breaking Down Our Cypher Queries

### Query 1: Find Warranties Expiring Soon

**File:** `WarrantyRepository.java`

```java
@Query("""
    MATCH (w:Warranty)
    WHERE w.warrantyEndDate <= date() + duration('P30D')
    RETURN w
""")
List<Warranty> findWarrantiesExpiringSoon();
```

**Let's break it down:**

```cypher
MATCH (w:Warranty)                       -- Find all nodes labeled :Warranty
WHERE w.warrantyEndDate <= date() + duration('P30D')
                                         -- Filter: endDate ≤ today + 30 days
RETURN w                                 -- Return matching nodes
```

**Neo4j date functions:**
- `date()` → Current date (no time component)
- `duration('P30D')` → ISO 8601 duration: "Period of 30 Days"
- Adding them gives us "30 days from now"

---

### Query 2: Find Warranties by Customer ID

```java
@Query("""
    MATCH (c:Customer)-[:PURCHASED]->(s:Sale)-[:OF_PRODUCT]->(p:Product)-[:HAS_WARRANTY]->(w:Warranty)
    WHERE c.custId = $custId
    RETURN w
""")
List<Warranty> findWarrantiesByCustomerId(String custId);
```

**This is where graph databases shine!** Let's trace the path:

```
Customer ──PURCHASED──▶ Sale ──OF_PRODUCT──▶ Product ──HAS_WARRANTY──▶ Warranty
```

**In SQL, this would require 4 JOINs:**

```sql
SELECT w.* FROM warranties w
JOIN products p ON w.product_id = p.id
JOIN sale_products sp ON p.id = sp.product_id
JOIN sales s ON sp.sale_id = s.id
JOIN customers c ON s.customer_id = c.id
WHERE c.id = ?
```

**In Cypher, it reads like English:**

```cypher
MATCH (c:Customer)-[:PURCHASED]->(s:Sale)-[:OF_PRODUCT]->(p:Product)-[:HAS_WARRANTY]->(w:Warranty)
WHERE c.custId = $custId
RETURN w
```

> 💡 **The `$custId` syntax** is a parameterized query. Spring Data Neo4j automatically binds the method parameter to it, preventing Cypher injection attacks.

---

## 4. The Angular Fixed Layout (Navbar + Sidebar)

Our UI has three fixed regions:

```
┌────────────────────────────────────────────────────────┐
│                    NAVBAR (fixed top)                  │  height: 60px
├──────────────┬─────────────────────────────────────────┤
│              │                                         │
│   SIDEBAR    │           MAIN CONTENT                  │
│  (fixed left)│           (scrollable)                  │
│              │                                         │
│  width: 260px│                                         │
│              │                                         │
└──────────────┴─────────────────────────────────────────┘
```

### The Shell Component

**File:** `app.ts`

```typescript
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, SidebarComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  sidebarCollapsed = signal(false);      // Reactive state using Angular signals

  toggleSidebar(): void {
    this.sidebarCollapsed.update(v => !v); // Toggle true ↔ false
  }
}
```

**File:** `app.html`

```html
<app-navbar (toggleSidebar)="toggleSidebar()" />

<app-sidebar [collapsed]="sidebarCollapsed()" />

<main class="main-content" [class.sidebar-collapsed]="sidebarCollapsed()">
  <router-outlet />  <!-- Page content renders here -->
</main>
```

**How the communication works:**

1. User clicks hamburger menu in `<app-navbar>`
2. Navbar emits `(toggleSidebar)` event
3. `App` component calls `toggleSidebar()`, flipping the signal
4. Angular detects signal change, updates:
   - `<app-sidebar [collapsed]="true">` — slides left
   - `<main class="sidebar-collapsed">` — expands to full width

### CSS: Making Things "Fixed"

**File:** `app.css`

```css
.main-content {
  margin-top: 60px;           /* Space for fixed navbar */
  margin-left: 260px;         /* Space for fixed sidebar */
  min-height: calc(100vh - 60px);
  overflow-y: auto;           /* This area scrolls */
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.main-content.sidebar-collapsed {
  margin-left: 0;             /* Expand when sidebar hidden */
}
```

**File:** `navbar.component.css`

```css
.navbar {
  position: fixed;            /* ← KEY: Stays in place during scroll */
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  z-index: 1000;              /* Above everything else */
}
```

**File:** `sidebar.component.css`

```css
.sidebar {
  position: fixed;            /* ← KEY: Stays in place during scroll */
  top: 60px;                  /* Below navbar */
  left: 0;
  bottom: 0;
  width: 260px;
  height: calc(100vh - 60px); /* Full remaining height */
  overflow-y: auto;           /* Sidebar itself scrolls if content overflows */
  z-index: 900;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar.collapsed {
  transform: translateX(-260px);  /* Slide off-screen */
}
```

**Why `position: fixed`?**

| Position | Behavior |
|----------|----------|
| `static` | Normal document flow |
| `relative` | Normal flow, but can offset with `top/left` |
| `absolute` | Positioned relative to nearest positioned ancestor |
| `fixed` | **Positioned relative to the viewport. Doesn't move when scrolling.** |

---

## 5. Reactive State with Angular Signals

Angular 16+ introduced **signals** as a simpler alternative to RxJS for component state.

### What is a Signal?

A signal is a wrapper around a value that tells Angular when it changes:

```typescript
// Create a signal with initial value
products = signal<Product[]>([]);

// Read the current value (call it like a function)
console.log(this.products());  // []

// Update the value
this.products.set([newProduct]);

// Update based on previous value
this.products.update(list => [...list, anotherProduct]);
```

### Computed Signals

Computed signals derive values from other signals and auto-update:

**File:** `home.component.ts`

```typescript
// Source signals
allProducts = signal<Product[]>([]);
productSearch = signal('');
productPage = signal(0);
readonly productPageSize = 6;

// Computed: Automatically re-calculates when dependencies change
filteredProducts = computed(() => {
  const term = this.productSearch().toLowerCase();
  return this.allProducts().filter(p =>
    p.productName.toLowerCase().includes(term) ||
    p.productSerialNumber.toLowerCase().includes(term)
  );
});

pagedProducts = computed(() => {
  const start = this.productPage() * this.productPageSize;
  return this.filteredProducts().slice(start, start + this.productPageSize);
});

totalProductPages = computed(() =>
  Math.ceil(this.filteredProducts().length / this.productPageSize)
);
```

**The magic:** When `productSearch` changes, `filteredProducts` recalculates. When `filteredProducts` changes, `pagedProducts` and `totalProductPages` recalculate. Angular only re-renders what actually changed.

### Signals vs. RxJS Observables

| Feature | Signals | RxJS Observables |
|---------|---------|------------------|
| Syntax | `value()` to read | `value$ \| async` in template |
| Updates | `set()` / `update()` | `next()` on Subject |
| Derived values | `computed()` | `pipe(map(...))` |
| Learning curve | Low | High |
| Best for | Component state | Async streams (HTTP, WebSockets) |

> 💡 We still use RxJS for HTTP calls (`HttpClient` returns `Observable`), but convert to signals for component state.

---

## 6. Frontend-Backend Communication & CORS

### How Angular Talks to Spring Boot

**File:** `product.service.ts`

```typescript
@Injectable({ providedIn: 'root' })      // ① Singleton service
export class ProductService {
  private readonly http = inject(HttpClient);  // ② Inject HttpClient
  private readonly baseUrl = `${environment.apiBaseUrl}/products`;
                                         // ③ Base URL from environment

  getAll(): Observable<Product[]> {
    return this.http.get<Product[]>(this.baseUrl);
                                         // ④ GET request, typed response
  }

  create(product: Product): Observable<Product> {
    return this.http.post<Product>(this.baseUrl, product);
                                         // ⑤ POST with JSON body
  }
}
```

**File:** `environment.ts`

```typescript
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080'    // Backend URL
};
```

### What is CORS?

**CORS** = Cross-Origin Resource Sharing

When your browser loads a page from `http://localhost:4200` (Angular) and that page tries to fetch data from `http://localhost:8080` (Spring), the browser blocks it by default. This is a security feature to prevent malicious websites from stealing your data.

**The error you'd see without CORS config:**

```
Access to XMLHttpRequest at 'http://localhost:8080/products' from origin 
'http://localhost:4200' has been blocked by CORS policy: No 
'Access-Control-Allow-Origin' header is present on the requested resource.
```

### Enabling CORS in Spring Boot

**File:** `WebConfig.java`

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                    // All endpoints
                .allowedOrigins("http://localhost:4200")  // Only this origin
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")                  // Any header
                .allowCredentials(true);             // Allow cookies/auth
    }
}
```

**What each setting does:**

| Setting | Meaning |
|---------|---------|
| `addMapping("/**")` | Apply CORS to all URL paths |
| `allowedOrigins(...)` | Only requests from this URL are allowed |
| `allowedMethods(...)` | Only these HTTP methods are allowed |
| `allowedHeaders("*")` | Accept any request headers |
| `allowCredentials(true)` | Allow `Authorization` headers and cookies |

---

## 7. Error Handling Across the Stack

### Backend: Global Exception Handler

Instead of try/catch in every controller, we use a centralized handler:

**File:** `GlobalExceptionHandler.java`

```java
@RestControllerAdvice                    // ① Applies to all controllers
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)  // ② Catches this exception type
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)   // ③ Catch-all for unexpected errors
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
            "An unexpected error occurred: " + ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}
```

**Example error response:**

```json
{
  "timestamp": "2026-03-01T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product with id 'abc123' was not found"
}
```

### Frontend: Handling Errors in Services

```typescript
// In a component
this.productService.getAll().subscribe({
  next: (data) => this.products.set(data),
  error: (err) => {
    console.error('Failed to load products', err);
    // Could show a toast notification here
  }
});
```

For form submissions:

```typescript
this.productService.create(product).subscribe({
  next: (created) => {
    this.router.navigate(['/products', created.productId]);
  },
  error: (err) => {
    this.submitting.set(false);
    this.errorMsg.set('Failed to create product. Please try again.');
  }
});
```

---

## 🎉 Congratulations!

You've just learned:

1. **Backend architecture** — Controller → Service → Repository pattern
2. **Spring Data Neo4j** — How annotations map Java classes to graph nodes
3. **Cypher queries** — How to traverse relationships in a graph database
4. **Angular layout** — How `position: fixed` creates a shell with scrollable content
5. **Signals** — Angular's reactive state management
6. **CORS** — Why browsers block cross-origin requests and how to allow them
7. **Error handling** — Centralized exception handling on both ends

**Next steps:**
- Add authentication with Spring Security + JWT
- Implement real-time updates with WebSockets
- Add unit tests for services and components
- Deploy with Docker Compose (Neo4j + Spring Boot + Angular)

Happy coding! 🚀

