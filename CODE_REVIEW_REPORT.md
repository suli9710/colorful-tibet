# 项目全面代码审查报告

**审查日期:** 2026-05-21  
**审查范围:** 后端 (Spring Boot 3.2 / Java 17)、前端 (Vue 3 / TypeScript)、Docker 部署配置  
**审查文件数:** 150+ 源文件  

---

## 严重问题 (CRITICAL) — P0 立即修复

### 1. Redis Jackson 反序列化漏洞

**文件:** `backend/src/main/java/com/tibet/tourism/config/RedisConfig.java:27-30`  
**文件:** `backend/src/main/java/com/tibet/tourism/config/CacheConfig.java:116-120`

```java
mapper.activateDefaultTyping(
    LaissezFaireSubTypeValidator.instance,
    ObjectMapper.DefaultTyping.NON_FINAL,
    JsonTypeInfo.As.PROPERTY
);
```

**问题:** 使用 `LaissezFaireSubTypeValidator` 允许反序列化任意 Java 类型。如果攻击者能写入 Redis（如同一网络中的受感染服务），可触发任意类反序列化攻击（gadget chain），导致远程代码执行。

**修复建议:** 改用白名单类型限制反序列化，仅允许预期类型。

---

### 2. 支付回调签名密钥硬编码

**文件:** `backend/src/main/java/com/tibet/tourism/service/OrderCenterService.java:41`

```java
@Value("${app.payments.mock-callback-secret:dev-payment-callback-secret}")
private String callbackSecret;
```

**问题:** 默认值 `dev-payment-callback-secret` 是公开已知的。如果生产环境未通过环境变量覆盖此配置，支付回调签名验证将完全失效，可造成支付欺诈。

**修复建议:** 生产配置中必须通过环境变量设置强密钥，移除默认值或在未配置时启动失败。

---

### 3. BehaviorLog 索引列名不匹配 (Bug)

**文件:** `backend/src/main/java/com/tibet/tourism/entity/BehaviorLog.java:9-10`

```java
@Index(name = "idx_bl_user_id", columnList = "userId")   // ❌ 应为 "user_id"
@Index(name = "idx_bl_created_at", columnList = "createdAt") // ❌ 应为 "created_at"
```

**问题:** `@Index` 的 `columnList` 必须引用数据库列名（snake_case），而非 Java 字段名（camelCase）。Hibernate 默认的 `SpringPhysicalNamingStrategy` 会将列名转换为 snake_case，导致索引创建在错误的列或直接验证失败。

**修复建议:** 将 `columnList` 改为 `"user_id"` 和 `"created_at"`。

---

### 4. 明文存储用户手机号

**文件:** `backend/src/main/java/com/tibet/tourism/entity/PlatformOrder.java:62`  
**文件:** `backend/src/main/java/com/tibet/tourism/entity/HotelBooking.java:37`

`customerPhone` 和 `phone` 字段以**明文**存储，无加密，无 `@JsonIgnore` 注解。

**问题:** 与 `User.java` 中 phone 字段使用 `PiiCryptoConverter` 加密的做法不一致。如果订单数据通过 Jackson 序列化返回给客户端，手机号将以明文泄露。

**修复建议:** 统一使用 `PiiCryptoConverter` 加密，并添加 `@JsonIgnore` 注解。

---

### 5. 未认证文件上传端点

**文件:** `backend/src/main/java/com/tibet/tourism/controller/CommentController.java:146`

```java
@PostMapping("/upload-image")
public ResponseEntity<?> uploadCommentImage(@RequestParam("file") MultipartFile file) {
    // 无任何认证检查
}
```

**问题:** `uploadCommentImage` 端点无任何认证检查，任何未登录用户均可上传文件至服务器。可被用于上传恶意文件或消耗存储空间。

**修复建议:** 添加 `@PreAuthorize("isAuthenticated()")` 注解，并限制文件大小和类型。

---

### 6. 任意认证用户可查看/修改所有酒店订单 (IDOR)

