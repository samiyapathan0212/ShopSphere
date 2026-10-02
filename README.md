# ShopSphere

Production-style full-stack e-commerce platform built with React/TypeScript and Spring Boot.

## Overview

ShopSphere is a production-style e-commerce portfolio project. A React single-page
application talks to a Spring Boot REST API backed by PostgreSQL for persistence and
Redis for caching. Docker Compose provides the local infrastructure. The project
covers the customer-facing shopping flow — browse, cart, wishlist, checkout, payment
confirmation, order history and tracking — plus role-protected admin order management.

## Features

### Customer

- **Registration and login** — email/password accounts with BCrypt-hashed credentials.
- **JWT access-token authentication** — short-lived bearer token on every API call.
- **Rotating refresh-token authentication** — silent session restore on reload.
- **Product browsing** — curated demo catalogue with home, listing and detail pages.
- **Search and filtering** — text search, category and price filters, and sorting in the
  current storefront.
- **Cart** — add, change quantity, remove and clear, with live navbar badge and totals.
- **Wishlist** — persist saved products and remove them again.
- **Checkout** — shipping-address form, order summary and a single guarded submission
  that cannot create duplicate orders.
- **Sandbox payment flow** — the checkout transaction charges a sandbox provider and
  returns the payment result, which the confirmation screen renders as approved or declined.
- **Order confirmation** — real order number, totals, address snapshot and payment status.
- **Order history** — paginated list of the signed-in customer's orders.
- **Order detail** — item snapshots, money breakdown, address, payment and status.
- **Order cancellation** — offered only while the order status permits it.
- **Current order tracking** — lifecycle position and cancellability from the API.
- **Light/dark theme persistence** — token-based theming stored in the browser.

> **Storefront catalogue is currently mock-backed on the frontend.** The home, product
> listing, product detail and wishlist pages render a curated demo catalogue from
> `frontend/src/mocks/`, and search/filter/sort are applied client-side. Every
> **transactional** operation — cart, wishlist membership, checkout, orders, payments and
> inventory — goes through the backend API and real PostgreSQL records. The backend also
> exposes a complete product catalogue API that the storefront does not yet consume.

### Admin

- **Protected admin access** — role-gated routes, hidden navigation and server-side
  authorization.
- **View customer orders** across all users.
- **Filter orders by status.**
- **View order details** — items, totals, address, customer id and payment status.
- **Update order status** through the transitions the backend lifecycle permits; an
  invalid transition is rejected by the server and surfaced in the UI.

Admin authorization is enforced by the backend. The frontend additionally guards the
admin pages and hides the navigation entry from non-admins, but this is convenience only —
the API answers `403` regardless.

**Backend-only, no admin UI yet:** product, category and inventory CRUD APIs exist and are
role-protected, but there is no management interface for them. Analytics, coupon
management and user management are not implemented.

## Architecture

```
React SPA
   ↓  REST (JSON, bearer JWT)
Spring Boot API
   ↓
PostgreSQL + Redis
   ↓
Sandbox payment abstraction
```

- **DTO boundaries** — controllers exchange `dto/request` and `dto/response` records;
  JPA entities never leave the service layer.
- **Layers** — controller → service → repository, with mappers translating entities to
  response DTOs.
- **Validation** — `jakarta.validation` constraints on every request record, returning a
  field-level error map.
- **Global exception handling** — domain exceptions map to meaningful HTTP statuses
  (400/404/409) through one central handler with a consistent error envelope.
- **Transactional checkout** — stock validation, order creation, payment and cart clearing
  run in a single transaction; any failure rolls the whole thing back.
- **Pessimistic inventory reservation** — stock rows are locked in ascending product-id
  order to serialize concurrent checkouts and avoid deadlocks, then converted to a sale or
  released.
- **Redis cache-aside** — category and product services read and populate caches through a
  small helper; cache failures are logged and never break a request.

## Technology Stack

**Frontend** — React 18 · TypeScript · Vite · Redux Toolkit · RTK Query · React Router ·
hand-written CSS with design tokens · Node.js/npm

**Backend** — Java 23 · Spring Boot 3.4.1 · Maven · Spring Security · JWT (jjwt) ·
Spring Data JPA / Hibernate · Bean Validation · Flyway · PostgreSQL · Redis ·
springdoc OpenAPI

