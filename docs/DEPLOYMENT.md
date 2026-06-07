# 七彩西藏本地开发部署文档

本文档按当前本地开发环境编写，适用于在 `C:\Users\Suli\Desktop\colorful-tibet` 目录下启动和调试项目。重点覆盖 Windows + PowerShell + WSL Ubuntu + Docker Compose 的本地运行方式，也补充了前后端分开调试的开发模式。

本地部署默认使用 `docker-compose.yml`，不是 `docker-compose.prod.yml`。本地环境使用 HTTP，前端容器监听宿主机 `80` 端口，后端监听 `8080`，MySQL 映射到 `3307`，Redis 映射到 `6380`，Scrapling 映射到 `8000`。

## 1. 本地部署架构

本地 Compose 会启动 5 个服务：

| 服务 | 容器名 | 说明 | 本机访问 |
| --- | --- | --- | --- |
| `frontend` | `colorful-tibet-frontend` | Nginx 托管前端构建产物，代理 `/api`、`/uploads`、部分图片资源 | `http://localhost` |
| `backend` | `colorful-tibet-backend` | Spring Boot 后端 API | `http://localhost:8080` |
| `mysql` | `colorful-tibet-mysql` | MySQL 8.4，本地数据卷持久化 | `127.0.0.1:3307` |
| `redis` | `colorful-tibet-redis` | Redis 7，本地缓存和限流 | `127.0.0.1:6380` |
| `scrapling` | `colorful-tibet-scrapling` | 票价抓取微服务 | `http://localhost:8000` |

本地前端容器使用 `frontend/nginx.http.conf`，并设置 `NGINX_HTTP_ONLY=true`，所以不需要配置 HTTPS 证书。

## 2. 环境准备

推荐本机环境：

- Windows 10/11
- PowerShell 5+ 或 PowerShell 7+
- WSL2，发行版名称建议为 `Ubuntu`
- Docker 可用，可以是 Docker Desktop，也可以是 WSL 内 Docker Engine
- Git
- 可选：JDK 17、Maven 3.9+、Node.js 20+/22+，用于前后端分开调试

检查 WSL：

```powershell
wsl --list --verbose
```

检查 Docker：

```powershell
wsl -d Ubuntu -- bash -lc "docker version && docker compose version"
```

如果使用 Docker Desktop，也可以直接在 PowerShell 中检查：

```powershell
docker version
docker compose version
```

项目自带的 `start.ps1` 会优先使用 `Ubuntu` 这个 WSL 发行版。如果本机 WSL 发行版不是 `Ubuntu`，脚本会自动选择第一个非 `docker-desktop` 发行版。

## 3. 本地环境变量

本地 Compose 会自动读取项目根目录 `.env` 文件。先复制示例：

```powershell
cd C:\Users\Suli\Desktop\colorful-tibet
Copy-Item .env.example .env
```

然后编辑 `.env`。本地至少需要保证下面这些变量有值：

```dotenv
SPRING_PROFILES_ACTIVE=local
VITE_API_BASE_URL=/api

MYSQL_DATABASE=colorful_tibet
MYSQL_USERNAME=colorful_tibet_app
MYSQL_PASSWORD=local-db-password
MYSQL_ROOT_PASSWORD=local-root-password
MYSQL_SSL_MODE=DISABLED
MYSQL_ALLOW_PUBLIC_KEY_RETRIEVAL=true
DB_SSL_MODE=REQUIRED
DB_ALLOW_PUBLIC_KEY_RETRIEVAL=false
DB_JDBC_EXTRA_PARAMS=

JWT_SECRET=replace-with-at-least-64-random-characters-for-local-dev
CSRF_SIGNING_SECRET=replace-with-a-different-64-random-character-local-secret
ADMIN_ENCRYPTION_KEY=replace-with-at-least-64-random-characters-for-local-dev
PAYMENT_CALLBACK_SECRET=replace-with-at-least-64-random-characters-for-local-dev

PII_KEYS=v1:replace-with-base64-32-byte-key
PII_ACTIVE_KID=v1
PII_ENCRYPTION_KEY=replace-with-at-least-64-random-characters-for-local-dev
SUPER_ADMIN_USERNAME=lzh
SUPER_ADMIN_TOTP_SECRET=JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP

REQUIRE_STRONG_SECRETS=false
COOKIE_SECURE=false
PUBLIC_DOCS_ENABLED=true
SEED_CONTENT_ENABLED=true

FRONTEND_HOST_PORT=80
BACKEND_HOST_PORT=8080
MYSQL_HOST_PORT=3307
REDIS_HOST_PORT=6380
SCRAPLING_HOST_PORT=8000
SCRAPLING_API_KEY=local-scrapling-dev-secret
SCRAPLING_ALLOW_UNAUTHENTICATED=false
```

