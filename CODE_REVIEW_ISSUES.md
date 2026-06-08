# Code Review Issues

Last updated: 2026-06-08
Last reviewed: 2026-06-08 16:51:11 +08:00

This file tracks issues re-checked against the current worktree. Items marked fixed have code and tests in this worktree; residual items remain candidates for the next 10-minute review cycle.

## Verification Snapshot

- Backend: `mvn -q -DskipTests compile`, `mvn -q -Dtest="com.tibet.tourism.common.security.*Test,com.tibet.tourism.modules.auth.**.*Test,com.tibet.tourism.modules.user.**.*Test" test`, and `mvn -q test` passed on 2026-06-08 16:49 +08:00. Surefire summary: 95 report files, 795 tests, 0 failures, 0 errors, 0 skipped.
- Frontend: `npm run test -- --run src/components/adminA11y.test.ts src/components/AiRouteFloatingBall.test.ts src/api/client.test.ts src/api/sse.test.ts src/utils/sanitize.test.ts src/views/richTextXss.test.ts` passed on 2026-06-08 16:49 +08:00 (6 files / 21 tests). `npm run check` also passed (`vue-tsc`, 30 Vitest files / 148 tests, and production build).
- Review agents: 4 code-review agents completed on 2026-06-08 16:49 +08:00. They confirmed CT-SEC-001/002/003 and private upload fixes were not reopened, revalidated residual BE/FE/OPS items, and added CT-BE-009, CT-FE-010, CT-FE-011, and CT-OPS-009.
- Diff hygiene: `git diff --check` passed on 2026-06-08 16:48 +08:00; it only reported LF-to-CRLF warnings.
- Known non-failing warnings: stale Browserslist/baseline data, Rolldown pure annotation warnings in `@vueuse/core`, and expected negative-path backend logs.

## Fixed This Cycle

- CT-SEC-001: Authenticated `/api/guide/chat` now requires signed double-submit XSRF when an `AUTH_TOKEN` cookie is present. Anonymous public POST behavior remains allowed with browser metadata checks.
- CT-SEC-002: Controller authorization matrix now discovers real module `@RestController` classes automatically and covers previously omitted controllers.
- CT-SEC-003: CSRF trusted origins no longer accept wildcard patterns that CORS rejects when credentials are enabled.
- Private uploads: `/uploads/private/**` is excluded from public reads and denied for authenticated users too.
- CT-FE-001: Admin 401 responses now use centralized auth expiration unless a request explicitly sets `skipAuthRedirect`.
- CT-FE-002: Reported hardcoded endpoint paths were moved to `frontend/src/api/endpoints.ts` / auth modules, including follow-up profile/community/route share paths.
- CT-FE-003: Public rich text now strips remote HTTP(S) images while preserving local, same-origin, and bounded safe data images.
- CT-FE-004: SSE parsing now enforces buffer/frame byte limits and cancels hostile streams on overflow.
- CT-BE-002: Redis guide-chat window-limit rejections no longer consume daily quota; race compensation was added.
- CT-BE-003: Duplicate AI route job creation now has single-JVM serialization plus Redis start locks with TTL for cross-instance duplicate starts.
- CT-BE-004: AI route job query/stream now falls back to persisted RUNNING records when memory state is missing, and startup recovery marks stale RUNNING records failed instead of leaving them forever active.
- CT-BE-005: Itinerary expansion now uses Hibernate batch fetching on days/items and related spot/hotel/room type references to reduce N+1 pressure.
- CT-BE-001: Scrapling prices are no longer hardcoded reference-only when upstream explicitly marks a result as publishable/non-reference; default behavior remains conservative.
- CT-BE-007: Batch price updates now delegate single-spot work to `SingleSpotPriceUpdateService` with `REQUIRES_NEW`, including async batch paths.
- CT-BE-008: Price update focused tests were updated for constructor injection and now compile/pass.
- CT-FE-005: Live2D rendering is gated behind desktop interaction or idle loading, with a lightweight placeholder before the model loads.
- CT-FE-006: Admin dashboard/community/heritage clickable panels now expose keyboard semantics with `role`, `tabindex`, accessible labels, and Enter/Space handlers; action controls use real buttons.