**文件:** `backend/src/main/java/com/tibet/tourism/controller/HotelBookingController.java:113-125`  
**文件:** `backend/src/main/java/com/tibet/tourism/controller/HotelBookingController.java:128-146`

**问题:**
- `getAllBookings` 仅检查 `isAuthenticated()`，但未限制结果仅返回当前用户的订单。任何登录用户可查看系统中**所有**用户的酒店订单。
- `updateStatus` 同样仅检查认证，未检查 ADMIN 角色，任何用户可尝试修改任意订单状态。

**修复建议:** `getAllBookings` 应添加 `@PreAuthorize("hasRole('ADMIN')")`，或过滤仅返回当前用户的订单。`updateStatus` 同样需要角色检查。

---

### 7. 多处硬编码密钥

| 文件 | 行号 | 密钥类型 | 默认值 |
|------|------|----------|--------|
| `docker-compose.yml` | 8, 39 | MySQL root 密码 | `root123456` |
| `backend/src/main/resources/application.yml` | 109 | JWT Secret | `dev-only-jwt-secret-change-me-...` |
| `backend/src/main/resources/application.yml` | 160 | CSRF Signing Secret | `dev-only-csrf-signing-secret-...` |
| `docker-compose.yml` | 51 | Admin Encryption Key | `dev-only-admin-key-change-me-...` |
| `.env` | 1 | 生产 JWT Secret | 真实生产密钥存在于本地磁盘 |
| `docker-compose.yml` | 82-99 | Redis | 无密码保护 |

**风险:** 
- 如果生产环境未覆盖这些默认值，攻击者可利用已知密钥伪造 JWT Token、绕过 CSRF 保护、解密管理员密码、直接访问 MySQL/Redis。
- `.env` 文件虽被 gitignore，但通过备份/压缩包等方式可能泄露真实生产密钥。

**修复建议:** 移除所有硬编码默认值，在缺少必需环境变量时启动失败。将 `.env` 中的生产密钥迁移至服务器安全存储。

---

## 高危问题 (HIGH) — P1 本周内修复

### 8. 缺少 @Transactional 导致并发竞态风险

**文件:** `backend/src/main/java/com/tibet/tourism/service/UserService.java:22-29`  
**文件:** `backend/src/main/java/com/tibet/tourism/service/BookingService.java:15`

```java
// UserService.register - 无 @Transactional
if (userRepository.existsByUsername(user.getUsername())) {  // 检查
    throw new RuntimeException("Username already exists");
}
userRepository.save(user);  // 写入
```

**问题:** `register` 方法先检查用户名是否存在，再保存。无事务保护下，两个并发请求可以同时通过存在检查，导致重复用户名（取决于数据库唯一约束）。

**修复建议:** 添加 `@Transactional` 注解，并依赖数据库唯一约束作为最终防护。

---

### 9. 大量实体缺少 @Version 乐观锁

29 个实体类缺少 `@Version` 字段：

| 实体 | 并发风险 |
|------|----------|
| `User.java` | 个人资料并发更新 |
| `ScenicSpot.java` | 爬虫/管理员并发更新 |
| `Comment.java` | likeCount 并发递增 |
| `SharedRoute.java` | viewCount/likeCount/commentCount 并发递增 |
| `TravelQuestion.java` | viewCount/answerCount/likeCount 并发递增 |
| `TravelAnswer.java` | likeCount 并发递增 |
| `TibetanDictionary.java` | usageCount 并发递增 |
| `Voucher.java` | 代金券核销竞争 |
| `Hotel.java`, `TravelRoute.java`, 等 | 管理员并发编辑 |

**修复建议:** 为有并发更新风险的核心实体添加 `@Version private Long version;` 字段。

---

### 10. 点赞可重复创建 (无数据库唯一约束)

**文件:** `backend/src/main/java/com/tibet/tourism/service/SharedRouteService.java:112-113`

```java
if (!routeLikeRepository.existsByRouteAndUser(route, user)) {
    // 竞态窗口：两次并发 request 同时通过检查
    RouteLike like = new RouteLike();
    // ...
    routeLikeRepository.save(like);
}
```