生成本地随机密钥的 PowerShell 示例：

```powershell
$bytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

生成 `PII_KEYS` 中的 32 字节 Base64 key：

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
"PII_KEYS=v1:$([Convert]::ToBase64String($bytes))"
```

说明：

- 本地 `REQUIRE_STRONG_SECRETS=false`，但 Compose 仍要求关键变量非空。
- `PII_KEYS` 格式必须是 `kid:base64-32-byte-key`，例如 `v1:xxxx`。
- `SUPER_ADMIN_TOTP_SECRET` 本地可先使用示例 Base32 值，真实联调二次认证时再替换为认证器 App 中的密钥。
- AI Key 可以留空；没有 `DOUBAO_API_KEY` 或 `ARK_API_KEY` 时，AI 路线相关功能应使用本地兜底逻辑或返回可理解的错误。
- `SCRAPLING_API_KEY` 同时传给 backend 和 scrapling；只有 loopback-only 本地调试才可以临时设置 `SCRAPLING_ALLOW_UNAUTHENTICATED=true`。

生产数据库 TLS 说明：

`docker-compose.prod.yml`、`.env.example` 和 `application-prod.yml` 的生产默认值保持一致：

```dotenv
DB_SSL_MODE=REQUIRED
DB_ALLOW_PUBLIC_KEY_RETRIEVAL=false
DB_JDBC_EXTRA_PARAMS=
```

这些默认值适用于项目自带 MySQL 服务的直接部署路径：JDBC 会要求加密传输，但不要求为 Compose 管理的数据库额外配置 truststore。

如果接入外部 MySQL，建议使用身份校验并继续禁用 public key retrieval：

```dotenv
DB_SSL_MODE=VERIFY_IDENTITY
DB_ALLOW_PUBLIC_KEY_RETRIEVAL=false
DB_JDBC_EXTRA_PARAMS=&trustCertificateKeyStoreUrl=file:/run/secrets/mysql-truststore.p12&trustCertificateKeyStorePassword=change-me&trustCertificateKeyStoreType=PKCS12
```

`DB_JDBC_EXTRA_PARAMS` 会追加到已有 JDBC 查询串后面，因此必须以 `&` 开头。生产环境不要使用 `DB_SSL_MODE=DISABLED`。只有在有明确记录的临时运维场景下，才可以设置 `DB_ALLOW_PUBLIC_KEY_RETRIEVAL=true`。

## 4. 推荐方式：一键启动

项目提供了 Windows 启动脚本：

```powershell
cd C:\Users\Suli\Desktop\colorful-tibet
.\start.ps1
```

也可以双击或运行：

```powershell
.\start.bat
```

脚本会做这些事情：

- 检查 WSL 是否可用。
- 尝试启动 WSL 内 Docker daemon。
- 将本机可用的 AI 环境变量转发给 WSL/Docker。
- 计算构建输入指纹，必要时自动执行 `docker compose up -d --build`。
- 等待 `mysql`、`redis`、`scrapling`、`backend`、`frontend` 就绪。
- 默认自动打开 `http://localhost`。

脚本支持几个常用开关：

```powershell
# 强制重建镜像
$env:COLORFUL_TIBET_REBUILD = "1"
.\start.ps1

# 不自动打开浏览器
$env:COLORFUL_TIBET_NO_BROWSER = "1"
.\start.ps1

# 脚本结束时不暂停
$env:COLORFUL_TIBET_NO_PAUSE = "1"
.\start.ps1

# 跳过自动重建判断，直接快速启动已有镜像
$env:COLORFUL_TIBET_SKIP_AUTO_REBUILD = "1"
.\start.ps1
```

启动成功后访问：

- 前端首页：http://localhost
- 后端健康检查：http://localhost:8080/actuator/health/readiness
- 后端 Swagger：http://localhost:8080/swagger-ui.html，需 `PUBLIC_DOCS_ENABLED=true`
- Scrapling 健康检查：http://localhost:8000/health

## 5. 手动 Docker Compose 启动

如果不使用脚本，可以手动执行：

```powershell
cd C:\Users\Suli\Desktop\colorful-tibet
docker compose config
docker compose up -d --build
```

