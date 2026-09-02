# ShopSphere

E-commerce platform. **Phase 1: project foundation** — no business features
(auth, products, cart, wishlist, reviews, orders, payments, admin, analytics)
are implemented yet.

## Tech stack

| Layer       | Technology                                   |
| ----------- | -------------------------------------------- |
| Backend     | Java 23, Spring Boot 3.4, Maven              |
| Frontend    | React 18, TypeScript, Vite                   |
| Frontend UI state | Redux Toolkit + RTK Query              |
| Database    | PostgreSQL                                   |
| Cache       | Redis                                        |
| Migrations  | Flyway                                       |
| API docs    | springdoc OpenAPI (Swagger UI)               |
| Testing     | JUnit 5 + Mockito (backend)                  |

## Project layout

```
backend/   Spring Boot application (backend/src/main/java/com/shopsphere/backend)
frontend/  React + TypeScript application (frontend/src)
docker-compose.yml   PostgreSQL, Redis, backend, frontend
.github/workflows/ci.yml   GitHub Actions CI
```

Backend packages: `config`, `security`, `controller`, `service`, `repository`,
`domain`, `dto/request`, `dto/response`, `mapper`, `exception`, `validation`,
`util`. Only `config` and `controller` are populated in Phase 1; the remaining
packages are reserved for later phases.

## Prerequisites

- JDK 23+ (`java -version`)
- Maven 3.9+ (`mvn -version`)
- Node.js 20+ (`node -v`)
- Docker + Docker Compose (optional, for the containerized setup)

## Local development setup

### 1. Environment variables

Copy the example environment file and adjust as needed (defaults match the
local Docker setup):

```bash
cp .env.example .env
```

### 2. Infrastructure (PostgreSQL + Redis)

```bash
docker compose up -d postgres redis
```

Alternatively use any locally running PostgreSQL/Redis and point the variables
in `.env` at them.

### 3. Backend

```bash
cd backend
mvn spring-boot:run
```

- Health check: http://localhost:8080/api/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Flyway runs the baseline migration (`V1__baseline.sql`) on startup. No business
tables are created in Phase 1.

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:3000 — the Vite dev server proxies `/api` to the backend
at `http://localhost:8080`.

## Docker Compose (all services)

```bash
docker compose up --build
```

| Service  | URL                          |
| -------- | ---------------------------- |
| Frontend | http://localhost:3000        |
| Backend  | http://localhost:8080        |
| Swagger  | http://localhost:8080/swagger-ui.html |
| Postgres | localhost:5432               |
| Redis    | localhost:6379               |

## CI

GitHub Actions (`.github/workflows/ci.yml`) runs on push/PR against `main` and
`master`:

- Backend: `mvn -B verify` (compiles + runs JUnit/Mockito tests)
- Frontend: `npm ci && npm run build` (type-check + Vite build)

## Security note

`.env` files and real credentials are never committed. Only `.env.example`
(local development defaults) is tracked. Treat any real secret as a deploy
secret, not a repo file.