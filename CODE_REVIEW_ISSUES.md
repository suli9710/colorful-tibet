# Code Review Issues

Last updated: 2026-06-08
Last reviewed: 2026-06-08 18:02:24 +08:00

This file tracks issues re-checked against the current worktree. Items marked fixed have code and tests in this worktree; residual items remain candidates for the next 10-minute review cycle.

## Verification Snapshot

- Backend: `mvn -q -DskipTests compile`, focused security/auth/user tests, and `mvn -q test` passed earlier; latest full `mvn -q test` passed again on 2026-06-08 17:42 +08:00.
- Frontend: latest full `npm run check` passed on 2026-06-08 17:39 +08:00 (`vue-tsc`, 34 Vitest files / 158 tests, and production build).
- Latest focused follow-up: `npm run test -- --run src/utils/devProxyTarget.test.ts` passed on 2026-06-08 16:55 +08:00 (1 file / 3 tests). `cmd /c stop.bat --dry-run` passed on 2026-06-08 16:57 +08:00 and resolved the project to `/mnt/c/Users/Suli/Desktop/colorful-tibet`. `npm run test -- --run src/utils/scriptSecurity.test.ts src/utils/amap.test.ts src/utils/recaptcha.test.ts` passed on 2026-06-08 17:00 +08:00 (3 files / 8 tests). `npm run test -- --run src/api/stream.test.ts` passed on 2026-06-08 18:01 +08:00 (1 file / 9 tests).
- Current failing verification: `mvn -q -Dtest="com.tibet.tourism.common.security.*Test" test` failed on 2026-06-08 18:01 +08:00 because `UploadResourceSecurityHeadersTest` cannot load `WebSecurityConfig` without a `SecurityAccessDeniedHandler` bean in that WebMvc slice; Surefire also recorded `WebSecurityConfigPublicAccessTest` context errors in the same run.
- Compose preflight check: `docker compose --env-file .env.example -f docker-compose.prod.yml config --quiet` and `docker compose --env-file .env.example -f docker-compose.yml config --quiet` exited 0 on 2026-06-08 17:35 +08:00 after the Redis/MySQL healthcheck secret handling update. A focused scan for `redis-server ... --requirepass`, `redis-cli -a`, and `mysqladmin ... -p` returned no matches in either Compose file.
- Negative compose preflight for CT-OPS-009: with all other `.env.example` values loaded and `SUPER_ADMIN_USERNAME` omitted, `docker compose -f docker-compose.prod.yml config --quiet` now exits 1 on 2026-06-08 18:02 +08:00 with the required-variable error. Positive production compose config still exits 0 with `.env.example`.
- This cycle's testing agents completed: backend `mvn -q test` passed with 95 Surefire XML files / 795 tests; frontend `npm run check` passed with 34 Vitest files / 158 tests and production build.
- This cycle's 4 code-review agents returned by 2026-06-08 17:59 +08:00. They revalidated existing BE/FE/OPS residuals, confirmed CT-OPS-003 should stay fixed, and added CT-BE-010/011/012/013/014, CT-FE-012/013/014, and CT-OPS-010/011/012.
- Diff hygiene: `git diff --check` passed on 2026-06-08 17:39 +08:00; it only reported LF-to-CRLF warnings.
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
- CT-FE-007: Heat map remote geo JSON fallback is now disabled by default and gated behind `VITE_HEATMAP_REMOTE_GEO_FALLBACK_ENABLED`; local geo JSON remains the default path.
- CT-FE-008: AMap and reCAPTCHA dynamic loaders now apply shared third-party script security attributes, including CSP nonce, optional SRI, and `crossOrigin`, with env/meta/runtime nonce support covered by focused tests.
- CT-FE-010: AI route stream/job APIs now use `endpoints.routes.generateStream`, `generateJob`, `generateJobDetail(jobId)`, and `generateJobStream(jobId)` from the endpoint registry; `frontend/src/api/stream.test.ts` covers the generated fetch URLs.
- CT-OPS-001: Vite dev proxy target now derives from remote `VITE_API_BASE_URL`/`VITE_DEV_PROXY_TARGET` through `resolveDevProxyTarget`, so `frontend/start-remote.bat` no longer silently falls back to local `localhost:8080` for `/api` during remote debugging.
- CT-OPS-002: Production Compose now defaults `PUBLIC_METRICS_ENABLED=true` for the bundled internal Prometheus scrape, with docs/comments requiring trusted backend/monitoring networks or an explicit protected metrics path when disabled.
- CT-OPS-003: Production Compose no longer exposes Redis/MySQL credentials through the old long-lived `redis-server --requirepass`, `redis-cli -a`, or `mysqladmin -p...` command arguments; Redis now boots from a restricted temporary config file with an escaped `requirepass`, Redis healthcheck uses `REDISCLI_AUTH`, and local/production MySQL healthchecks use a temporary `--defaults-extra-file`.
- CT-OPS-005: `stop.bat` now uses Windows `>nul`, derives the project path from `%~dp0`, detects/overrides the WSL distribution, and supports `--dry-run`.
- CT-OPS-009: Production Compose now requires `SUPER_ADMIN_USERNAME` during compose interpolation, so missing super-admin configuration fails during preflight instead of later at backend startup.