查看状态：

```powershell
docker compose ps
```

查看日志：

```powershell
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
docker compose logs -f redis
docker compose logs -f scrapling
```

停止服务：

```powershell
docker compose down
```

停止并删除 MySQL、Redis 数据卷：

```powershell
docker compose down -v
```

注意：`docker compose down -v` 会删除本地数据库和 Redis 数据，只在需要重置环境时使用。

## 6. 本地开发调试模式

本地有两种常用方式。

### 6.1 全容器模式

适合完整联调和验收：

```powershell
docker compose up -d --build
```

特点：

- 前端使用 Dockerfile 构建后的静态产物。
- Nginx 代理 `/api` 到后端容器。
- 适合检查 Dockerfile、Nginx 配置、后端健康检查、上传路径和容器间通信。
- 前端代码修改后需要重新构建前端镜像。

只重建前端：

```powershell
docker compose build frontend
docker compose up -d frontend
```

只重建后端：

```powershell
docker compose build backend
docker compose up -d backend
```

### 6.2 前后端热更新模式

适合日常写代码。只用 Docker 跑 MySQL、Redis 和 Scrapling，前端和后端在本机直接启动：

```powershell
docker compose up -d mysql redis scrapling
```

启动后端：

```powershell
cd C:\Users\Suli\Desktop\colorful-tibet\backend

$env:SPRING_PROFILES_ACTIVE = "local"
$env:MYSQL_HOST = "localhost"
$env:MYSQL_PORT = "3307"
$env:MYSQL_DATABASE = "colorful_tibet"
$env:MYSQL_USERNAME = "colorful_tibet_app"
$env:MYSQL_PASSWORD = "local-db-password"
$env:MYSQL_SSL_MODE = "DISABLED"
$env:MYSQL_ALLOW_PUBLIC_KEY_RETRIEVAL = "true"
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6380"
$env:SCRAPLING_SERVICE_URL = "http://localhost:8000"
$env:JWT_SECRET = "replace-with-at-least-64-random-characters-for-local-dev"
$env:CSRF_SIGNING_SECRET = "replace-with-a-different-64-random-character-local-secret"
$env:ADMIN_ENCRYPTION_KEY = "replace-with-at-least-64-random-characters-for-local-dev"
$env:PAYMENT_CALLBACK_SECRET = "replace-with-at-least-64-random-characters-for-local-dev"
$env:PII_KEYS = "v1:replace-with-base64-32-byte-key"
$env:PII_ACTIVE_KID = "v1"
$env:PII_ENCRYPTION_KEY = "replace-with-at-least-64-random-characters-for-local-dev"
$env:SUPER_ADMIN_TOTP_SECRET = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP"
$env:REQUIRE_STRONG_SECRETS = "false"
$env:COOKIE_SECURE = "false"
$env:PUBLIC_DOCS_ENABLED = "true"

mvn spring-boot:run
```

启动前端：

```powershell
cd C:\Users\Suli\Desktop\colorful-tibet\frontend
npm install
npm run dev
```

访问：

- Vite 前端：http://localhost:5173
- 后端 API：http://localhost:8080

Vite 已在 `frontend/vite.config.ts` 中配置代理：

- `/api` -> `http://localhost:8080`
- `/images` -> `http://localhost:8080`，但优先使用 `frontend/public` 中的本地静态资源
- `/uploads` -> `http://localhost:8080`

## 7. 本地端口说明

默认端口来自 `docker-compose.yml`：

| 用途 | 默认端口 | 修改变量 |
| --- | --- | --- |
| 前端 Nginx | `127.0.0.1:80` | `FRONTEND_HOST_PORT` |
| 后端 API | `8080` | `BACKEND_HOST_PORT` |
| MySQL | `3307` | `MYSQL_HOST_PORT` |
| Redis | `6380` | `REDIS_HOST_PORT` |
| Scrapling | `8000` | `SCRAPLING_HOST_PORT` |

`docker-compose.yml` 默认把前端绑定到 `127.0.0.1`，因此本地演示不会把 HTTP 前端暴露到所有网卡；只有主动修改 Compose 端口映射时才会对外监听。

如果 `80` 端口被占用，可以在 `.env` 中改成：

```dotenv
FRONTEND_HOST_PORT=8081
```

然后重启：

```powershell
docker compose up -d frontend
```

访问地址变为 `http://localhost:8081`。

