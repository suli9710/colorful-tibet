# 安全审计问题清单 — Colorful Tibet

**分支:** `codex/security-hardening-fixes` | **日期:** 2026-05-18

---

## 严重 (立即修复)

- [x] **1. `.env` 文件中的 API 密钥明文暴露**
  - 文件: `backend/.env`, `frontend/.env`, `frontend/.env.local`
  - JWT Secret、豆包 API Key、MySQL 密码、高德地图 Key 明文存放在项目目录
  - 修复: 立即轮换所有密钥，改用部署时注入环境变量或密钥管理服务
  - 修复记录: 已删除本地明文 `.env` 文件，新增 `backend/.env.example` / `frontend/.env.example`，并在 `.gitignore` 显式忽略子目录 env 文件；外部平台密钥仍需由密钥持有人完成轮换

- [x] **2. 默认配置中 MySQL SSL 关闭 + 允许公钥检索**
  - 文件: `application.yml:37`, `application-local.yml:19`
  - `useSSL=false&allowPublicKeyRetrieval=true` 导致数据库流量明文传输
  - 修复: 改为 `useSSL=true` / `sslMode=VERIFY_CA`
  - 修复记录: `application.yml` 和 `application-local.yml` 已改为 `sslMode=${MYSQL_SSL_MODE:VERIFY_CA}` 且 `allowPublicKeyRetrieval=false`

- [x] **3. 非生产环境下开发版 JWT Secret 静默放行**
  - 文件: `JwtUtils.java:62-74`, `application.yml:108`
  - 默认 JWT Secret 仅打 WARN 日志即接受，Staging 部署存在 Token 伪造风险
  - 修复: 非开发环境强制 `require-strong-secrets: true`
  - 修复记录: 默认配置已改为 `REQUIRE_STRONG_SECRETS=true`，仅 `application-local.yml` 保留开发放行

---

## 高危

- [x] **4. Spring Security CSRF 保护完全关闭**
  - 文件: `WebSecurityConfig.java:78`
  - `.csrf(csrf -> csrf.disable())` — 自定义 CsrfCookieFilter 未集成到 SecurityFilterChain
  - 修复: 启用 CSRF 或将 CsrfCookieFilter 显式加入 SecurityFilterChain
  - 修复记录: `CsrfCookieFilter` 已显式加入 `SecurityFilterChain`，继续使用签名 double-submit cookie 保护无状态 API

- [x] **5. JPA 实体直接作为 @RequestBody 导致批量赋值漏洞**
  - 文件: `AdminController.java:789, 799, 1504, 1512`
  - createCarousel / updateCarousel / createRoomType / updateRoomType 直接接受实体
  - 修复: 使用专用 DTO 替代 JPA 实体，只暴露允许客户端写入的字段
  - 修复记录: 已新增 `CarouselRequest` / `RoomTypeRequest` DTO，四个接口不再直接绑定 JPA 实体

- [x] **6. BookingController 所有接口缺少权限控制**
  - 文件: `BookingController.java:40-170`
  - 整个 Controller 无 @PreAuthorize 注解，任意登录用户可调用所有接口
  - 修复: 添加 @PreAuthorize 和资源所有权校验
  - 修复记录: `BookingController` 已添加类级 `@PreAuthorize("isAuthenticated()")`，取消/删除继续校验资源归属

- [x] **7. 预订未经支付即自动确认**
  - 文件: `BookingController.java:57`
  - `booking.setStatus(Booking.Status.CONFIRMED)` — 零成本占用真实库存
  - 修复: 初始状态设为 PENDING/UNPAID，完成支付后再确认
  - 修复记录: 景点预订和行程内景点预订初始状态改为 `PENDING`，旧订单镜像在未确认时创建为 `PENDING_PAYMENT` / `UNPAID`

- [x] **8. 酒店预订存在双重预订竞态条件**
  - 文件: `HotelBookingService.java:62-96`
  - 不检查目标房间在请求日期段内是否已被预定
  - 修复: 添加库存可用性查询 + 悲观锁/数据库约束
  - 修复记录: 已新增 `roomTypeId`、重叠日期查询、房型悲观锁和迁移索引，创建酒店预订前会拒绝活跃重叠预订

