# 部署配置治理说明

本文补充 `docs/DEPLOYMENT.md` 的本地部署步骤，重点说明 local/prod Compose 边界、环境变量来源、metrics 公开性，以及 CI 与分支保护要求。本文只描述治理规则，不改变运行行为。

## Compose 环境边界

| 场景 | Compose 文件 | 启动命令 | 主要用途 | 外部暴露面 |
| --- | --- | --- | --- | --- |
| 本地开发 | `docker-compose.yml` | `docker compose up -d --build` | 本机联调、演示、开发验收 | 前端映射到 `${FRONTEND_HOST_PORT:-80}`；backend、MySQL、Redis、Scrapling 默认绑定到 `127.0.0.1` 的宿主机端口 |
| 生产部署 | `docker-compose.prod.yml` | `docker compose -f docker-compose.prod.yml up -d --build` | HTTPS 前端、生产 profile、监控组件 | 前端发布 `80/443`；backend、Prometheus、Alertmanager、Grafana、Zipkin 默认只绑定 `127.0.0.1`；MySQL 和 Redis 不发布宿主机端口 |

本地 Compose 默认 `SPRING_PROFILES_ACTIVE=local`，允许 HTTP、本地种子内容和较宽松的调试开关。生产 Compose 固定 `SPRING_PROFILES_ACTIVE=prod`，要求真实域名、证书、强密钥、Redis 密码、Scrapling API key、reCAPTCHA 和生产地图配置。

不要用 `docker-compose.yml` 承载公网生产流量；也不要把从 `.env.example` 复制出的本地 `.env` 直接用于生产。生产发布前必须用 `docker compose -f docker-compose.prod.yml config` 检查最终展开结果，确认没有 `change-me`、`replace-with-*` 或本地 profile 值残留。

## 变量来源矩阵

Docker Compose 展开变量时，宿主机环境变量和 `--env-file`/`.env` 会参与插值，随后 `docker-compose*.yml` 中的 `environment`、`build.args` 和默认值把最终值传入容器。应用内部的 `application*.yml` 默认值只在容器没有提供同名变量时生效。

| 来源 | 作用范围 | 示例 | 治理要求 |
| --- | --- | --- | --- |
| `.env.example` | 模板和变量清单 | `JWT_SECRET=replace-with-at-least-64-random-characters` | 只能作为复制起点，不能作为真实环境配置提交或部署 |
| `.env` 或 `--env-file` | Compose 插值和容器环境 | `DB_PASSWORD`、`REDIS_PASSWORD`、`NGINX_SERVER_NAME` | 每个环境独立维护；生产值来自密钥管理或受控主机，不复用本地值 |
| 宿主机环境变量 | 覆盖 Compose 插值 | `ARK_API_KEY`、`DOUBAO_API_KEY`、`AI_MODEL` | 适合临时注入或 CI/CD 注入；上线前用 `docker compose config` 确认最终值 |
| `docker-compose.yml` | 本地默认值和必填保护 | `MYSQL_*`、`REQUIRE_STRONG_SECRETS=false`、`COOKIE_SECURE=false` | 仅用于本地和内网调试；不要通过改本地默认值来满足生产需求 |
| `docker-compose.prod.yml` | 生产默认值、必填保护和监控栈 | `SPRING_PROFILES_ACTIVE=prod`、`REQUIRE_STRONG_SECRETS=true`、`PUBLIC_METRICS_ENABLED=${PUBLIC_METRICS_ENABLED:-true}` | 生产发布入口；任何公开性、证书、数据库 TLS 决策都要在发布记录中确认 |
| Docker build args | 镜像构建期变量 | `VITE_API_BASE_URL`、`VITE_AMAP_KEY`、`VITE_RECAPTCHA_*` | 前端 build args 会固化到构建产物；修改后必须重建 frontend 镜像 |
| GitHub Actions secrets/settings | CI 运行时 | Gitleaks、Trivy、构建权限 | 不在仓库中保存密钥；通过 GitHub settings 管理并定期轮换 |

关键变量按环境分工如下：

