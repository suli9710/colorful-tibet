# 部署配置治理说明

本文补充 `docs/DEPLOYMENT.md` 的本地部署步骤，重点说明 local/prod Compose 边界、环境变量来源、metrics 公开性，以及 CI 与分支保护要求。本文只描述治理规则，不改变运行行为。

## Compose 环境边界

| 场景 | Compose 文件 | 启动命令 | 主要用途 | 外部暴露面 |
| --- | --- | --- | --- | --- |
| 本地开发 | `docker-compose.yml` | `docker compose up -d --build` | 本机联调、演示、开发验收 | 前端映射到 `${FRONTEND_HOST_PORT:-80}`；backend、MySQL、Redis、Scrapling 默认绑定到 `127.0.0.1` 的宿主机端口 |
| 生产部署 | `docker-compose.prod.yml` | `docker compose -f docker-compose.prod.yml up -d --build` | HTTPS 前端、生产 profile、监控组件 | 前端发布 `80/443`；backend、Prometheus、Alertmanager、Grafana、Zipkin 默认只绑定 `127.0.0.1`；MySQL 和 Redis 不发布宿主机端口 |

本地 Compose 默认 `SPRING_PROFILES_ACTIVE=local`，允许 HTTP、本地种子内容和较宽松的调试开关。生产 Compose 固定 `SPRING_PROFILES_ACTIVE=prod`，要求真实域名、证书、强密钥、Redis 密码、Scrapling API key、reCAPTCHA 和生产地图配置。

后端启动安全校验除 `SPRING_PROFILES_ACTIVE=prod` 外，也会把 `APP_ENV=production`、`ENVIRONMENT=production`、`RAILWAY_ENVIRONMENT=production` 以及 `K_SERVICE`、`RENDER_SERVICE_ID`、`FLY_APP_NAME`、`WEBSITE_HOSTNAME`、`KUBERNETES_SERVICE_HOST` 等云或 K8s 信号视为生产意图，并强制 cookie-secure、强密钥、禁用 mock callback 和 Scrapling API key 检查。

不要用 `docker-compose.yml` 承载公网生产流量；也不要把从 `.env.example` 复制出的本地 `.env` 直接用于生产。生产发布前必须用 `docker compose -f docker-compose.prod.yml config` 检查最终展开结果，确认没有 `change-me`、`replace-with-*` 或本地 profile 值残留。

生产 Compose 还包含一次性的 `production-preflight` 服务。真正执行 `docker compose -f docker-compose.prod.yml up` 时，它会在 MySQL、Redis、backend、Scrapling 和 Grafana 启动前拒绝基础设施密码、应用签名/加密密钥、PII keyset、Scrapling API key、reCAPTCHA key、前端地图 key 和告警 webhook 的空值或占位值，包括 `DB_PASSWORD`、`MYSQL_ROOT_PASSWORD`、`REDIS_PASSWORD`、`GRAFANA_ADMIN_PASSWORD`、`JWT_SECRET`、`CSRF_SIGNING_SECRET`、`CACHE_KEY_HMAC_SECRET`、`METRICS_SCRAPE_TOKEN`、`ADMIN_ENCRYPTION_KEY`、`PAYMENT_CALLBACK_SECRET`、`PII_KEYS`、`PII_ACTIVE_KID`、`SUPER_ADMIN_TOTP_SECRET`、`SCRAPLING_API_KEY`、`RECAPTCHA_SITE_KEY`、`RECAPTCHA_SECRET_KEY`、`VITE_AMAP_KEY`、`VITE_AMAP_SECURITY_CODE`、`ALERTMANAGER_WEBHOOK_URL` 和 `APP_VERSION`，以及已设置但仍是占位值的 `MYSQL_PASSWORD`。它也会拒绝匿名 metrics、Redis 保护降级、mock 支付、demo/seed content、非 secure cookie、MySQL public key retrieval、Scrapling 匿名访问、reCAPTCHA/anti-bot 关闭、Flyway 关闭或 Hibernate DDL auto 非 `validate` 等生产危险覆盖值，并要求 `SUPER_ADMIN_TOTP_SECRET` 是至少 32 字符的 Base32 secret。`NGINX_SERVER_NAME` 必须是空格分隔 DNS host 列表，`NGINX_REDIRECT_HOST` 和 `NGINX_CERT_DOMAIN` 必须是单一 DNS host，不能是 `example.com`、带 scheme/path/port 的值或其他占位值。`upload-server.ps1` 的远端 preflight 使用同一规则，且会在拼接 Let's Encrypt 证书路径前先校验证书域名，避免绕过 Compose 直接发布。