## Open / Residual Issues

### CT-BE-004 - Medium - AI route job recovery is query-safe but not fully live-stream recoverable

- Status: Partially fixed.
- Evidence: `backend/src/main/java/com/tibet/tourism/modules/ai/application/AiRouteGenerationJobService.java:189` can construct a record-backed RUNNING job when in-memory `jobs` state is missing, but that object is not connected to the original producer/subscriber list. The durable fallback lookup at `AiRouteGenerationJobService.java:451` only works by `jobId`, while `backend/src/main/java/com/tibet/tourism/modules/ai/application/AiRouteRecordService.java:66` and `97` clear `jobId` on completed/failed records.
- Residual risk: Query/stream no longer returns `not found` for persisted RUNNING records, and startup recovery marks stale RUNNING records failed. True cross-instance live SSE recovery still needs shared stream state, Redis/pub-sub, a message table, or worker-affinity routing while a job is actively running. Completed/failed jobs also stop being durable by job URL after in-memory retention cleanup because terminal records no longer retain the external `jobId`.
- Suggested fix: Keep a durable external job/request id until job URLs expire, or introduce a separate job table/event log that can serve terminal snapshots and replayable stream history after memory cleanup.

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

### CT-BE-010 - Medium - Refund requests have no production review/completion path

- Status: Open.
- Evidence: `backend/src/main/java/com/tibet/tourism/modules/order/application/OrderCenterService.java:254` creates a `RefundOrder`, and `OrderCenterService.java:267` transitions the order to `REFUND_PENDING`. `backend/src/main/java/com/tibet/tourism/modules/order/domain/RefundOrder.java:85` defines `REQUESTED`, `APPROVED`, `REJECTED`, and `COMPLETED`, but production code only exposes the user request endpoint at `backend/src/main/java/com/tibet/tourism/modules/order/web/OrderCenterController.java:101`; no admin/provider callback path updates refund status or `processedAt`.
- Residual risk: Orders can remain permanently in "after-sales/refund pending" states, while `REFUNDED`, partial refund semantics, item refund status, and `processedAt` cannot be produced by normal business flow.
- Suggested fix: Add an admin/ops refund review endpoint or payment-provider callback that atomically updates `RefundOrder.status/processedAt`, order payment/status, order items, and payment transactions, with concurrency tests.

### CT-BE-011 - Medium - Profile stats and comments still load full user histories

