# 后端框架维护

## 2026-11-01：Spring Boot 4 迁移

PR #16 合并前的安全扫描发现已提交 Spring 6.2 依赖存在漏洞。Spring 官方将 CVE-2026-47884、CVE-2026-47890、CVE-2026-47892 的 6.2.20 修复列为企业支持，公开修复在 7.0.9。用户已明确授权完成框架迁移、验证后合并；不是通过忽略发现或降低门槛放行。

后端改用 Spring Boot 4.0.9 / Spring Framework 7.0.10，继续使用 Java 17，依赖版本以 `backend/pom.xml` 和 Boot BOM 为准。移除旧 Tomcat、Netty 与 Lombok 的版本覆盖，让它们使用同一 Boot 基线；springdoc 同步迁移到兼容的 3.0.x。

## 兼容边界

- 保留现有 `/api` 路径、DTO、权限规则、CSRF、JWT、TOTP、限流和数据迁移文件。
- 使用 Boot 官方 `spring-boot-jackson2` 过渡模块，继续使用已修补的 Jackson 2.21.7 与现有 ObjectMapper。MVC 和 WebClient 显式选择 Jackson 2，避免本次框架迁移同时改变日期、数值、缓存及外部 API 的 JSON 格式。
- Jackson 2 兼容模块在 Boot 4 中已弃用。后续移除前，需要单独迁移 JSON 序列化并验证接口、缓存和外部服务协议，不能仅删除依赖。
- Flyway、Zipkin 和测试自动配置迁移到 Boot 4 的专用模块；生产 MySQL 方言更新为 `MySQLDialect`，仍由 Flyway 管理表结构。
- Security 7 使用新的认证提供者构造方式；测试改用 `@MockitoBean` 和 Boot 4 测试包，保留原断言。
- Actuator 健康指示器及健康描述类型迁移到新包，管理员安全状态的含义保持一致。

## 合并验证要求

执行后端完整测试、真实 MySQL 的上下文/Flyway/并发预订锁测试、OpenAPI 契约差异检查，以及依赖和容器镜像安全扫描。前端执行 `npm run check` 与现有浏览器回归。不能把 NVD 限流导致未完成的 Dependency Check 记为扫描通过；以 CI 运行记录分别说明验证结果。

本地隔离检出验证：`mvn -q test` 通过，共 1664 项、0 失败、0 错误、5 项环境条件跳过。OpenAPI 成功生成，前端类型仅更新框架内建 `SseEmitter.timeout` 的可空标注，`npm run typecheck` 通过。Trivy 0.70.0 使用 2026-11-01 官方漏洞库完成全严重级别漏洞与 secret 扫描，0 发现、退出码 0。真实 MySQL 及容器镜像结果以 PR #16 的 GitHub Actions 为准，以上本地结果不能代替它们。

## 官方依据

- [Spring Boot 4.0 迁移指南](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Spring Security 7 迁移](https://docs.spring.io/spring-security/reference/migration-7/index.html)
- [CVE-2026-47884](https://spring.io/security/cve-2026-47884/)
- [CVE-2026-47890](https://spring.io/security/cve-2026-47890/)
- [CVE-2026-47892](https://spring.io/security/cve-2026-47892/)