**问题:** 两次并发的 like 请求可同时通过 `existsByRouteAndUser` 检查，导致重复创建点赞记录。同样模式存在于 `TravelQAService.likeQuestion`。

**修复建议:** 添加数据库唯一约束 `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"route_id", "user_id"}))` 作为最终防护。

---

### 11. N+1 查询

**文件:** `backend/src/main/java/com/tibet/tourism/service/ColdStartOptimizationService.java:208`

```java
for (ScenicSpot spot : popularSpots) {  // 最多 500 个
    int historyCount = historyRepository.findBySpotId(spot.getId()).size();  // N+1
}
```

**问题:** 对每个景点单独查询历史记录，500 个景点产生 501 次数据库查询。

**修复建议:** 使用 `findBySpotIdIn(List<Long> spotIds)` 一次查询所有相关历史记录，再在内存中分组。

---

### 12. 订单号生成冲突风险

**文件:** `backend/src/main/java/com/tibet/tourism/service/OrderCenterService.java:604-606`

```java
return prefix + LocalDateTime.now().format(...)
    + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
```

**问题:** 业务流水号仅使用 UUID 的前 8 位 hex 字符（约 40 亿组合）。UUID 设计上是完整使用，不应截断。高并发下存在非平凡碰撞概率。

**修复建议:** 使用完整 UUID 或使用数据库自增序列/雪花算法生成唯一流水号。

---

### 13. 生产数据库 SSL 默认关闭

**文件:** `docker-compose.prod.yml:44`

```yaml
- DB_SSL_MODE=${DB_SSL_MODE:-DISABLED}
```

**问题:** 虽然 `application-prod.yml` 中默认为 `VERIFY_IDENTITY`，但 docker-compose.prod.yml 的环境变量覆盖为 `DISABLED`，导致生产数据库连接不加密。

**修复建议:** 将 docker-compose.prod.yml 默认值改为 `VERIFY_IDENTITY`。

---

### 14. nginx 暴露版本号

**文件:** `frontend/nginx.conf`

**问题:** 缺少 `server_tokens off;` 指令。错误页面和响应头中会暴露 nginx 版本号，帮助攻击者定位版本特定的漏洞。

**修复建议:** 在每个 `server` 块中添加 `server_tokens off;`。

---

### 15. 生产环境 CSP 包含 `'unsafe-eval'`

**文件:** `frontend/nginx.conf:56`

```
script-src 'self' 'unsafe-eval' ...
```

**问题:** vue-i18n 需要 `unsafe-eval`，但这显著削弱了 XSS 防护能力，使得某些 XSS 注入可以执行任意代码。

**修复建议:** 在构建时预编译本地化消息文件，移除 `unsafe-eval` 指令。

---

### 16. CSRF 已禁用 + CORS 允许 credentials

**文件:** `backend/src/main/java/com/tibet/tourism/security/WebSecurityConfig.java:84`

```java
.csrf(csrf -> csrf.disable())
```

**问题:** Spring Security 的内置 CSRF 保护完全禁用。同时 CORS 配置开启了 `allowCredentials(true)`。如果自定义的 `CsrfCookieFilter` 有缺陷，状态变更的 API 端点将面临 CSRF 攻击风险。

**修复建议:** 确保自定义 CSRF 过滤器覆盖所有状态变更端点，或在 CORS 配置中使用严格的 origin 白名单。

---

## 中危问题 (MEDIUM) — P2 本迭代修复

### 17. 前端 localStorage 存储个人身份信息 (PII)

**文件:** `frontend/src/views/HotelBooking.vue:411`

```typescript
localStorage.setItem('hotel-orders', JSON.stringify([order, ...existing]))
```

**问题:** `order` 对象包含 `guestName`、`phone`、`checkInDate`、`checkOutDate`，以明文存入 localStorage。任何 XSS 漏洞（包括来自第三方 CDN 脚本）都可读取此数据。

**修复建议:** 移除 PII 字段，仅存储订单 ID、状态等非敏感元数据。或完全依赖服务端 API 查询历史订单。