`TRUST_PROXY_HEADERS` 生产默认关闭，`TRUSTED_PROXY_CIDRS` 生产默认留空。只有确认 backend 的直接来源确实是受控反向代理或负载均衡器时，才可以把 `TRUST_PROXY_HEADERS=true` 并填写具体代理 IP/CIDR，例如单个 `/32` 或明确的负载均衡器子网。不要使用 `10.0.0.0/8`、`172.16.0.0/12`、`192.168.0.0/16`、`0.0.0.0/0` 或 `::/0` 这类宽范围；preflight 会拒绝这些值。

反自动化行为日志默认保留 90 天，并由每日定时任务批量清理。`ANTIBOT_LOG_RETENTION_ENABLED` 在生产必须保持 `true`，`ANTIBOT_LOG_RETENTION_DAYS` 只能设置为 1–365；`ANTIBOT_LOG_CLEANUP_CRON` 和 `ANTIBOT_LOG_CLEANUP_ZONE` 用于调整清理窗口。用户账号删除流程会同步删除该用户关联的行为风控日志，避免已删除账号仍能通过稳定风控标签继续关联。

生产 `scrapling` 服务使用 `no-new-privileges`、`cap_drop: [ALL]`、只读根文件系统和 `/tmp` tmpfs。Scrapling/Playwright 的临时运行态应写入 `/tmp`；不要在生产镜像运行时依赖写入 `/app` 或浏览器安装目录。

## 变量来源矩阵

Docker Compose 展开变量时，宿主机环境变量和 `--env-file`/`.env` 会参与插值，随后 `docker-compose*.yml` 中的 `environment`、`build.args` 和默认值把最终值传入容器。应用内部的 `application*.yml` 默认值只在容器没有提供同名变量时生效。

| 来源 | 作用范围 | 示例 | 治理要求 |
| --- | --- | --- | --- |
| `.env.example` | 模板和变量清单 | `JWT_SECRET=replace-with-at-least-64-random-characters` | 只能作为复制起点，不能作为真实环境配置提交或部署 |
| `.env` 或 `--env-file` | Compose 插值和容器环境 | `DB_PASSWORD`、`REDIS_PASSWORD`、`NGINX_SERVER_NAME`、`NGINX_REDIRECT_HOST` | 每个环境独立维护；生产值来自密钥管理或受控主机，不复用本地值 |
| 宿主机环境变量 | 覆盖 Compose 插值 | `ARK_API_KEY`、`DOUBAO_API_KEY`、`AI_MODEL` | 适合临时注入或 CI/CD 注入；上线前用 `docker compose config` 确认最终值 |
| `docker-compose.yml` | 本地默认值和必填保护 | `MYSQL_*`、`REQUIRE_STRONG_SECRETS=false`、`COOKIE_SECURE=false` | 仅用于本地和内网调试；不要通过改本地默认值来满足生产需求 |
| `docker-compose.prod.yml` | 生产默认值、必填保护和监控栈 | `SPRING_PROFILES_ACTIVE=prod`、`REQUIRE_STRONG_SECRETS=true`、`PUBLIC_METRICS_ENABLED=${PUBLIC_METRICS_ENABLED:-false}` | 生产发布入口；任何公开性、证书、数据库 TLS 决策都要在发布记录中确认 |
| Docker build args | 镜像构建期变量 | `VITE_API_BASE_URL`、`VITE_AMAP_KEY`、`VITE_RECAPTCHA_*` | 前端 build args 会固化到构建产物；修改后必须重建 frontend 镜像 |
| GitHub Actions secrets/settings | CI 运行时 | Gitleaks、Trivy、构建权限 | 不在仓库中保存密钥；通过 GitHub settings 管理并定期轮换 |

关键变量按环境分工如下：