- Status: Open.
- Evidence: `backend/src/main/java/com/tibet/tourism/modules/user/application/CurrentUserApplicationService.java:71` counts routes by loading `findByAuthorOrderByCreatedAtDesc(user).size()`, and `CurrentUserApplicationService.java:73` counts bookings by loading `bookingRepository.findByUserId(userId).size()`. `CurrentUserApplicationService.java:85` and `88` return full spot and route comment lists for `/me/comments`.
- Residual risk: Active users can make profile endpoints increasingly expensive in DB load, memory, and serialization, compounding the existing unpaged order/itinerary list risks.
- Suggested fix: Use repository count queries for stats, page `/me/comments`, and keep profile summary endpoints separate from paginated history endpoints.

### CT-BE-012 - Medium - Order creation accepts unbounded item lists

- Status: Open.
- Evidence: `backend/src/main/java/com/tibet/tourism/modules/order/web/dto/CreateOrderRequest.java:22` validates `items` with `@NotEmpty` but no `@Size(max = ...)`. `backend/src/main/java/com/tibet/tourism/modules/order/application/OrderCenterService.java:190` iterates every requested item to build order items, and `OrderCenterService.java:682` creates inventory locks for every item/service date.
- Residual risk: An authenticated user can submit a very large order item list and amplify validation, DB lookups, lock creation, and transaction time.
- Suggested fix: Add a DTO `@Size(max = N)` and mirror the hard limit in service validation before creating order items or inventory locks.

### CT-BE-013 - Medium - Legacy booking list remains unpaged

- Status: Open.
- Evidence: `backend/src/main/java/com/tibet/tourism/modules/order/web/BookingController.java:133` returns all legacy bookings for `/api/bookings/my`, and `backend/src/main/java/com/tibet/tourism/modules/order/infra/BookingRepository.java:21` exposes `findByUserId(...)` as an unpaged `List`.
- Residual risk: Older booking history can still produce oversized list responses even if the newer order center is fixed.
- Suggested fix: Add `Pageable` / `PageResponse<BookingResponse>` for legacy booking history and move controller business logic into a service.

### CT-BE-014 - Medium - Recommendation history queries are unbounded

- Status: Open.
- Evidence: `backend/src/main/java/com/tibet/tourism/modules/recommendation/application/RecommendationService.java:149` reads a user's full `UserVisitHistory`, `backend/src/main/java/com/tibet/tourism/modules/user/infra/UserVisitHistoryRepository.java:10` through `13` expose only unpaged `List` methods, and `backend/src/main/java/com/tibet/tourism/modules/recommendation/application/strategy/UserBasedCFStrategy.java:34` / `67` load overlap and similar-user histories without query-level limits.
- Residual risk: Recommendation latency and memory can grow with all user history and overlap history, even though some downstream scoring stages limit final results.
- Suggested fix: Query only recent/top-N histories with projections, page or cap overlap/similar-user lookups, and enforce candidate/history limits in repository methods rather than only in stream post-processing.

### CT-FE-009 - Low - Admin/frontend types still rely on broad `any`

- Status: Open.
- Evidence: `frontend/src/views/AdminDashboard.vue:1111` still uses `any[]` for stats payloads, helpers accept `route: any` around `AdminDashboard.vue:1321`, and admin state still uses `ref<any[]>` / `ref<any>` for spots, news, carousels, routes, hotels, and room types around `AdminDashboard.vue:1391`, `1408`, `1884`, `1934`, `2037`, and `2090`. `AdminCommunityPanel.vue` and `AdminHeritagePanel.vue` now have typed list helpers covered by `frontend/src/components/adminA11y.test.ts`.
- Suggested fix: Add typed admin DTOs and `PageResponse<T>` in `frontend/src/api/types.ts` or derive them from generated schema types, then use typed refs and typed `api.get<T>()`/partial update payloads.

### CT-FE-010 - Medium - AI route job SSE paths still bypass the endpoint registry

- Status: Open.
- Evidence: `frontend/src/api/stream.ts:149`, `stream.ts:162`, `stream.ts:183`, and `stream.ts:271` hardcode AI route generate/job stream paths, while `frontend/src/api/endpoints.ts:9` has no builders for route stream, job creation, job detail, or job stream URLs.
- Suggested fix: Add `routes.generateStream`, `routes.generateJob`, `routes.generateJobDetail(jobId)`, and `routes.generateJobStream(jobId)` builders to `frontend/src/api/endpoints.ts`, then consume those builders from `stream.ts`.