---

### 18. 24+ 个仓库方法无分页限制

以下方法可加载整张表数据，数据量增长后可能导致 OOM：

| 仓库 | 方法 |
|------|------|
| `TravelQuestionRepository` | `findAllByOrderByCreatedAtDesc()` |
| `TravelAnswerRepository` | `findAllByOrderByCreatedAtDesc()` |
| `SharedRouteRepository` | `findAllByOrderByCreatedAtDesc()` |
| `CommentRepository` | `findAllByOrderByCreatedAtDesc()` / `findBySpotIdOrderByCreatedAtDesc()` |
| `HotelBookingRepository` | `findAllByOrderByCreatedAtDesc()` |
| `UserVisitHistoryRepository` | `findByUserId()` / `findBySpotId()` / `findBySpotIdIn()` / `findByUserIdIn()` |
| `ScenicSpotRepository` | `findAll()` / `findByCategory()` / `findByNameContaining()` |
| `BookingRepository` | `findByUserId()` / `findByStatusAndCreatedAtAfter...()` |
| ... 等其他方法 |

**修复建议:** 添加 `Pageable` 参数或通过 `@Query` 添加 `LIMIT` 子句。

---

### 19. 43 个 JPA 实体缺少 `equals()/hashCode()`

**文件:** 所有 `backend/src/main/java/com/tibet/tourism/entity/*.java`

**问题:** 没有一个实体类实现 `equals()` 和 `hashCode()`。JPA 标准要求实体类实现这两个方法，用于：
- Set 集合中正确去重
- 跨持久化上下文实体比较
- HashMap/HashSet 在脱管状态下的正确行为

**修复建议:** 基于实体 ID（数据库主键）实现 `equals()` 和 `hashCode()`。

---

### 20. 控制器缺少输入验证

**文件:** `backend/src/main/java/com/tibet/tourism/controller/CommentController.java:58-63`

```java
@PostMapping("/add")
public ResponseEntity<?> addComment(@RequestBody Map<String, Object> payload) {
    Long spotId = Long.parseLong(payload.get("spotId").toString());  // NPE if null
    Integer rating = Integer.valueOf(payload.get("rating").toString()); // NPE if null
}
```

以下控制器同样使用 `Map<String, Object>` 无 `@Valid`：

| 控制器 | 方法 | 风险 |
|--------|------|------|
| `TravelQAController` | `askQuestion`, `answerQuestion` | 内容和标题无长度限制，无输入消毒 |
| `SharedRouteController` | `shareRoute`, `addComment` | 不安全类型转换，ClassCastException |
| `BookingController` | `createBooking` | visitDate 为 null 导致 500 错误（应为 400） |
| `HotelBookingController` | `updateStatus` | status 字符串无业务验证 |
| `AdminScenicSpotController` | `updateSpot` | 多个字段无 null 检查 |

**修复建议:** 使用 DTO + `@Valid` 注解替代 `Map<String, Object>`，在边界处进行输入验证。

---

### 21. 信息泄露：404 代替 403 导致订单 ID 可枚举

**文件:** `backend/src/main/java/com/tibet/tourism/controller/BookingController.java:140-142`

```java
if (!booking.getUser().getId().equals(user.getId())) {
    return ResponseEntity.status(404).body(Map.of("message", "Booking not found"));
}
```

**问题:** 当用户取消不属于自己的预定时返回 404 而非 403。攻击者可通过 404 vs 200 的响应差异枚举有效的订单 ID（自己的订单返回成功，别人的返回 404，但订单实际存在）。

**修复建议:** 返回 403 状态码，消息改为 "You do not have permission to access this booking"。

---

### 22. 前端 TypeScript `any` 类型泛滥

28 个文件中存在 123+ 处 `any` 类型使用：

| 文件 | `any` 出现次数 |
|------|----------------|
| `AdminDashboard.vue` | 34 |
| `UserProfile.vue` | 13 |
| `RouteCommunity.vue` | 多处 |
| `ScenicSpotDetail.vue` | 多处 |
| 其他 24 个文件 | 多处 |

