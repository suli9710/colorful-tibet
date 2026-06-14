# Project Instructions

## Tech Stack
- Backend: Spring Boot 3.5.12, Java 17, Spring Security JWT, Spring Data JPA, Redis cache, MySQL.
- Frontend: Vue 3, Vite 8, TypeScript, Pinia, Vue Router, vue-i18n, Tailwind CSS.
- Deployment: Docker Compose with separate backend, frontend, and Redis services.

## Build & Run
- Frontend dev: `cd frontend && npm run dev`
- Frontend type check: `cd frontend && npm run typecheck`
- Frontend build: `cd frontend && npm run build`
- Backend compile: `cd backend && mvn -q -DskipTests compile`
- Backend tests: `cd backend && mvn -q test`
- Local compose: `docker compose up -d`
- Production compose: `docker compose -f docker-compose.prod.yml up -d`

## Project Structure
- `backend/src/main/java/com/tibet/tourism/controller/`: REST controllers under `/api/**`.
- `backend/src/main/java/com/tibet/tourism/service/`: business logic, recommendations, AI route generation, price updates.
- `backend/src/main/java/com/tibet/tourism/repository/`: Spring Data repositories.
- `backend/src/main/resources/application*.yml`: runtime profiles and environment-driven config.
- `frontend/src/views/`: route-level Vue pages.
- `frontend/src/components/`: reusable UI panels and widgets.
- `frontend/src/api/index.ts`: Axios client and endpoint registry.
- `frontend/src/stores/`: Pinia state stores.

## Conventions
- Keep backend controller methods thin; place business rules in services.
- Preserve `/api` as the frontend API base path; Vite and nginx both proxy it to the backend.
- Use environment variables for production secrets and deployment-specific hosts.
- Use structured logging through SLF4J in backend code instead of `System.out` or `System.err`.
- Keep frontend routes lazy-loaded and endpoint paths centralized in `frontend/src/api/index.ts`.
- Do not revert unrelated working-tree changes; this repository often has broad in-progress edits.

## Verification
- Run `npm run check` and `mvn -q test` before handing off production-facing changes.
- For Docker/deployment edits, also inspect `backend/Dockerfile`, `frontend/Dockerfile`, `docker-compose.yml`, and `docker-compose.prod.yml` together.

## Cursor Cloud specific instructions

The Cloud VM has no Docker; services run natively. JDK 17, Maven, MySQL 8, and Redis 7 are pre-installed in the snapshot (the update script only refreshes `frontend` npm deps and backend Maven deps). Standard build/run/test commands are in `## Build & Run` above; the notes below are the non-obvious caveats.

- **Default JDK is 17**: the snapshot pins `update-alternatives` to `java-17-openjdk-amd64` (Java 21 is also present but the build targets 17). If a tool ignores it, set `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`.
- **Start MySQL and Redis each session** (they do not auto-start): `sudo service mysql start` and `sudo service redis-server start`. The `colorful_tibet` database and the `colorful_tibet_app` MySQL user (password `localdevpassword`, host `localhost`/`127.0.0.1`) already exist in the snapshot. Flyway creates the schema on first backend boot.
- **Backend env / secrets**: the local-profile secrets live in `/home/ubuntu/colorful-tibet-backend.env` (gitignored, outside the repo, local-dev throwaway values; `REQUIRE_STRONG_SECRETS=false`). Run the backend with `cd backend && source /home/ubuntu/colorful-tibet-backend.env && mvn -q -DskipTests spring-boot:run`. It listens on `:8080`; check `curl localhost:8080/actuator/health`.
- **Seeded demo data**: with `SEED_CONTENT_ENABLED=true` and `SEED_DEMO_USERS=true`, the first boot seeds ~100 users, scenic spots, hotels and history. Demo logins: `admin`/`AdminDev123!`, super-admin `lzh`/`SuperDev123!`, `user1`/`UserDev123!`. The seeder is idempotent and skips when data already exists. Note: seeded demo accounts have `mustChangePassword=true`, so the very first login forces a password-change step (set a new password to proceed); after that, log in with the new password.
- **Frontend dev** (`cd frontend && npm run dev`, `:5173`) proxies `/api`, `/images`, `/uploads` to the backend on `:8080`, so the backend must be running for API-backed pages. `npm run dev` is the dev command — never build (`npm run build`) for development.
- **CSRF tokens are HMAC-bound to the auth session** (`XSRF-TOKEN` cookie + `X-XSRF-TOKEN` header, see `CsrfCookieFilter`/`CsrfTokenService`). When manually testing in a single long-lived browser session, a stale `XSRF-TOKEN` cookie can make state-changing POSTs (e.g. hotel booking submit) fail with a generic UI error ("提交失败") while the backend logs `Invalid CSRF token` (403). Fix: clear site cookies (or use a fresh login/registration) so the cookie re-syncs with the session — this is browser-session staleness, not a backend bug (a fresh login + immediate POST always works).
- **AI itinerary generation and price scraping are optional**: no `ARK_API_KEY`/`DOUBAO_API_KEY` is set (AI uses local fallback routes) and the `scrapler` microservice is not run by default (`SCRAPLING_HEALTH_ENABLED=false` so the backend boots without it).
- **`scrapler` test deps**: `requirements-dev.txt` is hash-pinned but contains an unpinned transitive (`uvloop`), so `pip install --require-hashes` fails (this is a pre-existing CI failure, not environment-specific). To run its 16 pytest cases locally, install with hashes stripped into a temp file. Scrapler is optional for the core product.
- **CI gap to know about**: no backend test boots the full Spring/JPA context (no `@SpringBootTest`/`@DataJpaTest`), so `mvn test` can stay green while the app fails to start. Always boot the backend (`spring-boot:run`) to validate context/wiring changes.