### CT-FE-011 - Medium - Major route/order flows still bypass i18n

- Status: Open.
- Evidence: `frontend/src/views/RoutePlanner.vue` still has user-visible Chinese and `zh-CN` formatting outside `t(...)`, including representative lines `77`, `768`, `895`, `1325`, `1337`, `1341`, `1432`, `1709`, and `2291`. `frontend/src/views/OrderCenter.vue` has the same issue for page copy, status labels, tabs, metrics, errors, confirms, date/currency formatting, and item labels around lines `12`, `104`, `126`, `218`, `292`, `418`, `456`, `484`, `502`, `560`, `587`, `598`, and `614`.
- Suggested fix: Move labels/messages/ARIA strings into `zh.json` and `bo.json`, and drive currency/date formatting from the active locale instead of hardcoded `zh-CN`.

### CT-FE-012 - Medium - Non-SSE API calls still bypass the endpoint registry

- Status: Open.
- Evidence: `frontend/src/stores/auth.ts:286` fetches `${normalizedApiBaseURL()}/auth/me` even though `frontend/src/api/modules/auth.ts:5` exposes `auth.me`. `frontend/src/views/ScenicSpotDetail.vue:798` and `ScenicSpotDetail.vue:947` handwrite `/comments/${comment.id}/liked` and `/comments/${comment.id}/like`, while `frontend/src/api/endpoints.ts:156` only has comment list/create/delete/upload builders.
- Suggested fix: Add `comments.liked(id)` and `comments.like(id)` builders, and provide an absolute URL helper for fetch-only flows such as session refresh.

### CT-FE-013 - Medium - Some route/order/heritage controls are still mouse-only or invalidly nested

- Status: Open.
- Evidence: `frontend/src/views/OrderCenter.vue:137` / `142` uses clickable `<article>` cards without `role`, `tabindex`, or keyboard handlers. `frontend/src/views/Heritage.vue:343` contains category `<motion.button>` cards that nest external `<a>` links at `Heritage.vue:424`, and `Heritage.vue:520` uses a clickable `<span>` for Baike links. `frontend/src/views/UserProfile.vue:880` / `881` uses a clickable `<h3>` to open a saved route.
- Suggested fix: Use real `button`, `router-link`, or `a` elements for each action; otherwise add `role`, `tabindex`, and Enter/Space handlers. Avoid nesting links inside buttons.

### CT-FE-014 - Low - Footer fake links and icon-only share controls have weak semantics

- Status: Open.
- Evidence: `frontend/src/components/Footer.vue:33` uses `href="javascript:void(0)"` for WeChat sharing, `Footer.vue:130` uses an empty `href="#"` for About Us, and icon-only share links at `Footer.vue:33`, `38`, and `43` rely on `title` without explicit accessible names.
- Suggested fix: Use a real `<button type="button" aria-label="...">` for WeChat sharing, make About Us a real route/modal button or remove it, and add explicit `aria-label` to icon-only social links.

### CT-OPS-004 - Medium - Deployment scripts make `data` and `logs` world-writable

- Status: Open.
- Evidence: `upload-server.ps1:261` and `deploy-new-server-http.ps1:392` still run `chmod -R a+rwX "$PROJECT_DIR/data" "$PROJECT_DIR/logs"`.
- Suggested fix: `chown` to the container UID/GID or a dedicated group and use narrower permissions such as `750`/`770`.

### CT-OPS-006 - High - Legacy PII key can be empty while v1 ciphertext may remain