**问题:** 几乎全部 API 响应和 reactive 状态都用 `any`，完全绕过 TypeScript 类型安全。难以在编译期发现字段名拼写错误、类型不匹配等问题。

**修复建议:** 为所有 API 响应定义 TypeScript 接口，在 `frontend/src/types/` 下统一管理。

---

### 23. 前端 memory leak

**文件:** `frontend/src/views/RoutePlanner.vue:978-980`

```typescript
window.addEventListener('auth-expired', () => {
    if (window.location.pathname !== '/login') alert(t('routePlanner.authFailed'))
})
// 从未调用 removeEventListener
```

**问题:** 事件监听器在模块作用域注册，从未被移除。每次访问 RoutePlanner（配合 `<keep-alive>`）会重复添加，随时间累积。

**文件:** `frontend/src/views/Login.vue:136-166`

```typescript
countdownTimer = setInterval(() => { ... }, 1000)
// 组件卸载时未调用 clearInterval
```

**问题:** 如果用户在倒计时期间离开登录页，interval 继续执行，尝试更新已卸载组件的状态。

**修复建议:** 在 `onUnmounted` 生命周期钩子中清理定时器和事件监听器。

---

### 24. 静默错误吞没

多处 `catch` 块完全吞没异常，无日志记录：

**文件:** `backend/src/main/java/com/tibet/tourism/controller/ScenicSpotController.java:206-211`  
**文件:** `backend/src/main/java/com/tibet/tourism/controller/PriceController.java:58-59`  
**文件:** `backend/src/main/java/com/tibet/tourism/controller/AiRouteController.java:70`  

```java
catch (Exception e) {
    return ResponseEntity.ok(Map.of("success", false, "message", "计算失败，请稍后重试"));
    // 异常 e 未被记录，排查问题几乎不可能
}
```

**修复建议:** 在 catch 块中添加 `log.error("...", e)` 记录完整堆栈跟踪。

---

### 25. 虚假分析数据

**文件:** `backend/src/main/java/com/tibet/tourism/service/AdminStatsService.java:147-161`

```java
private Map<String, Long> buildVisitorCityDistribution() {
    // 使用 java.util.Random 生成完全随机的假数据
}
```

**问题:** `buildVisitorCityDistribution()` 方法使用 `java.util.Random` 生成完全虚假的游客城市分布数据，呈现给管理员看板，但标注为真实分析。

**修复建议:** 如果数据源暂不可用，应明确标注为"演示数据"或直接返回空结果。

---

## 低危问题 / 建议 (LOW) — P3 积压

### 26. `window.alert` 全局覆盖

**文件:** `frontend/src/composables/useToast.ts:28-37`

```typescript
window.alert = (message?: any) => {
    showToast(String(message ?? ''), 'info')
}
```

**问题:** 覆盖原生 `window.alert` 可能破坏第三方库或浏览器扩展。任何 XSS 负载调用 `alert()` 将静默显示 toast。

**修复建议:** 调用原始 `alert` 作为降级方案，或移除此覆盖。

---

### 27. 敏感数据日志泄露

| 文件 | 行号 | 问题 |
|------|------|------|
| `AiRouteService.java` | 85-88 | 记录完整的 AI API 主机名和路径 |
| `PriceFetchService.java` | 398-399 | Debug 级别记录完整的 AI 价格响应 |
| `AiQuotaService.java` | 52 | Redis 异常消息可能包含连接信息 |

**修复建议:** 在日志中脱敏 URL、API 响应和异常消息。

---

### 28. 海拔风险评估阈值不一致

| 服务 | 高风险阈值 | 中风险阈值 |
|------|-----------|-----------|
| `ItineraryService.java:546` | 4700m | 3900m |
| `TibetTravelKitService.java:728` | 5000m | 3800m |

**修复建议:** 统一阈值定义至一个常量或配置类。

---

### 29. @ManyToOne 默认 EAGER 加载