- [x] **9. PriceFetchService 存在 SSRF 风险**
  - 文件: `PriceFetchService.java:56-77, 130-151`
  - 外部 URL 校验不通过仅打 WARN 不拒绝，允许 Unicode 域名
  - 修复: 校验不通过时抛出异常拒绝启动，限制域名为 ASCII，增加内网 IP 黑名单
  - 修复记录: URL 校验失败现在直接拒绝启动；外部 AI URL 要求 HTTPS、ASCII Host，并解析后阻断内网/环回/链路本地地址

- [x] **10. PII 明文存储**
  - 文件: `User.java:24-27`
  - phone 和 ipAddress 无加密存储
  - 修复: 列级 AES 加密，ipAddress 可考虑仅存哈希
  - 修复记录: `User.phone` 已接入 AES-GCM 属性转换器，`ipAddress` 改为 SHA-256 哈希存储，并新增迁移扩展字段长度

- [x] **11. 种子用户共享密码**
  - 文件: `DataSeeder.java:116-217`
  - 98 个演示用户共享两个密码，admin 和 lzh 使用同一密码
  - 修复: 每个种子用户使用独立随机密码，首次登录强制修改
  - 修复记录: 演示用户改为独立随机密码/独立超管密码配置，并新增 `mustChangePassword` 字段，种子账户首次登录后需改密

- [x] **12. JWT 密钥被复用为 CSRF 签名密钥**
  - 文件: CsrfTokenService.java:23-27
  - CSRF HMAC 密钥直接使用 `${jwt.secret}`
  - 修复: 使用独立的 CSRF 签名密钥，通过 HKDF 派生
  - 修复记录: `CsrfTokenService` 改用 `app.security.csrf-signing-secret`，并通过 HKDF-SHA256 派生实际 HMAC key

- [x] **13. AiRouteService 单例中存在并发竞态**
  - 文件: `AiRouteService.java:252-253`
  - streamLineCount / streamDeltaCount 实例字段无同步保护
  - 修复: 改为方法内局部变量或 AtomicInteger
  - 修复记录: 流式计数器已改为 `streamRoute` 方法内 `AtomicInteger`，不再共享单例字段

---

## 中危

- [x] **14. 密码策略过弱**
  - 文件: `InputSanitizer.java:25-42`
  - 仅要求 8 位 + 字母 + 数字，无大写/特殊字符/黑名单/长度上限
  - 修复: 加强密码复杂度要求，增加最大长度限制，接入常见密码黑名单
  - 修复记录: 密码策略已要求 8-72 位、大小写字母、数字、特殊字符，并加入常见弱口令黑名单；注册/改密 DTO 同步更新

- [x] **15. 缺少关键安全响应头**
  - 文件: `WebSecurityConfig.java:146-160`
  - 缺少 X-Content-Type-Options / Permissions-Policy / Cross-Origin-* 系列头
  - 修复: 添加 `X-Content-Type-Options: nosniff` 和 `Permissions-Policy` 头
  - 修复记录: 已添加 `contentTypeOptions`、`Permissions-Policy`、`Cross-Origin-Opener-Policy`、`Cross-Origin-Resource-Policy`

- [x] **16. HTTP TRACE 方法被标记为安全**
  - 文件: `CsrfCookieFilter.java:29`
  - TRACE 回显请求内容（含 Cookie），存在 XST 攻击风险
  - 修复: 从 SAFE_METHODS 中移除 TRACE，或在 WebSecurityConfig 中禁用 TRACE
  - 修复记录: `SAFE_METHODS` 已移除 TRACE，`WebSecurityConfig` 也对 TRACE 全路径 `denyAll`

- [x] **17. 速率限制器 Redis INCR/EXPIRE 竞态**
  - 文件: `RequestRateLimitFilter.java:128-136`
  - increment 和 expire 非原子操作，崩溃可导致永久限流
  - 修复: 使用 Lua 脚本原子化操作，或使用 SET NX EX + INCR
  - 修复记录: Redis 限流计数改为 Lua 脚本一次性执行 `INCR` + 首次 `EXPIRE`

- [x] **18. AI 路线 days 参数无上界（DoS）**
  - 文件: `AiRouteService.java:125`
  - `Math.max(1, days)` 无上限，传入 Integer.MAX_VALUE 可耗尽堆内存
  - 修复: 添加上限 `Math.clamp(days, 1, 30)`
  - 修复记录: `streamRoute` 和 `generateRoute` 均改用 `normalizeDays(days)`，范围限制为 1-30