## Open / Residual Issues

### CT-BE-004 - Medium - AI route job recovery is query-safe but not fully live-stream recoverable

- Status: Partially fixed.
- Evidence: `backend/src/main/java/com/tibet/tourism/modules/ai/application/AiRouteGenerationJobService.java:189` can construct a record-backed RUNNING job when in-memory `jobs` state is missing, but that object is not connected to the original producer/subscriber list.
- Residual risk: Query/stream no longer returns `not found` for persisted RUNNING records, and startup recovery marks stale RUNNING records failed. True cross-instance live SSE recovery still needs shared stream state, Redis/pub-sub, a message table, or worker-affinity routing while a job is actively running.

### CT-BE-005 - Medium - Itinerary list/detail still expands full object graphs

- Status: Partially fixed.
- Evidence: `Itinerary`/`ItineraryDay`/`ItineraryItem` now use Hibernate `@BatchSize`, but `backend/src/main/java/com/tibet/tourism/modules/route/application/ItineraryService.java:97` still lists through `findByUserIdOrderByCreatedAtDesc()` and maps each item through full `toResponse()` at `ItineraryService.java:474`, expanding days, items, spot, hotel, and room type associations. `backend/src/main/java/com/tibet/tourism/modules/route/infra/ItineraryRepository.java:11` has no paging or summary fetch plan.
- Residual risk: Batch fetching reduces query count, but list/detail responses can still perform avoidable lazy batches and transfer full detail DTOs where summaries or detail-specific fetch plans would be safer.

### CT-BE-001 - Medium - Reference-price review storage is still missing

- Status: Partially fixed.
- Evidence: `backend/src/main/java/com/tibet/tourism/modules/spot/application/SingleSpotPriceUpdateService.java:49` skips reference-only or low-confidence fetched prices without saving a durable observation. `backend/src/main/java/com/tibet/tourism/modules/spot/domain/ScenicSpot.java:41` exposes formal ticket-price fields, but there is no review/reference/evidence table or field for rejected candidates.
- Residual risk: High-confidence explicit Scrapling publish candidates can persist, but non-publishable/reference results still have no durable review/reference queue.

### CT-BE-006 - Medium - Recommendation behavior writes and cache invalidation may be missing

- Status: Needs product confirmation.
- Evidence: `RecommendationService` reads `UserVisitHistory` and exposes `invalidateUserCache()`, but runtime `UserVisitHistory` writes and business calls to `invalidateUserCache()` were not found outside seed/test flows. Representative gaps: `backend/src/main/java/com/tibet/tourism/modules/community/web/FavoriteController.java:76` writes favorites without recommendation invalidation, and `backend/src/main/java/com/tibet/tourism/modules/spot/web/ScenicSpotController.java:79` returns spot detail without visit-history write.
- Suggested fix: If recommendations should react to views, favorites, ratings, or orders, add behavior writes and user cache invalidation at those actions.

### CT-BE-009 - Medium - Order list is unpaged and expands every child collection

- Status: Open.
- Evidence: `backend/src/main/java/com/tibet/tourism/modules/order/application/OrderCenterService.java:209` returns all of a user's orders through `findByUserIdOrderByCreatedAtDesc()`, while `OrderCenterService.java:1173` maps each order to a full response including items, payment transactions, refunds, vouchers, and invoices. `backend/src/main/java/com/tibet/tourism/modules/order/infra/PlatformOrderRepository.java:30` is a plain `List` query with no `Pageable` or summary projection.
- Suggested fix: Make the list endpoint paged and summary-oriented, then load the full child graph through a detail endpoint with an explicit fetch plan.

### CT-FE-007 - Low/Policy - Heat map falls back to third-party geo JSON

- Status: Needs product/deployment policy confirmation.
- Evidence: `frontend/src/components/HeatMap.vue:127` still fetches `https://geo.datav.aliyun.com/areas_v3/bound/540000_full.json` when local geo JSON loading fails.
- Suggested fix: Keep with explicit CSP/privacy documentation, or remove/config-gate the fallback for offline/privacy-strict deployments.