**文件:** `backend/src/main/java/com/tibet/tourism/entity/SpotTag.java:13`  
**文件:** `backend/src/main/java/com/tibet/tourism/entity/UserVisitHistory.java:17,22`

**问题:** `@ManyToOne` 未指定 `fetch = FetchType.LAZY`，JPA 默认为 EAGER。每次加载 SpotTag 都会同时加载完整的 ScenicSpot，每次加载 UserVisitHistory 都会同时加载 User 和 ScenicSpot。

**修复建议:** 添加 `fetch = FetchType.LAZY`。

---

### 30. DashboardStatsDTO 嵌入 JPA 实体

**文件:** `backend/src/main/java/com/tibet/tourism/dto/DashboardStatsDTO.java:12-13`

```java
List<Booking> recentBookings;
List<ScenicSpot> popularSpots;
```

**问题:** DTO 直接持有 JPA 实体引用，可能导致：
- 事务外访问触发 `LazyInitializationException`
- Jackson 序列化时循环引用
- 意外泄露不应暴露的实体字段

**修复建议:** 使用专门的响应 DTO 替代原始实体。

---

### 31. 部署相关低危问题

- **Docker 镜像未锁定 SHA256 摘要** — 标签更新可能引入恶意镜像
- **生产 compose 中 alertmanager 缺少 healthcheck** — 可能在其他服务就绪前启动
- `.dockerignore` 缺少 `*.pem`、`*.key`、`*.crt` 排除 — 证书文件可能泄漏到构建上下文
- **Maven 构建以 root 运行** — 受感染的 Maven 插件具备 root 权限
- **`upload-server.ps1` 包含真实服务器 IP 和域名** — 暴露基础设施信息

---

## 正面发现 (值得保持)

以下是项目中做得好的部分：

| 方面 | 实践 |
|------|------|
| **认证** | httpOnly Cookie JWT，不存储在 localStorage |
| **XSS 防护** | 所有 4 处 `v-html` 均通过 DOMPurify 消毒 |
| **CSRF** | axios 配置 `xsrfCookieName`/`xsrfHeaderName`，stream 请求手动附加 token |
| **设备指纹** | FingerprintJS 添加 `X-Device-Fingerprint` header |
| **人机验证** | 预订/支付操作集成 reCAPTCHA |
| **错误监控** | 前端 `errorMonitoring.ts` 提供结构化错误上报 |
| **懒加载** | 所有路由使用 `() => import(...)` 动态导入 |
| **会话管理** | 401 响应触发全局 `auth-expired` CustomEvent |
| **行为追踪** | `useBehaviorTracker.ts` 收集行为数据辅助反欺诈 |

---

## 问题统计汇总

| 严重程度 | 数量 | 类别分布 |
|----------|------|----------|
| 严重 (CRITICAL) | 7 | 反序列化漏洞、密钥泄露、Bug、PII 泄露、IDOR |
| 高危 (HIGH) | 9 | 事务、乐观锁、N+1、冲突风险、SSL/CSP 配置 |
| 中危 (MEDIUM) | 9 | 输入验证、分页、类型安全、Memory leak、错误处理 |
| 低危 (LOW) | 6+ | 日志、阈值不一致、EAGER 加载、DTO 设计 |

---

## 修复优先级时间线

| 优先級 | 时间 | 问题编号 |
|--------|------|----------|
| **P0** | 立即 | #1 Jackson 反序列化, #2 支付密钥, #3 索引列名, #4 明文 PII, #5 未认证上传, #6 IDOR, #7 硬编码密钥 |
| **P1** | 本周 | #8 @Transactional, #9 @Version, #10 唯一约束, #11 N+1, #12 订单号, #13 DB SSL, #14 nginx 版本, #15 unsafe-eval, #16 CSRF |
| **P2** | 本迭代 | #17 localStorage PII, #18 仓库分页, #19 equals/hashCode, #20 输入验证, #21 信息泄露, #22 any 类型, #23 memory leak, #24 静默错误, #25 虚假数据 |
| **P3** | 积压 | #26-#31 及低危建议项 |