全容器模式下，修改 `BACKEND_HOST_PORT` 只影响宿主机访问端口，前端容器仍通过 Docker 网络访问 `backend:8080`。热更新模式下，Vite 代理默认指向 `localhost:8080`，因此后端本机调试建议保持 `8080`。

## 8. 启动后验证

基础验证：

```powershell
docker compose ps
curl.exe http://localhost/health
curl.exe http://localhost:8080/actuator/health/readiness
curl.exe http://localhost:8000/health
```

端口验证：

```powershell
Test-NetConnection 127.0.0.1 -Port 80
Test-NetConnection 127.0.0.1 -Port 8080
Test-NetConnection 127.0.0.1 -Port 3307
Test-NetConnection 127.0.0.1 -Port 6380
Test-NetConnection 127.0.0.1 -Port 8000
```

MySQL 验证：

```powershell
docker compose exec mysql mysql -uroot -p
```

Redis 验证：

```powershell
docker compose exec redis redis-cli ping
```

页面验证：

- 打开 `http://localhost`。
- 检查首页、景点、非遗、新闻、路线规划、酒店页面。
- 登录和注册流程正常。
- 访问 `/admin` 时未登录会跳转登录页。
- 上传图片后，确认 `/uploads/...` 可以访问。
- AI 路线生成在无 API Key 时有兜底表现；配置 API Key 后能正常返回。

## 9. 常用维护命令

查看所有服务状态：

```powershell
docker compose ps
```

查看最近日志：

```powershell
docker compose logs --tail=200 backend
docker compose logs --tail=200 frontend
docker compose logs --tail=200 mysql
```

进入后端容器：

```powershell
docker compose exec backend sh
```

进入 MySQL：

```powershell
docker compose exec mysql mysql -uroot -p
```

重启单个服务：

```powershell
docker compose restart backend
docker compose restart frontend
```

重建并重启：

```powershell
docker compose up -d --build
```

清理未使用镜像：

```powershell
docker image prune
```

## 10. 本地数据与文件

本地持久化位置：

| 内容 | 位置 |
| --- | --- |
| MySQL 数据 | Docker volume `colorful-tibet_mysql_data` |
| Redis 数据 | Docker volume `colorful-tibet_redis_data` |
| 后端上传和运行数据 | `C:\Users\Suli\Desktop\colorful-tibet\data` |
| 后端日志 | `C:\Users\Suli\Desktop\colorful-tibet\logs` |

备份本地 MySQL：

```powershell
docker compose exec -T mysql mysqldump -uroot -p --databases colorful_tibet > backup-colorful-tibet.sql
```

恢复本地 MySQL：

```powershell
docker compose exec -T mysql mysql -uroot -p < backup-colorful-tibet.sql
```

重置本地数据库：

```powershell
docker compose down -v
docker compose up -d --build
```

重置会删除 MySQL 和 Redis 的 Docker volume。`data/` 和 `logs/` 是宿主机目录，不会被 `down -v` 自动删除。

## 11. 前端与后端检查

前端类型检查和构建：

```powershell
cd C:\Users\Suli\Desktop\colorful-tibet\frontend
npm run typecheck
npm run build
```

后端编译和测试：

```powershell
cd C:\Users\Suli\Desktop\colorful-tibet\backend
mvn -q -DskipTests compile
mvn -q test
```

Scrapling 测试：

```powershell
cd C:\Users\Suli\Desktop\colorful-tibet
scrapler\.venv\Scripts\python.exe -m unittest discover -s scrapler -p "test_*.py"
```

如果没有 `scrapler\.venv`，可以只通过 Docker 容器验证 `http://localhost:8000/health`。

## 12. 常见问题

### 12.1 Compose 提示环境变量缺失

常见报错：

```text
MYSQL_PASSWORD is required
JWT_SECRET is required
PII_KEYS is required
SUPER_ADMIN_TOTP_SECRET is required
```

处理：

- 确认根目录存在 `.env`。
- 确认 `.env` 中变量不是空值。
- 如果 `.env.example` 没有某些变量，按本文第 3 节手动补上。
- 执行 `docker compose config` 检查最终配置。

### 12.2 80 端口被占用

检查端口：

```powershell
netstat -ano | findstr ":80"
```

解决方式：

- 关闭占用 80 端口的本地服务。
- 或在 `.env` 中设置 `FRONTEND_HOST_PORT=8081`，然后访问 `http://localhost:8081`。

### 12.3 MySQL 密码改了但容器还是连不上