### CT-FE-008 - Low/Policy - Dynamically injected third-party scripts may conflict with strict CSP

- Status: Needs CSP target confirmation.
- Evidence: `frontend/src/utils/amap.ts:37` and `frontend/src/utils/recaptcha.ts:81` still assign dynamic third-party script URLs for AMap and reCAPTCHA without nonce/SRI handling.
- Suggested fix: Add nonce support and document exact script-src policy for AMap and reCAPTCHA.

### CT-FE-009 - Low - Admin/frontend types still rely on broad `any`

- Status: Open.
- Evidence: `frontend/src/views/AdminDashboard.vue:1111` still uses `any[]` for stats payloads, helpers accept `route: any` around `AdminDashboard.vue:1321`, and admin state still uses `ref<any[]>` / `ref<any>` for spots, news, carousels, routes, hotels, and room types around `AdminDashboard.vue:1391`, `1408`, `1884`, `1934`, `2037`, and `2090`. `AdminCommunityPanel.vue` and `AdminHeritagePanel.vue` now have typed list helpers covered by `frontend/src/components/adminA11y.test.ts`.
- Suggested fix: Add typed admin DTOs and `PageResponse<T>` in `frontend/src/api/types.ts` or derive them from generated schema types, then use typed refs and typed `api.get<T>()`/partial update payloads.

### CT-FE-010 - Medium - AI route job SSE paths still bypass the endpoint registry

- Status: Open.
- Evidence: `frontend/src/api/stream.ts:149`, `stream.ts:162`, and `stream.ts:183` hardcode `/routes/generate/jobs...` paths, while `frontend/src/api/endpoints.ts:9` has no builders for route job creation, job detail, or job stream URLs.
- Suggested fix: Add `routes.generateJob`, `routes.generateJobDetail(jobId)`, and `routes.generateJobStream(jobId)` builders to `frontend/src/api/endpoints.ts`, then consume those builders from `stream.ts`.

### CT-FE-011 - Medium - Major route/order flows still bypass i18n

- Status: Open.
- Evidence: `frontend/src/views/RoutePlanner.vue` still has user-visible Chinese and `zh-CN` formatting outside `t(...)`, including representative lines `77`, `768`, `895`, `1325`, `1337`, `1341`, `1432`, `1709`, and `2291`. `frontend/src/views/OrderCenter.vue` has the same issue for page copy, status labels, tabs, metrics, errors, confirms, date/currency formatting, and item labels around lines `12`, `104`, `126`, `218`, `292`, `418`, `456`, `484`, `502`, `560`, `587`, `598`, and `614`.
- Suggested fix: Move labels/messages/ARIA strings into `zh.json` and `bo.json`, and drive currency/date formatting from the active locale instead of hardcoded `zh-CN`.

### CT-OPS-001 - High - Remote frontend script falls back to local `/api` proxy

- Status: Open.
- Evidence: `frontend/start-remote.bat:30` still sets `VITE_API_BASE_URL=http://1.15.29.168:6000/api`, while `frontend/src/utils/apiOrigin.ts:31` ignores cross-origin API base URLs and `frontend/vite.config.ts:29` proxies `/api` to `http://localhost:8080`.
- Suggested fix: Configure the Vite proxy target for remote debugging, or explicitly support cross-origin auth/CSRF/CORS.

### CT-OPS-002 - Medium - Production Prometheus scrape is incompatible with default metrics authorization

- Status: Open.
- Evidence: `docker-compose.prod.yml:40` defaults `PUBLIC_METRICS_ENABLED=false`, `monitoring/prometheus/prometheus.yml:15` scrapes `/actuator/prometheus` without credentials, and `backend/src/main/java/com/tibet/tourism/common/security/WebSecurityConfig.java:102` / `104` requires `ADMIN` for that path when public metrics are disabled.
- Suggested fix: Add authenticated Prometheus scraping, a dedicated internal metrics path, or set and document `PUBLIC_METRICS_ENABLED=true` only for a trusted metrics network.