| 类别 | 本地 Compose | 生产 Compose |
| --- | --- | --- |
| Profile | `SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-local}` | `SPRING_PROFILES_ACTIVE=prod` |
| 数据库 | `MYSQL_DATABASE`、`MYSQL_USERNAME`、`MYSQL_PASSWORD` | `DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`、`DB_SSL_MODE`、`DB_JDBC_EXTRA_PARAMS` |
| Redis | 本地 Redis 不要求密码 | `REDIS_PASSWORD` 必填 |
| 强密钥 | 关键密钥必须非空，可用本地随机值 | `JWT_SECRET`、`CSRF_SIGNING_SECRET`、`CACHE_KEY_HMAC_SECRET`、`PII_KEYS`、`ADMIN_ENCRYPTION_KEY` 等必须为强随机生产值 |
| 文档公开 | `PUBLIC_DOCS_ENABLED` 默认 false，可本地打开 | 默认 false，生产公开前必须有明确审批 |
| Metrics 公开 | 应显式评估 `PUBLIC_METRICS_ENABLED` | 生产 Compose 默认 false；内置 Prometheus 使用独立 `METRICS_SCRAPE_TOKEN`，只有配置认证代理、独立内网 management 端口或明确 allowlist 后才可打开匿名采集 |
| 前端公开配置 | 可留空或使用开发 key | `VITE_AMAP_KEY`、`VITE_AMAP_SECURITY_CODE`、`VITE_RECAPTCHA_SITE_KEY` 必须使用生产配置 |
| 反向代理 | 本地 HTTP 和 `localhost` | `NGINX_SERVER_NAME` 可包含多个域名；`NGINX_REDIRECT_HOST` 必须是单一规范 host，不带 scheme、path、port、逗号、空白或控制字符；`NGINX_CERT_DOMAIN` 和证书挂载必须真实可用 |

前端容器启动时会在 `envsubst` 渲染 Nginx 模板前再次校验这些 host 值；`NGINX_SERVER_NAME` 仅允许空格分隔的 DNS host，`NGINX_REDIRECT_HOST` 和 `NGINX_CERT_DOMAIN` 必须是单一 DNS host。生产 HTTPS 模板还包含 default server，未匹配的 Host/SNI 会直接关闭连接，避免落到正式站点。

## Metrics 公开性

后端 Prometheus 指标路径是 `/actuator/prometheus`。应用配置和生产 Compose 默认 `PUBLIC_METRICS_ENABLED=false`，此时该路径要求管理员权限。仓库内置 Prometheus 使用独立的 `METRICS_SCRAPE_TOKEN` Bearer 凭据抓取，不复用管理员 JWT，也不需要打开匿名指标。

`METRICS_SCRAPE_TOKEN` 必须是至少 64 字符的独立随机值，由 backend 与 Prometheus 共享；Compose 启动时只把它写入 Prometheus 的 `/tmp`，并通过 `credentials_file` 读取。外部 Prometheus 应使用同一专用身份、认证代理或独立内部 management 端口；只有在发布记录中证明 metrics 仅对受信采集主体可达时，才可显式设置 `PUBLIC_METRICS_ENABLED=true`。

这里的“公开”指任何能连到 backend 的网络主体都可无认证读取 metrics，不等同于一定暴露到公网。生产安全边界必须同时满足：

- backend 宿主机端口保持 `127.0.0.1:8080:8080` 或处在受控内网。
- Nginx 和外部反向代理不得把 `/actuator/prometheus` 代理给公网用户。
- Prometheus、Grafana、Alertmanager、Zipkin 的宿主机端口保持 `127.0.0.1` 绑定，或放到带认证和访问控制的运维网络。
- 如果 backend 会被公网或非受信网络直接访问，保持 `PUBLIC_METRICS_ENABLED=false`，并改用受控网络、认证代理或防火墙 allowlist 完成采集。

上线评审应把 `PUBLIC_METRICS_ENABLED=false` 作为默认门禁；`scripts/deploy-preflight.ps1` 会拒绝匿名 metrics 和 Redis 保护降级配置。

## Redis 安全依赖

生产限流、登录暴破保护和管理员 TOTP 重放保护必须使用 Redis，并在 Redis 不可用时 fail-closed。`RATE_LIMIT_REDIS_ENABLED=true`、`RATE_LIMIT_REDIS_FAIL_CLOSED=true`、`BRUTE_FORCE_REDIS_ENABLED=true`、`BRUTE_FORCE_REDIS_FAIL_CLOSED=true`、`TOTP_REPLAY_FAIL_CLOSED=true` 是生产 preflight 的必检项；后端启动校验也会在生产 profile、生产环境变量或云运行信号下拒绝关闭这些开关。TOTP 开启 fail-closed 后，Redis 未配置、原子 claim 返回不确定结果或 Redis 异常都会拒绝本次验证码，避免多实例或重启后的验证码重放窗口。