- Status: Needs confirmation.
- Evidence: `docker-compose.prod.yml:34` allows empty `PII_ENCRYPTION_KEY`, `backend/src/main/resources/application-prod.yml:160` reads it with an empty default, and `backend/src/main/java/com/tibet/tourism/common/security/PiiCryptoConverter.java:102` can return raw `enc:v1:` data when `legacyV1Key` is absent. If `PII_MIGRATION_ENABLED=true`, `backend/src/main/java/com/tibet/tourism/common/security/PiiBackfillRunner.java:69` / `70` decrypts every non-v2 PII value and immediately re-encrypts it, so an undecrypted `enc:v1:` string can be wrapped as new v2 ciphertext.
- Suggested fix: Confirm whether v1 data exists. If it does, require the legacy key until migration is complete, and fail startup/backfill when v1 rows are present without the legacy key. Add a `PiiBackfillRunner` regression test for v1-without-legacy-key.

### CT-OPS-007 - Low - Nginx may overwrite client forwarding chain

- Status: Needs production topology confirmation.
- Evidence: `docker-compose.prod.yml:46` defaults `TRUST_PROXY_HEADERS=true`, `docker-compose.prod.yml:47` trusts broad private CIDRs, backend trusted-proxy logic reads `X-Forwarded-For`, and `frontend/nginx.conf:96`, `119`, `160`, `220`, and `265` set `X-Forwarded-For` to `$remote_addr`.
- Suggested fix: If there is an upstream CDN or load balancer, configure `real_ip_header`/trusted upstreams, use `$proxy_add_x_forwarded_for`, and narrow `TRUSTED_PROXY_CIDRS` to the actual proxy networks.

### CT-OPS-008 - Low - Supply-chain inputs are not fully pinned

- Status: Needs release policy confirmation.
- Evidence: `backend/Dockerfile:2`, `backend/Dockerfile:9`, `frontend/Dockerfile:2`, and `frontend/Dockerfile:24` use mutable image tags; `scrapler/requirements.txt:1`, `2`, `3`, `6`, and `7` use version ranges; `.github/workflows/ci.yml:19` and other workflow steps use action tags; `frontend/Dockerfile:5` and `.github/workflows/ci.yml:176` / `178` still allow `npm install` when no lock is present.
- Suggested fix: For production releases, pin image digests and GitHub Actions SHAs, add locked or hashed Python dependency inputs, and fail builds when frontend lockfiles are missing.

### CT-OPS-010 - High - Deployment archives can include local uploads and generated artifacts

- Status: Open.
- Evidence: `upload-server.ps1:72` starts tar excludes without excluding `output` or `backend/uploads`, and `upload-server.ps1:92` top-level excludes also omit `output`; the current workspace contains `backend/uploads/admin`, `backend/uploads/avatars`, and `output/lzh-totp-qr.png`. `deploy-new-server-http.ps1:594` excludes `output` but still does not exclude `data` or `backend/uploads`, and `deploy-new-server-http.ps1:619` omits `data` from top-level excludes.
- Suggested fix: Build deployment archives from `git archive` / `git ls-files` allowlists, or explicitly exclude `output`, `data`, `backend/uploads`, ignored files, and local generated artifacts, with a pre-upload archive contents check.

### CT-OPS-011 - Medium - Demo deployment bootstraps Docker from a mutable remote installer

- Status: Open.
- Evidence: `deploy-new-server-http.ps1:302` downloads `https://get.docker.com` to `/tmp/get-docker.sh`, and `deploy-new-server-http.ps1:303` executes it without pinning a version, checksum, or package repository fingerprint.
- Suggested fix: Require Docker to be preinstalled, or install from pinned package repositories and verified GPG fingerprints. If a bootstrap script remains, pin and verify its SHA256 before execution.

### CT-OPS-012 - Low - CI image scanning does not build the same frontend image shape as production Compose

- Status: Open.
- Evidence: `.github/workflows/ci.yml:217`, `220`, and `223` build images with plain `docker build`, while `docker-compose.prod.yml:229` through `234` pass production frontend build args for API base, reCAPTCHA, and AMap. `frontend/Dockerfile:9` through `20` bakes those args into the built frontend assets.
- Suggested fix: Build and scan the production Compose image configuration in CI, or pass deterministic dummy production args and deploy the same scanned image artifact.