**Infrastructure** — Docker · Docker Compose · PostgreSQL 16 · Redis 7 · Nginx

**Testing** — JUnit 5 · Mockito · AssertJ · MockMvc · H2 (test scope only)

## Security

- **Password hashing** — BCrypt via Spring Security; hashes only, never returned.
- **Access tokens** — short-lived HS256 JWTs carrying the user id, email and role,
  sent as `Authorization: Bearer`. Lifetime is configurable
  (`JWT_EXPIRATION_MS`, 15 minutes by default).
- **Refresh tokens** — opaque random values. Only a **SHA-256 hash** is stored, so a
  database leak does not expose usable tokens.
- **Single-use rotation** — every refresh revokes the presented token and issues a new
  one; expired or revoked tokens are rejected.
- **Transport** — the refresh token is delivered only in an `HttpOnly`, `Secure`,
  `SameSite=Lax` cookie scoped to `/api/auth`, never in a JSON body.
- **Authorization** — two roles, `CUSTOMER` and `ADMIN`, applied with method-level
  annotations on every endpoint; ownership checks live in the service layer.
- **Error handling** — JSON `401` for missing/invalid authentication and JSON `403` for
  insufficient role, with generic messages that do not leak which rule failed.
- **Stateless** — no server-side session; CSRF protection is disabled because the API is
  stateless and token-authenticated.
- **Frontend** — the access token is held in the Redux auth slice and refreshed silently
  on load; admin pages are route-guarded in addition to server-side enforcement.

No real secrets, tokens or credentials are stored in this repository. `.env` files are
git-ignored; only `.env.example` with development placeholders is tracked.

## Checkout and Order Flow

```
Cart
  → Checkout (address + summary)
  → Inventory reservation (pessimistic lock)
  → Order creation (immutable item/address snapshots)
  → Sandbox payment
  → Order confirmation
  → Order status management
```

Stock is reserved before the order is created and only converted to a sale once payment is
approved; if the sandbox provider declines, the reservation is released, the order is
cancelled and the cart is left intact so the customer can retry. Reservation and release
are part of the checkout transaction, and insufficient stock is rejected rather than
oversold. Prices, shipping and totals are always computed server-side from catalogue
records, never from client input.

## Database and Migrations

Flyway manages all schema evolution; the application validates the schema and never
mutates it at runtime.

| Migration | Purpose |
| --- | --- |
| `V1__baseline.sql` | Baseline marker |
| `V2__create_users_table.sql` | Users (unique email, BCrypt hash, role) |
| `V3__create_refresh_tokens_table.sql` | Refresh tokens (unique hash, expiry, revocation) |
| `V4__create_categories_and_products.sql` | Categories and products |
| `V5__create_cart_wishlist_and_review_tables.sql` | Carts, cart items, wishlists, wishlist items, reviews |
| `V6__create_inventory_order_payment_tables.sql` | Inventory, orders, order items, payments |
| `V7__fix_cart_item_quantity_type.sql` | Cart item quantity column correction |
| `V8__fix_review_rating_type.sql` | Review rating column correction |
| `V9__seed_shopsphere_demo_catalog.sql` | Demo catalogue seed (14 canonical products) |
| `V10__seed_demo_inventory.sql` | Demo inventory seed (usable stock for the demo catalogue) |

Uniqueness is enforced in the database for user email, refresh-token hash, category name,
product SKU, one cart and wishlist per user, one review per user/product, one inventory row
per product, order number, and one payment per order.

## API Overview

Representative endpoint groups:

| Group | Endpoints |
| --- | --- |
| Auth | `/api/auth/register`, `/login`, `/refresh`, `/logout`, `/me` |
| Products | `/api/products`, `/api/products/{id}` |
| Categories | `/api/categories`, `/api/categories/{id}` |
| Reviews | `/api/products/{id}/reviews`, `/api/reviews/{id}` |
| Cart | `/api/cart`, `/api/cart/items`, `/api/cart/items/{productId}` |
| Wishlist | `/api/wishlist`, `/api/wishlist/items/{productId}` |
| Checkout | `/api/checkout` |
| Orders | `/api/orders`, `/api/orders/{id}`, `/api/orders/{id}/tracking`, `/api/orders/{id}/cancel` |
| Payments | `/api/payments/{orderId}`, `/api/payments/{orderId}/process`, `/api/payments/{orderId}/verify` |
| Admin orders | `/api/admin/orders`, `/api/admin/orders/{id}`, `/api/admin/orders/{id}/status` |
| Inventory | `/api/inventory`, `/api/inventory/{productId}` |