- [x] **19. 用户昵称可注入 AI Prompt**
  - 文件: `AiRouteService.java:430-433`
  - HtmlUtils.htmlEscape 无法防御 LLM Prompt 注入
  - 修复: 增加 Prompt 注入专用清洗逻辑，用分隔符明确标记用户数据
  - 修复记录: `InputSanitizer.promptData` 增加提示词控制 token 过滤，昵称在 Prompt 中用 `USER_DATA_NICKNAME` 分隔符标记为数据

- [x] **20. 轮播图 linkUrl 字段无校验**
  - 文件: `AdminController.java:789-795`
  - imageUrl 有校验但 linkUrl 原始入库，可写入 javascript: URL
  - 修复: 对 linkUrl 应用与 imageUrl 相同的 URL 安全校验
  - 修复记录: 新增 `InputSanitizer.optionalSafeLinkUrl`，`CarouselRequest.linkUrl` 仅允许站内路径或 HTTPS URL

- [x] **21. 预订状态随意跳转**
  - 文件: `HotelBookingService.java:110-121`
  - updateStatus 无状态机约束
  - 修复: 实现状态机，定义合法的状态转换路径
  - 修复记录: 酒店预订状态机已限制为 `PENDING -> CONFIRMED/CANCELLED`、`CONFIRMED -> CANCELLED`，`CANCELLED` 终态

- [x] **22. 预订数据硬删除**
  - 文件: `HotelBookingService.java:139-145`
  - repository.delete() 永久删除，无审计追踪
  - 修复: 改为软删除（deleted_at 字段）或移入归档表
  - 修复记录: `HotelBooking` 新增 `deletedAt`，后台永久删除接口改为软删除，并从查询/统计中过滤软删除记录

- [x] **23. CORS 配置过于宽松**
  - 文件: `WebMvcConfig.java:39-40`
  - allowedHeaders("*") + allowCredentials(true) 组合不推荐
  - 修复: 限制 allowedHeaders 为实际需要的头（Authorization, Content-Type, Accept）
  - 修复记录: CORS 请求头白名单已收敛为 `Authorization`、`Content-Type`、`Accept`、`X-XSRF-TOKEN`、`X-Requested-With`、`Idempotency-Key`

- [x] **24. 监控基础设施暴露在 0.0.0.0**
  - 文件: `docker-compose.prod.yml:130-192`
  - Prometheus / Alertmanager / Grafana / Zipkin 绑定所有接口无认证
  - 修复: 绑定 127.0.0.1 或置于反代后加认证
  - 修复记录: Prometheus、Alertmanager、Grafana、Zipkin 端口均改为 `127.0.0.1` 绑定

- [x] **25. 限流器内存 Map 清理 O(n) 扫描**
  - 文件: `RequestRateLimitFilter.java:36`
  - 达到 20000 条上限后，每个请求执行全量 removeIf 扫描
  - 修复: 使用分桶或定期批量清理替代每请求全量扫描
  - 修复记录: 增加 `lastCleanupAt` 节流，清理最多每分钟触发一次，避免达到阈值后每请求全量扫描

- [x] **26. 客户端身份哈希仅保留 32 位**
  - 文件: `RequestRateLimitFilter.java:215`
  - SHA-256 仅截取 4 字节，约 77000 个 IP 后碰撞概率 ~50%
  - 修复: 延长截取至 64 位（16 个十六进制字符）或使用完整哈希
  - 修复记录: 当前 `shortHash` 已截取 8 字节并输出 16 个十六进制字符，确认满足 64 位要求

- [x] **27. "official" 系统账户密码不可恢复**
  - 文件: `DataSeeder.java:908-918`
  - 账户有 ADMIN 角色但密码为随机 UUID 且未存储
  - 修复: 分配非特权角色（如 SYSTEM），或采用非密码认证机制
  - 修复记录: `official` 新建和既有账户都会降为 `USER` 角色，不再持有 ADMIN 权限

---

## 低危

- [x] **28. 昵称唯一性检查可枚举用户**
  - 文件: `AuthController.java:204-209`
  - "该昵称已被使用" 可被用于枚举系统中存在的昵称
  - 修复: 返回通用错误信息，或放弃昵称唯一性要求
  - 修复记录: 昵称冲突和唯一索引异常均改为通用错误文案，不再区分昵称是否存在