本地环境仍可通过 `RATE_LIMIT_REDIS_FAIL_CLOSED=false`、`BRUTE_FORCE_REDIS_FAIL_CLOSED=false` 和 `TOTP_REPLAY_FAIL_CLOSED=false` 保留单机内存降级，便于 Redis 未启动时开发调试。不要把这些本地值带到生产 `.env`。

## 远程开发代理

`frontend/start-remote.bat` 不内置远程后端地址。远程联调时先显式设置 `REMOTE_BACKEND`，推荐使用 HTTPS：

```bat
set REMOTE_BACKEND=https://api.example.com
frontend\start-remote.bat
```

脚本和 Vite proxy resolver 都会拒绝非 localhost 的明文 HTTP 目标。如果远端后端暂时没有 TLS，请先用 SSH、Cloudflare Tunnel 或同类工具把它映射到本机，再使用 `http://localhost:<port>` 作为 `REMOTE_BACKEND`。

## 运行时密钥暴露控制

生产 Compose 不应把数据库或缓存密码放进长期可见的进程参数。`docker-compose.prod.yml` 中 Redis 通过容器启动时生成的受限临时配置文件读取转义后的 `requirepass`，启动后的主进程参数只包含配置文件路径；Redis healthcheck 使用 `REDISCLI_AUTH` 环境变量传递密码，不使用 `redis-cli -a`。生产 secret 应使用单行强随机值，不要使用包含换行符的多行值。

MySQL healthcheck 使用 `--defaults-extra-file` 读取 root 凭据，并在每次检查退出时删除临时配置文件；本地和生产 Compose 都不要把 `mysqladmin -p...` 形式重新引入。生产主机上仍应限制 Docker socket、容器 exec 权限和宿主机 root 权限，因为这些权限本身足以读取容器环境变量或临时文件。

## Supply-chain Pinning

`CI / Supply chain pin gate` 是发布闸门，不是普通提示。当前工作区已把 GitHub Actions tag ref 固定到通过官方 GitHub 仓库 `git ls-remote` 查询到的 40 位 commit SHA；Scrapler Python 依赖已生成 exact pins + `--hash=sha256:` 锁文件，并在 CI 与 Dockerfile 中启用 `pip --require-hashes`。Dockerfile、Compose 和 workflow 中的外部镜像也已经写入 `name:tag@sha256:<digest>`，并由 `docker-digest-evidence.json` 记录 source/image/digest 证据。后续轮换任何镜像 tag 或 digest 时，必须同步更新配置、resolver source 清单和 evidence JSON。可执行检查脚本是 `npm run check:supply-chain-pins`，脚本测试是 `npm run test:ops`，人工执行清单见 `docs/release-supply-chain-pin-checklist.md`。

已核验并固定的 GitHub Actions refs：

| Action tag | Pinned SHA |
| --- | --- |
| `actions/checkout@v4` | `34e114876b0b11c390a56381ad16ebd13914f8d5` |
| `actions/setup-python@v5` | `a26af69be951a213d495a4c3e4e4022e16d87065` |
| `gitleaks/gitleaks-action@v2` | `dcedce43c6f43de0b836d1fe38946645c9c638dc` |
| `actions/setup-java@v4` | `c1e323688fd81a25caa38c78aa6df2d33d3e20d9` |
| `aquasecurity/trivy-action@v0.36.0` | `a9c7b0f06e461e9d4b4d1711f154ee024b8d7ab8` |
| `actions/setup-node@v4` | `49933ea5288caeca8642d1e84afbd3f7d6820020` |
| `docker/setup-buildx-action@v3` | `8d2750c68a42422c14e847fe6c8ac0403b4cbd6f` |
| `actions/upload-artifact@v4` | `ea165f8d65b6e75b540449e92b4886f43607fa02` |

以下 Docker image ref 必须保持 `name:tag@sha256:<digest>` 形式；如果版本或 digest 轮换，需从官方 registry 重新生成证据并同步这些引用：