Swagger UI and the OpenAPI document are served by the running backend at
`/swagger-ui.html` and `/v3/api-docs`.

## Local Setup

### Docker (all services)

```powershell
Copy-Item .env.example .env
docker compose up -d --build
docker compose ps
```

| Service | URL |
| --- | --- |
| Frontend | http://localhost:3000 |
| Backend | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |

### Prerequisites

JDK 23+, Maven 3.9+, Node.js 20+, and Docker with Docker Compose.

### Local development

Infrastructure only:

```powershell
docker compose up -d postgres redis
```

Backend on http://localhost:8080:

```powershell
cd backend
mvn spring-boot:run
```

Frontend on http://localhost:3000 (Vite proxies `/api` to the backend):

```powershell
cd frontend
npm install
npm run dev
```

Stop the containers with `docker compose down` (add `-v` to also delete the database and
cache volumes).

### Environment variables

`.env` is read by Docker Compose. The backend also reads these directly, so they work for
`mvn spring-boot:run`:

| Variable | Purpose |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Redis connection for caching |
| `JWT_SECRET` | base64 signing key for access tokens (**set a strong value in production**) |
| `JWT_EXPIRATION_MS` | Access-token lifetime (default 15 minutes) |
| `REFRESH_TOKEN_EXPIRATION_MS` | Refresh-token lifetime (default 30 days) |
| `SERVER_PORT` | Backend HTTP port |
| `CATEGORY_CACHE_TTL_SECONDS`, `PRODUCT_CACHE_TTL_SECONDS`, `PRODUCT_LIST_CACHE_TTL_SECONDS` | Catalog cache TTLs |
| `CATALOG_MAX_PAGE_SIZE` | Maximum catalogue page size |
| `VITE_API_URL` | Frontend API base URL (defaults to `/api`) |

## Testing

Backend — 111 tests covering services, persistence, validation, and controller
authorization:

```powershell
cd backend
mvn -B test
```

Frontend — type-check and production build:

```powershell
cd frontend
npm run build
```

CI runs `mvn -B verify` and `npm ci && npm run build` on every push and pull request.

## Project Layout

```
backend/    Spring Boot API (config, security, controller, service, repository,
            domain, dto, mapper, exception) + Flyway migrations
frontend/   React SPA (app/api slices, components, layouts, pages, mocks,
            hand-written CSS)
.github/    CI workflow
docker-compose.yml
.env.example
```

## Known Limitations

- **Storefront catalogue is mock-backed.** Home, product listing, product detail and the
  wishlist render curated demo data from `frontend/src/mocks/`, and search, filtering,
  sorting and pagination run client-side. The backend catalogue API is complete but not
  yet consumed by these pages; wiring them to it is the main outstanding frontend task.
- **No frontend automated tests.** Frontend assurance comes from the production build
  (type-checked) and manual verification; all 111 automated tests are backend.
- **Direct API usage requires an initial cart read.** `POST /api/cart/items` returns
  `404` if `GET /api/cart` has not been called first, because the cart row is created
  lazily on read. The web UI always loads the cart first, so this only affects API
  consumers.
- **Admin orders expose the customer id only.** The admin order response carries
  `userId` but no customer name or email, so the admin UI shows the id.
- **Admins cannot see payment provider details.** Payment reads are ownership-scoped to
  the customer, and there is no refund endpoint, so the admin view shows payment status
  only.
- **No carrier tracking history.** The backend stores a single current order status; no
  shipment scan events are recorded.
- **Payments are simulated.** A sandbox provider approves charges by default and no card
  data is collected.
- **No admin UI for products, categories or inventory.** Those CRUD APIs exist and are
  role-protected, but no management screens are built.
- **Admin accounts must be created by promoting a user** in the database; registration
  always produces a `CUSTOMER`.

## Demo Data

Migrations `V9` and `V10` seed 14 canonical demo products with usable stock, so the
checkout flow works immediately after a fresh start.