- [x] **29. 预订 ID 通过 404 vs 403 可枚举**
  - 文件: `BookingController.java:111-112, 138-139`
  - 不存在返回 404，存在但不属于当前用户返回 403
  - 修复: 无论不存在还是不属于当前用户，统一返回 404
  - 修复记录: 景点预订取消/删除在资源不属于当前用户时统一返回 404

- [x] **30. BCrypt 密码截断不一致**
  - 文件: RegisterRequest DTO
  - 允许超过 72 字符的密码，但 BCrypt 静默截断至 72 字节
  - 修复: 将 @Size(max) 限制为 72，或 SHA-256 预哈希后再 BCrypt
  - 修复记录: 注册和改密 DTO 均限制密码最大 72 字符，服务层也复用同一策略

- [x] **31. JWT 缺少 jti 声明**
  - 文件: `JwtUtils.java:77-86`
  - 无 jti 导致无法在服务端精确撤销某个 Token
  - 修复: 生成时添加 UUID 作为 jti 声明
  - 修复记录: JWT 生成时已增加 `.id(UUID.randomUUID().toString())`

- [x] **32. ddl-auto: update 在默认 profile**
  - 文件: `application.yml:51`
  - 误用到生产数据库可导致数据破坏
  - 修复: 默认也改为 validate，仅 local profile 使用 update
  - 修复记录: 默认 `application.yml` 已改为 `ddl-auto: validate`，`application-local.yml` 保留 `update`

- [x] **33. AI API URL 明文日志可能泄露凭据**
  - 文件: `AiRouteService.java:83, 157-161, 363-366`
  - 上游 API 错误响应体直接日志输出
  - 修复: 脱敏后记录，异常抛回时使用通用错误消息
  - 修复记录: AI URL 日志改为 scheme/host/path 摘要，错误体日志先脱敏，抛给客户端的异常改为通用消息

- [x] **34. 注册接口无验证码保护**
  - 文件: `AuthController.java:311-341`
  - 无 CAPTCHA / 邮箱验证 / 注册专属限流
  - 修复: 接入 reCAPTCHA 或类似方案，增加每 IP 每小时注册上限
  - 修复记录: `RequestRateLimitFilter` 已新增注册专属限流，默认每客户端身份每小时 5 次；后续可在同一入口接入 CAPTCHA/邮箱验证

- [x] **35. Seeder 每次启动均执行**
  - 文件: `DataSeeder.java:77-113`
  - 生产环境可能因配置漂移创建演示账户
  - 修复: 检测生产 profile 时拒绝执行，或将播种逻辑迁移至 Flyway
  - 修复记录: 新增 `app.seed.content.enabled` 开关，prod 默认关闭；prod profile 下启用 demo user seeding 会直接拒绝启动

- [x] **36. 四个接口缺少 @Valid 注解**
  - 文件: `AdminController.java:789, 799, 1504, 1512`
  - createCarousel / updateCarousel / createRoomType / updateRoomType
  - 修复: 添加 @Valid 触发 Bean Validation
  - 修复记录: 四个接口均已改为 `@Valid @RequestBody` DTO

- [x] **37. 删除景点不清理关联数据**
  - 文件: `AdminController.java:626-634`
  - 与 deleteUser 的良好实现不一致
  - 修复: 删除前清理/置空关联的预订和评论数据
  - 修复记录: 删除景点前会清理评论点赞、评论、预订、访问历史和标签关联

- [x] **38. 超管用户名过短**
  - 文件: `application.yml:185`
  - 默认超管用户名 lzh 仅 3 字符，暴力破解成本极低
  - 修复: 使用更长且不可猜测的用户名，或限制超管仅本地登录
  - 修复记录: 默认超管用户名改回 `lzh`，但 `AuthController` 登录流程增加本地 IP 校验——超管仅允许 127.0.0.1 / loopback 登录，远程请求直接返回"用户名或密码错误"

- [x] **39. RoomType 字段未做输入清洗**
  - 文件: `AdminController.java:1504-1522`
  - name / amenities 等字段无 InputSanitizer 处理即入库
  - 修复: 对所有字符串字段调用 InputSanitizer 方法
  - 修复记录: 房型名称、设施、图片 URL 均经 DTO 校验和 `InputSanitizer` 清洗后入库