MySQL 第一次初始化后，账号密码会保存在 Docker volume 中。后续只改 `.env` 不会自动修改已存在数据库用户。

本地可接受丢数据时，直接重置：

```powershell
docker compose down -v
docker compose up -d --build
```

如果要保留数据，需要进入 MySQL 手动修改用户密码。

### 12.4 后端健康检查一直失败

查看日志：

```powershell
docker compose logs -f backend
```

重点检查：

- 数据库连接是否成功。
- Redis 是否可连接。
- `JWT_SECRET`、`PII_KEYS`、`PII_ACTIVE_KID`、`SUPER_ADMIN_TOTP_SECRET` 是否存在。
- `SPRING_PROFILES_ACTIVE` 是否为 `local`。
- 本地 profile 可以使用 `MYSQL_SSL_MODE=DISABLED`；生产环境应使用 `DB_SSL_MODE=REQUIRED` 或 `VERIFY_IDENTITY`。

### 12.5 前端页面能打开但接口失败

检查后端健康：

```powershell
curl.exe http://localhost:8080/actuator/health/readiness
```

检查 Nginx 代理日志：

```powershell
docker compose logs --tail=100 frontend
docker compose logs --tail=100 backend
```

全容器模式下，前端 API 基础路径应保持：

```dotenv
VITE_API_BASE_URL=/api
```

### 12.6 WSL 或 Docker 启动失败

检查 WSL 状态：

```powershell
wsl --list --verbose
```

手动启动 WSL 内 Docker：

```powershell
wsl -d Ubuntu
sudo service docker start
docker ps
```

如果使用 Docker Desktop，确认 Docker Desktop 已启动，并启用了 WSL integration。

### 12.7 AI 功能不可用

本地默认允许 AI Key 为空。需要真实调用模型时，在 `.env` 中配置：

```dotenv
DOUBAO_API_KEY=你的Key
DOUBAO_API_URL=https://ark.cn-beijing.volces.com/api/v3/responses
DOUBAO_MODEL=你的模型ID
AI_MODEL=你的模型ID
AI_STREAM_TIMEOUT=180
```

如果通过 `start.ps1` 启动，也可以把这些变量设置在 Windows 用户环境变量中；脚本会转发 `ARK_*`、`DOUBAO_*`、`AI_MODEL`、`AI_STREAM_TIMEOUT` 到 WSL/Docker。

### 12.8 Scrapling 健康检查失败

查看日志：

```powershell
docker compose logs -f scrapling
```

常用本地配置：

```dotenv
SCRAPLING_MODE=basic
SCRAPLING_TIMEOUT_SECONDS=30
SCRAPLING_RETRIES=2
SCRAPLING_MAX_SOURCES=4
SCRAPLING_SEARCH_PROVIDERS=baidu,bing
SCRAPLING_HEALTH_ENABLED=true
SCRAPLING_API_KEY=local-scrapling-dev-secret
SCRAPLING_ALLOW_UNAUTHENTICATED=false
```

本地网络不稳定时，可以先确认服务健康：

```powershell
curl.exe http://localhost:8000/health
```

### 12.9 HTTP 服务器部署脚本

`deploy-new-server-http.ps1` 只用于演示环境。它会写入 local profile 的 HTTP 配置，设置 `REQUIRE_STRONG_SECRETS=false` 并启用模拟支付回调；脚本现在要求显式传入 `-DemoOnly` 才会运行。

脚本不再自动信任未知 SSH 主机密钥。请先通过其他可信渠道核对服务器密钥，再把它加入 `~/.ssh/known_hosts`；也可以传入 `-SshHostKeyFingerprint SHA256:<fingerprint>`，在首次连接时固定该指纹。

任何公开生产部署都应使用 `docker-compose.prod.yml`，并配置真实 HTTPS 证书和生产密钥。

## 13. 本地开发建议

- 日常改前端页面时，优先用热更新模式：Docker 跑依赖服务，本机跑 `npm run dev`。
- 联调登录、上传、Nginx 代理、Dockerfile 时，用全容器模式。
- 修改 `frontend/Dockerfile`、`frontend/nginx.http.conf`、`backend/Dockerfile`、`backend/pom.xml` 后，需要重新构建镜像。
- 修改 `.env` 后，通常需要重启相关容器。
- 修改 MySQL 初始化账号密码后，如果 volume 已存在，需要手动改库内账号或重置 volume。
- 本地不要提交 `.env`、`data/`、`logs/`、`frontend/dist/`、`backend/target/`。