| 类别 | 本地 Compose | 生产 Compose |
| --- | --- | --- |
| Profile | `SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-local}` | `SPRING_PROFILES_ACTIVE=prod` |
| 数据库 | `MYSQL_DATABASE`、`MYSQL_USERNAME`、`MYSQL_PASSWORD` | `DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`、`DB_SSL_MODE`、`DB_JDBC_EXTRA_PARAMS` |
| Redis | 本地 Redis 不要求密码 | `REDIS_PASSWORD` 必填 |
| 强密钥 | 关键密钥必须非空，可用本地随机值 | `JWT_SECRET`、`CSRF_SIGNING_SECRET`、`PII_KEYS`、`ADMIN_ENCRYPTION_KEY` 等必须为强随机生产值 |
| 文档公开 | `PUBLIC_DOCS_ENABLED` 默认 false，可本地打开 | 默认 false，生产公开前必须有明确审批 |
| Metrics 公开 | 应显式评估 `PUBLIC_METRICS_ENABLED` | 当前生产 Compose 默认 true 以便 Prometheus 抓取；公网部署必须配合网络隔离或改为 false |
| 前端公开配置 | 可留空或使用开发 key | `VITE_AMAP_KEY`、`VITE_AMAP_SECURITY_CODE`、`VITE_RECAPTCHA_SITE_KEY` 必须使用生产配置 |
| 反向代理 | 本地 HTTP 和 `localhost` | `NGINX_SERVER_NAME`、`NGINX_CERT_DOMAIN`、证书挂载必须真实可用 |

## Metrics 公开性

后端 Prometheus 指标路径是 `/actuator/prometheus`。应用配置默认 `PUBLIC_METRICS_ENABLED=false`，此时该路径要求管理员权限；当前 `docker-compose.prod.yml` 将其默认展开为 true，目的是让同一 Compose 网络里的 Prometheus 无需管理员 token 即可抓取。

这里的“公开”指任何能连到 backend 的网络主体都可无认证读取 metrics，不等同于一定暴露到公网。生产安全边界必须同时满足：

- backend 宿主机端口保持 `127.0.0.1:8080:8080` 或处在受控内网。
- Nginx 和外部反向代理不得把 `/actuator/prometheus` 代理给公网用户。
- Prometheus、Grafana、Alertmanager、Zipkin 的宿主机端口保持 `127.0.0.1` 绑定，或放到带认证和访问控制的运维网络。
- 如果 backend 会被公网或非受信网络直接访问，设置 `PUBLIC_METRICS_ENABLED=false`，并改用受控网络、认证代理或防火墙 allowlist 完成采集。

上线评审应把 `PUBLIC_METRICS_ENABLED` 作为显式检查项，不能只凭默认值通过。

## Branch Protection 与 CI

`.github/workflows/ci.yml` 已包含仓库噪声检查、Gitleaks、后端测试、Scrapler 测试、前端 typecheck/build/test/audit，以及 Docker 镜像构建、Trivy 扫描和 SBOM 产物。CI 是否不可绕过不由 YAML 单独保证，必须在 GitHub branch protection 或 repository ruleset 中启用。

对 `main` 和 `master` 至少启用以下规则：

- Require a pull request before merging。
- Require status checks to pass before merging，并把这些 job 设为 required checks：`Repository noise guard`、`Secret scan`、`Backend tests`、`Scrapler tests`、`Frontend typecheck and build`、`Docker image build and scan`。
- Require branches to be up to date before merging。
- Require review from Code Owners 或至少一名 reviewer，并启用 stale review dismissal。
- Do not allow bypassing the above settings，管理员也不应直接 push 或使用跳过 CI 的合并路径。

任何生产发布例外都必须留下书面记录，说明原因、风险、回滚计划和补跑 CI 的结果。

## 发布前配置检查

生产发布前至少执行：

```powershell
docker compose -f docker-compose.prod.yml config
git diff --check
```

同时人工确认：

- 没有模板占位值进入最终 Compose 配置。
- `SPRING_PROFILES_ACTIVE=prod`，`REQUIRE_STRONG_SECRETS=true`，`COOKIE_SECURE=true`。
- `CORS_ALLOWED_ORIGINS`、`NGINX_SERVER_NAME`、`NGINX_CERT_DOMAIN` 与真实域名一致。
- `PUBLIC_DOCS_ENABLED` 和 `PUBLIC_METRICS_ENABLED` 已按公开性评审结论设置。
- CI required checks 全部通过且没有被管理员绕过。