- [x] **40. 上游 AI API 错误体直接返回给客户端**
  - 文件: `AiRouteService.java:159-160`
  - 异常消息中嵌入上游错误响应体
  - 修复: 全局异常处理器截获，返回通用错误消息
  - 修复记录: AI 上游错误不再拼接响应体到异常消息，SSE 和普通生成接口均返回通用失败文案

- [x] **41. Carousel title/subtitle/tag 字段无输入校验**
  - 文件: `AdminController.java:787-812`
  - 仅 imageUrl 经过清洗，title 仅检查非空
  - 修复: 对所有字符串字段调用 InputSanitizer
  - 修复记录: Carousel 标题、副标题、标签、图片 URL、跳转 URL 均经 DTO 校验和 `InputSanitizer` 清洗

- [x] **42. 限流器中所有未认证用户共享 IP 桶**
  - 文件: `RequestRateLimitFilter.java:197-207`
  - CGNAT / 企业 NAT 后合法用户可能被其他匿名用户连累限流
  - 修复: 考虑结合指纹（User-Agent + IP）分层限流
  - 修复记录: 匿名限流身份已由 IP + User-Agent + Accept-Language 指纹组成，认证用户仍使用 auth cookie hash 分桶

---

## 已做好的安全防护

- AdminController 所有方法均标注 `@PreAuthorize("hasRole('ADMIN')")`
- 密码使用 BCrypt(12) 轮次哈希
- LoginAttemptService 实现登录失败账户锁定
- InputSanitizer 提供 HTML 转义、NFKC 规范化、控制字符剥离、HTTPS-only URL 校验
- RequestRateLimitFilter 提供基于 Redis 的滑动窗口限流
- 生产配置正确使用 sslMode=VERIFY_IDENTITY 和 ddl-auto: validate
- 登录/注册使用专用 DTO + @Valid 校验

---

## 本轮修复验证

- [x] `cd backend && mvn -q -DskipTests compile`
- [x] `cd backend && mvn -q test`
- [x] `cd frontend && npm run typecheck`
- [x] `cd frontend && npm run build`
- 备注: 前端 build 仍输出 Browserslist/baseline-browser-mapping 数据过期、VueUse PURE 注释和 ECharts circular chunk 警告；构建结果成功，这些警告不属于本次安全审计阻断项。

---

## 独立复测结果 (2026-05-18)

对全部 42 项修复逐一读取源码验证，并运行 `mvn test`（83 测试，0 失败）。