- `backend/Dockerfile`: `maven:3.9-eclipse-temurin-17`, `eclipse-temurin:17-jre-alpine`
- `frontend/Dockerfile`: `node:22-alpine`, `nginx:alpine`
- `scrapler/Dockerfile`: `python:3.11-slim`
- `docker-compose.yml`: `mysql:8.4`, `redis:7-alpine`
- `docker-compose.prod.yml`: `alpine:3.21`, `redis:7-alpine`, `mysql:8.4`, `prom/prometheus:v2.55.1`, `prom/alertmanager:v0.27.0`, `grafana/grafana:11.4.0`, `openzipkin/zipkin:3.4`
- `.github/workflows/ci.yml` Docker runs/services: `prom/prometheus:v2.55.1`, `mysql:8.4`

轮换镜像 pin 时，在能访问 Docker Hub 的发布工作站上运行：

```powershell
node scripts/resolve-docker-image-digests.mjs --evidence --verifier=<name-or-email> --target-platform=multi-platform-index --output=docker-digest-evidence.json
node scripts/check-supply-chain-pins.mjs --evidence-only --evidence docker-digest-evidence.json
node scripts/resolve-docker-image-digests.mjs --markdown
```

该脚本会通过 Docker Hub token endpoint 和 Docker Registry manifest endpoint 读取 `Docker-Content-Digest`，输出每个源文件位置对应的 `image:tag@sha256:<digest>`。`--output` 会在所有必需镜像都解析成功后，先同步临时文件再原子替换目标文件；不要用 shell 的 `>` 重定向证据文件，因为命令执行前旧文件就会被截断。把新 digest 写回上面的 Dockerfile、Compose 和 workflow 后，再运行 `npm run check:supply-chain-pins` 确认配置引用和 evidence JSON 同步。

`--evidence` 输出带 `schemaVersion`、`resolver`、`generatedAt`、`verifier`、`targetPlatforms`、`lookupSource`、registry、`digestAlgorithm=sha256` 和逐项 source/image/digest 的 JSON 证据。`node scripts/check-supply-chain-pins.mjs --evidence-only --evidence docker-digest-evidence.json` 不访问外网，只校验证据是否由仓库 resolver 产出、是否覆盖所有必需镜像、digest 是否为 `sha256:<64 hex>`、`pinned` 是否等于 `image@digest`，以及同一镜像 tag 是否出现冲突 digest。`npm run check:supply-chain-pins` 默认也会校验根目录 `docker-digest-evidence.json`，所以 digest pins 和操作者证据必须一起提交。发布记录应保存该 evidence JSON；它不能替代把 verified digest 写回配置文件。

Scrapler Python 依赖已经使用 `pip-compile --generate-hashes` 生成 hash lock；`scrapler/requirements.txt` 和 `scrapler/requirements-dev.txt` 均为 `==` exact pins，并且每个包条目带有 `--hash=sha256:`。CI 和 `scrapler/Dockerfile` 都使用 `pip --require-hashes` 安装；dev lock 已将原先的 `pytest>=8.3,<9.0` 范围解析为 `pytest==8.4.2`。后续升级 Python 依赖时必须重新生成 hash lock 并重跑 `npm run check:supply-chain-pins`。

Workflow lint 不再在 CI 中执行 `go install`、`pip install yamllint` 或 `pip install --upgrade pip`。`.github/workflows/ci.yml` 使用 `npm --prefix frontend ci` 安装 `frontend/package-lock.json` 约束的 YAML parser，再执行仓库内 `scripts/validate-workflow-yaml.mjs`。`npm run test:ops` 覆盖 workflow validator、Docker digest resolver 和 supply-chain pin gate fixtures。

建议接受标准：

- `npm run test:ops` 通过，证明门禁 fixture 能同时覆盖 fail-closed 和 operator-supplied verified pins 两条路径。
- `node scripts/check-supply-chain-pins.mjs --evidence-only --evidence docker-digest-evidence.json` 通过，证明联网机器产出的 Docker digest evidence 可在离线环境审计。
- `npm run check:supply-chain-pins` 不再输出 unpinned action、image、workflow image、workflow tool install、Python range、missing hash 或 missing `pip --require-hashes` 报错。
- `docker compose --env-file .env.example -f docker-compose.yml config --quiet` 和 `docker compose --env-file .env.example -f docker-compose.prod.yml config --quiet` 仍然通过。
- `npm --prefix frontend run test -- --run src/utils/opsConfigGuardrails.test.ts` 仍然通过，并且文档中的人工清单已经随实际 pin 状态更新。

## Branch Protection 与 CI