### CT-OPS-003 - Medium - Redis/MySQL secrets appear in process arguments or health checks

- Status: Open.
- Evidence: `docker-compose.prod.yml:127` still passes Redis password through `redis-server --requirepass`, `docker-compose.prod.yml:136` uses `redis-cli -a`, and `docker-compose.prod.yml:160` passes the MySQL root password through `mysqladmin ... -p`.
- Suggested fix: Use Docker secrets or restricted config files for service credentials, and avoid passwords in argv-based health checks.

### CT-OPS-004 - Medium - Deployment scripts make `data` and `logs` world-writable

- Status: Open.
- Evidence: `upload-server.ps1:261` and `deploy-new-server-http.ps1:392` still run `chmod -R a+rwX "$PROJECT_DIR/data" "$PROJECT_DIR/logs"`.
- Suggested fix: `chown` to the container UID/GID or a dedicated group and use narrower permissions such as `750`/`770`.

### CT-OPS-005 - Low - `stop.bat` is machine-specific and uses Unix redirection

- Status: Open.
- Evidence: `stop.bat:2` uses `>/dev/null`, `stop.bat:10` hardcodes `/mnt/c/Users/Suli/Desktop/colorful-tibet`, and `stop.bat:13` hardcodes the `Ubuntu` WSL distribution.
- Suggested fix: Use `>nul`, derive the project path from `%~dp0`, and reuse the WSL distribution detection approach from `start.ps1`.

### CT-OPS-006 - Medium - Legacy PII key can be empty while v1 ciphertext may remain

- Status: Needs confirmation.
- Evidence: `docker-compose.prod.yml:34` allows empty `PII_ENCRYPTION_KEY`, `backend/src/main/resources/application-prod.yml:158` reads it with an empty default, and `backend/src/main/java/com/tibet/tourism/common/security/PiiCryptoConverter.java:101` can return raw `enc:v1:` data when `legacyV1Key` is absent.
- Suggested fix: Confirm whether v1 data exists. If it does, require the legacy key until migration is complete, or fail startup when v1 data is detected without a key.

### CT-OPS-007 - Low - Nginx may overwrite client forwarding chain

- Status: Needs production topology confirmation.
- Evidence: `docker-compose.prod.yml:44` defaults `TRUST_PROXY_HEADERS=true`, backend trusted-proxy logic reads `X-Forwarded-For`, and `frontend/nginx.conf:96`, `119`, `160`, and `220` set `X-Forwarded-For` to `$remote_addr`.
- Suggested fix: If there is an upstream CDN or load balancer, configure `real_ip_header`/trusted upstreams and use `$proxy_add_x_forwarded_for`.

### CT-OPS-008 - Low - Supply-chain inputs are not fully pinned

- Status: Needs release policy confirmation.
- Evidence: `backend/Dockerfile:2`, `frontend/Dockerfile:2`, and `frontend/Dockerfile:24` use mutable image tags; `scrapler/requirements.txt:1` and related lines use version ranges; `.github/workflows/ci.yml:19` uses action tags.
- Suggested fix: For production releases, pin image digests and GitHub Actions SHAs, and add locked or hashed Python dependency inputs.

### CT-OPS-009 - Medium - Production super-admin username is not fail-fast at compose validation

- Status: Open.
- Evidence: `docker-compose.prod.yml:47` passes `${SUPER_ADMIN_USERNAME}` through without a required-variable guard, while `backend/src/main/resources/application-prod.yml:181` binds it to `app.super-admin-username` and `backend/src/main/java/com/tibet/tourism/modules/auth/application/AuthApplicationService.java:105` / `109` fails later in prod/strict mode if it is blank. `docker compose -f docker-compose.prod.yml config --quiet` only warns when the variable is unset.
- Suggested fix: Change the compose entry to `SUPER_ADMIN_USERNAME=${SUPER_ADMIN_USERNAME:?SUPER_ADMIN_USERNAME is required in production}` and mirror the check in deployment preflight scripts.