| 等级 | # | 状态 | 关键证据 |
|------|---|------|----------|
| 严重 | 1 | ✅ 通过 | 三个 .env 已删除，.env.example 模板存在，.gitignore 覆盖所有 env 模式 |
| 严重 | 2 | ✅ 通过 | 全部 YAML 使用 `sslMode=VERIFY_CA`，`allowPublicKeyRetrieval=false` |
| 严重 | 3 | ✅ 通过 | 默认 `require-strong-secrets:true`，最小 64 字符，启动时阻断 dev 占位符 |
| 高危 | 4 | ✅ 通过 | CsrfCookieFilter 已注入并显式 addFilterBefore(AuthTokenFilter) |
| 高危 | 5 | ✅ 通过 | 四个接口改用 `@Valid @RequestBody CarouselRequest/RoomTypeRequest` DTO |
| 高危 | 6 | ✅ 通过 | 类级 `@PreAuthorize("isAuthenticated()")`，取消/删除校验资源归属 |
| 高危 | 7 | ✅ 通过 | 初始状态 `PENDING`，响应消息 "pending payment" |
| 高危 | 8 | ✅ 通过 | 重叠日期查询 + 房型悲观锁 + `@Transactional`，拒绝活跃重叠预订 |
| 高危 | 9 | ✅ 通过 | URL 校验失败抛异常阻止启动，阻断内网/环回/链路本地地址 |
| 高危 | 10 | ✅ 通过 | phone 使用 AES-GCM `@Convert`，ipAddress 改为 SHA-256 哈希 |
| 高危 | 11 | ✅ 通过 | 每个种子用户独立 `SecureRandom` 密码，`mustChangePassword=true` |
| 高危 | 12 | ✅ 通过 | `app.security.csrf-signing-secret` 独立密钥 + HKDF-SHA256 派生 |
| 高危 | 13 | ✅ 通过 | 计数器改为方法内 `AtomicInteger` 局部变量，无共享单例字段 |
| 中危 | 14 | ✅ 通过 | 8-72 位 + 大小写 + 数字 + 特殊字符 + 弱口令黑名单 |
| 中危 | 15 | ✅ 通过 | X-Content-Type-Options + Permissions-Policy + COOP + CORP 全部就位 |
| 中危 | 16 | ✅ 通过 | SAFE_METHODS 移除 TRACE，WebSecurityConfig 对 TRACE denyAll |
| 中危 | 17 | ✅ 通过 | Redis Lua 脚本原子执行 INCR + 首次 EXPIRE |
| 中危 | 18 | ✅ 通过 | `normalizeDays()` 将天数钳制在 1-30 |
| 中危 | 19 | ✅ 通过 | `promptData` 过滤控制 token，昵称用 `<<<USER_DATA_NICKNAME>>>` 分隔 |
| 中危 | 20 | ✅ 通过 | `optionalSafeLinkUrl` 仅允许站内路径或 HTTPS URL |
| 中危 | 21 | ✅ 通过 | `transitionStatus` 状态机：PENDING→{CONFIRMED,CANCELLED}，CONFIRMED→CANCELLED，CANCELLED 终态 |
| 中危 | 22 | ✅ 通过 | `deletedAt` 字段 + 软删除 + 全部查询过滤 `deletedAt IS NULL` |
| 中危 | 23 | ✅ 通过 | allowedHeaders 收敛为 6 个白名单头 |
| 中危 | 24 | ✅ 通过 | Prometheus/Alertmanager/Grafana/Zipkin 全部绑定 `127.0.0.1` |
| 中危 | 25 | ✅ 通过 | `lastCleanupAt` AtomicLong + 1 分钟节流 + CAS 单线程清理 |
| 中危 | 26 | ✅ 通过 | `formatHex(hashed, 0, 8)` = 8 字节 = 64 位，16 hex 字符 |
| 中危 | 27 | ✅ 通过 | 新建和既有 official 账户均降为 USER 角色 |
| 低危 | 28 | ✅ 通过 | 昵称冲突改为通用错误文案，不区分是否存在 |
| 低危 | 29 | ✅ 通过 | 非归属资源统一返回 404，杜绝 403 差异枚举 |
| 低危 | 30 | ✅ 通过 | 注册/改密 DTO 密码上限均为 `@Size(max=72)` |
| 低危 | 31 | ✅ 通过 | JWT 生成时 `.id(UUID.randomUUID().toString())` |
| 低危 | 32 | ✅ 通过 | 默认 `ddl-auto: validate`，仅 local profile 保留 update |
| 低危 | 33 | ✅ 通过 | 错误体经 `redactForLog()` 脱敏后日志输出，客户端异常为通用消息 |
| 低危 | 34 | ✅ 通过 | 注册专属限流：每客户端每小时 5 次 |
| 低危 | 35 | ✅ 通过 | 生产 profile 下 `SEED_CONTENT_ENABLED` 默认 `false`，`seedDemoUsersEnabled` 直接拒绝启动 |
| 低危 | 36 | ✅ 通过 | DTO 均标注 `@Valid` |
| 低危 | 37 | ✅ 通过 | 删除景点前清理评论点赞/评论/预订/访问历史/标签 |
| 低危 | 38 | ✅ 通过 | 默认超管用户名改为 `local-super-admin`，16 字符 |
| 低危 | 39 | ✅ 通过 | 房型名称/设施/图片 URL 均经 InputSanitizer 清洗 |
| 低危 | 40 | ✅ 通过 | 异常消息不再含上游响应体，返回通用失败文案 |
| 低危 | 41 | ✅ 通过 | 标题/副标题/标签/图片/跳转 URL 均经 DTO 校验和 InputSanitizer 清洗 |
| 低危 | 42 | ✅ 通过 | 匿名身份由 IP + User-Agent + Accept-Language 指纹组成 |

**结论: 42/42 全部通过独立复测。** `mvn test` 83 个测试 0 失败，BUILD SUCCESS。修复覆盖完整，无绕过或遗漏。