`.github/workflows/ci.yml` 已包含仓库噪声检查、Gitleaks、后端测试、Scrapler 测试、前端 typecheck/build/test/audit，以及 Docker 镜像构建、Trivy 扫描和 SBOM 产物。CI 是否不可绕过不由 YAML 单独保证，必须在 GitHub branch protection 或 repository ruleset 中启用。

对 `main` 和 `master` 至少启用以下规则：

- Require a pull request before merging。
- Require status checks to pass before merging，并把这些 job 设为 required checks：`Repository noise guard`、`Supply chain pin gate`、`Secret scan`、`Backend tests`、`Scrapler tests`、`Frontend typecheck and build`、`Docker image build and scan`。
- Require branches to be up to date before merging。
- Require review from Code Owners 或至少一名 reviewer，并启用 stale review dismissal。
- Do not allow bypassing the above settings，管理员也不应直接 push 或使用跳过 CI 的合并路径。

任何生产发布例外都必须留下书面记录，说明原因、风险、回滚计划和补跑 CI 的结果。

## 发布前配置检查

生产发布前至少执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/deploy-preflight.ps1 -Stage release -EnvFile .env.production -DockerDigestEvidence docker-digest-evidence.json
docker compose -f docker-compose.prod.yml config
git diff --check
```

`docs/release-staging-runbook.md` contains the complete release/staging
operator sequence, including the PII backfill guard, admin audit migration,
Docker digest evidence handling, and rollback checklist.

同时人工确认：

- 没有模板占位值进入最终 Compose 配置。
- `SPRING_PROFILES_ACTIVE=prod`，`REQUIRE_STRONG_SECRETS=true`，`COOKIE_SECURE=true`。
- `CORS_ALLOWED_ORIGINS`、`NGINX_SERVER_NAME`、`NGINX_REDIRECT_HOST`、`NGINX_CERT_DOMAIN` 与真实域名一致，且 `NGINX_REDIRECT_HOST` 是单一 host，不包含 scheme、path、port、逗号、空白或控制字符。
- `PUBLIC_DOCS_ENABLED` 和 `PUBLIC_METRICS_ENABLED` 已按公开性评审结论设置。
- CI required checks 全部通过且没有被管理员绕过。

### Administrator TOTP map

`ADMIN_TOTP_SECRETS` is an optional deployment variable for ordinary
administrators. Use semicolon-separated `username=Base32Secret` entries; the
syntax-only form is `admin=<CSPRNG-generated-160-bit-Base32-secret>`, not a
copyable shared example. Leave it empty only when the configured super-admin is
the sole administrator. The production Compose preflight and
`scripts/deploy-preflight.ps1` require complete 8-character Base32 blocks with
at least 160 bits, canonical uppercase encoding, and reject low-diversity or
published test values, duplicate usernames, obvious ascending/descending
Base32 sequences, periodic repeated blocks, placeholders, and reused secrets,
without printing secret values. These static checks cannot prove entropy or
random provenance: generate every credential from 20 random bytes with an
operating-system CSPRNG and deliver it through the approved secret manager. The
HTTP demo generator creates a separate admin secret and writes its one-time
`otpauth://` URI to the protected first-login file.

Administrator JWTs are cryptographically bound to the TOTP credential that was
current when the token was issued. Rotating or removing either
`SUPER_ADMIN_TOTP_SECRET` or an `ADMIN_TOTP_SECRETS` entry therefore invalidates
that administrator's existing JWTs as soon as the new configuration is active.
This configuration supports one active credential per administrator, not an
old/new overlap. For a multi-instance deployment, first stop or drain all
administrator authentication and API traffic, atomically replace the secret in
the shared deployment configuration, fully restart every instance, verify that
no old instance remains, and only then restore administrator traffic. Do not use
a rolling mixed-secret deployment: an old instance could continue accepting and
issuing tokens bound to the superseded credential.

`CACHE_KEY_HMAC_SECRET` derives both the TOTP replay-scope labels stored in Redis
and the administrator JWT MFA-binding (`mfb`) claim. Treat its rotation with the
same coordinated boundary: stop or drain administrator authentication and API
traffic, atomically update the shared secret, fully restart every instance,
confirm no process still has the old key, and then restore traffic. Never use a
rolling mixed-key deployment for `CACHE_KEY_HMAC_SECRET`; a mixed fleet would
disagree about replay claims and reject or accept administrator JWT bindings
inconsistently. Plan for all existing administrator sessions to reauthenticate.
