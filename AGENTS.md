# Project Instructions

## Tech Stack
- Backend: Spring Boot 3.2, Java 17, Spring Security JWT, Spring Data JPA, Redis cache, MySQL.
- Frontend: Vue 3, Vite 5, TypeScript, Pinia, Vue Router, vue-i18n, Tailwind CSS.
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
- Run `npm run typecheck`, `npm run build`, and `mvn -q test` before handing off production-facing changes.
- For Docker/deployment edits, also inspect `backend/Dockerfile`, `frontend/Dockerfile`, `docker-compose.yml`, and `docker-compose.prod.yml` together.
