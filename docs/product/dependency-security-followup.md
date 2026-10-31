# 依赖安全与兼容性验收

## 决策原则

- 保持现有公开 API、序列化字段/日期、身份权限及数据迁移行为；不通过关闭测试、调低扫描阈值或忽略漏洞获取绿色状态。
- 使用官方发布源及锁文件完整性校验。开发测试夹具不替代生产密钥，依赖升级不授权部署或真实交易。
- 扫描器的包版本发现与应用实际可利用性是两个问题；有条件的漏洞不能直接断言已可利用，也不能据此不经审查忽略扫描结果。

## Spring 版本边界

官方公告说明，CVE-2026-47884、CVE-2026-47890、CVE-2026-47892 的 Spring Framework 6.2 修复版 6.2.20 为企业支持发行，公开修复发行是 7.0.9。仅升级公开 Boot 3.5 补丁不能消除这些版本发现。因此采用公开且对齐的 Boot 4 / Framework 7 版本线，保留 Java 17，不引入商业仓库或收费依赖。

兼容验收重点：

1. 使用官方 Jackson 2 兼容模块维持现有 JSON 契约，不在安全补丁中静默改为不同字段/日期格式。
2. MVC/WebFlux、认证、CSRF、错误响应与 OpenAPI 契约必须继续通过回归。
3. Hibernate、Flyway 与真实 MySQL 的上下文、既有迁移、库存事务必须实测；单元 mock 不证明数据库语义。
4. 版本升级必须通过完整后端测试与现有安全扫描，未通过前保留草稿状态。

参考：
- https://spring.io/security/cve-2026-47884/
- https://spring.io/security/cve-2026-47890/
- https://spring.io/security/cve-2026-47892/
- https://docs.spring.io/spring-boot/4.1/system-requirements.html
- https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide

## Python 固定版本修复

AnyIO 4.14.2、Protego 0.6.2、Starlette 1.3.1、Certifi 2026.10.18 修复扫描中对应的请求处理/解析问题。生产与开发 requirements 同步固定版本和 PyPI 发布 SHA-256；安装保留 `--require-hashes`。与既有 FastAPI 依赖约束兼容，安装一致性检查与爬虫 16 项回归通过。

官方发布信息：
- https://pypi.org/project/anyio/4.14.2/
- https://pypi.org/project/protego/0.6.2/
- https://pypi.org/project/starlette/1.3.1/
- https://pypi.org/project/certifi/2026.10.18/

## 库存事务补充验证

真实 MySQL CI 曾观察到混合渠道并发场景中容量 2 却有 4 次预订成功；此前单次通过不足以证明并发正确性，不能靠重跑掩盖失败。旧 ORM 关联加载/加锁方案存在当前读、后续查询与持久化上下文缓存交错的风险；尚未捕获旧失败场景的完整 SQL，不能将某一种 ORM 执行策略断言为已证明的唯一原因。现在房型互斥、每晚库存及订单来源、历史预订状态均使用显式原生 `SELECT ... FOR UPDATE`；容量、分配数量/来源和历史状态均采用标量读取，避免复用已加载实体中的旧值。保留 REPEATABLE_READ，不吞掉异常，不放宽容量、过期占位、去重及事务回滚规则。

回归包含 10 次混合渠道并发、既有旧快照场景、已满库存后的 4 种跨渠道旧快照组合、管理员降低库存后旧实体仍在内存的场景，以及历史预订状态变更后的旧实体场景。每个请求线程单独捕获实际 SQL，验证互斥与计数确实使用同一条当前加锁读取，而非由其他线程 SQL 满足断言。全部场景仍须真实 MySQL CI 验证；单元 mock、有限次数并发通过不等同于所有生产负载已验证。

## 前端兼容升级

保持 Vue 3 / Vite 8，升级存在漏洞的公开依赖。Vue I18n 9→11、ECharts 5→6 保留现有组合式国际化与图表模块接口，并验证实际 OpenAPI 引用打包、类型生成、URL glob 匹配和 SVG SSR 输出。前端测试/构建不替代真浏览器视觉验收。

OpenAPI 工具链的临时 Redocly/minimatch 兼容处理及退出条件见 [`frontend/scripts/DEPENDENCY_SECURITY.md`](../../frontend/scripts/DEPENDENCY_SECURITY.md)。该处理使用实际修复版 minimatch，仅适配旧导入形式，具有版本/源文件哈希/接口守卫；不是忽略漏洞或伪造审计。常规安装、明确省略开发依赖的安装及 Docker 安装顺序分别验证。

## Docker 基础镜像与操作系统补丁（2026-10-31）

实际镜像扫描暴露了旧 Temurin Alpine 3.23.4 中的 11 项可修复 HIGH（Expat、p11-kit/p11-kit-trust、SQLite），不是 Maven 依赖问题。保持 Java 17、Python 3.11、Node 22 和现有 Nginx 运行方式，通过官方 Docker Registry v2 重新核验 Dockerfile 基础镜像的多架构摘要；未改动 Compose 中其他服务镜像。`docker-digest-evidence.json` 的逐项 `verifiedAt` 区分本次重验与历史记录。

后端改为更新全部已安装 Alpine 系统包后安装工具；Scrapler 改为更新全部已安装 Debian 系统包，而不是只更新 OpenSSL。前端原有 `apk upgrade` 保留。仍使用发行版原有软件源、摘要固定基础镜像、非 root 用户与 Python 哈希锁定依赖，没有引入测试版软件源或降低扫描阈值。系统包更新随发行版安全仓库变化，最终镜像应以 CI 产物摘要和 SBOM 留档，不能把固定基础摘要等同于最终镜像逐字节可复现。

远程扫描的**基础镜像**结果不等于应用最终镜像结果：
- 新 Temurin 基础镜像仍含 1 项 Brotli CRITICAL；新 Nginx 基础镜像仍含 Brotli/Curl 共 3 项 HIGH/CRITICAL。官方 Alpine 3.24 的 `main/x86_64/APKINDEX.tar.gz` 已提供 Brotli 1.2.1-r0、Curl 8.23.0-r0；同库也提供 Expat 2.9.0-r0、p11-kit 0.26.2-r0、SQLite 3.53.4-r0。最终构建中的全量 `apk upgrade` 必须应用这些补丁，并通过 CI 镜像扫描确认。
- 新 Python 3.11 / Debian 13.7 基础镜像在**不忽略未修复项**的扫描下仍有 44 项 OS HIGH，目前扫描数据库未列出发行版修复版本，另有 2 项基础镜像自带构建工具的 Python 漏洞；既有最终构建会卸载 pip/setuptools/wheel。不能据此声称所有 Python 镜像漏洞已清零。保留原 CI 的 `ignore-unfixed: true` 设置，没有放宽既有规则；未修复项仍需持续跟踪发行版安全公告。

本地无法构建 Docker 镜像。基础镜像远程扫描、官方仓库补丁核验、摘要校验和 Dockerfile 回归检查均不替代下一轮 CI 的三张最终镜像扫描与 SBOM。完整构建结果通过之前，不宣称镜像安全门槛已通过。

补丁核验源：https://dl-cdn.alpinelinux.org/alpine/v3.24/main/x86_64/APKINDEX.tar.gz 。摘要核验使用仓库既有 `scripts/resolve-docker-image-digests.mjs` 的 `resolveDockerHubDigest`，对 `auth.docker.io` / `registry-1.docker.io` 使用正常 TLS 验证。
