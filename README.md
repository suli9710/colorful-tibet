# 七彩西藏 Colorful Tibet

七彩西藏是一个面向西藏旅游场景的全栈 Web 应用，覆盖景点展示、AI 路线规划、路线社区、酒店预订、订单中心、非遗文化、新闻内容和后台管理等功能。项目采用前后端分离架构，前端通过 `/api` 统一访问后端服务，生产环境由 Nginx 托管前端并反向代理 API。

## 功能概览

- 景点浏览、详情、热力图、推荐和票价更新。
- AI 路线生成、流式路线输出、路线收藏和路线社区分享。
- 酒店列表、酒店详情、房型查询、酒店预订和订单管理。
- 非遗内容展示、传承人、活动、评论和点赞。
- 新闻、轮播图、藏语词典、旅行问答和用户个人中心。
- 管理后台支持用户、景点、路线、酒店、非遗、新闻和社区内容管理。
- 安全能力包含 JWT、CSRF、限流、设备指纹、行为校验、敏感信息加密和生产强密钥校验。
- Docker Compose 支持 MySQL、Redis、后端、前端、Scrapling 爬取服务以及生产监控组件。

## 技术栈

后端：

- Java 17
- Spring Boot 3.5.12
- Spring Security + JWT
- Spring Data JPA
- Flyway
- MySQL 8.4
- Redis 7
- Actuator + Prometheus Metrics

前端：

- Vue 3
- Vite 8
- TypeScript
- Pinia
- Vue Router
- vue-i18n
- Tailwind CSS
- Axios

部署与运维：

- Docker Compose
- Nginx
- Prometheus
- Grafana
- Alertmanager
- Zipkin
- Scrapling/FastAPI 票价爬取微服务

## 目录结构

```text
.
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/tibet/tourism/
│   │   ├── common/             # 通用配置、安全、异常、校验
│   │   └── modules/            # 业务模块
│   ├── src/main/resources/
│   │   ├── application*.yml    # 运行环境配置
│   │   └── db/migration/       # Flyway 数据库迁移
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                   # Vue 前端
│   ├── src/views/              # 页面路由组件
│   ├── src/components/         # 复用组件
│   ├── src/api/index.ts        # API 客户端与端点集中定义
│   ├── src/stores/             # Pinia 状态
│   ├── Dockerfile
│   ├── nginx.conf              # HTTPS 生产 Nginx 模板
│   └── nginx.http.conf         # HTTP 本地 Nginx 模板
├── scrapler/                   # 票价爬取微服务
├── monitoring/                 # Prometheus / Grafana / Alertmanager 配置
├── data/                       # 本地持久化数据与上传文件
├── logs/                       # 本地日志
├── docs/
│   └── DEPLOYMENT.md           # 部署文档
├── docker-compose.yml          # 本地 Docker Compose
├── docker-compose.prod.yml     # 生产 Docker Compose
└── .env.example                # 环境变量示例
```

## 环境要求

本地开发建议安装：

- JDK 17
- Maven 3.9+
- Node.js 20+ 或 22+
- npm
- Docker / Docker Compose
- MySQL 8.x
- Redis 7.x

如果使用 Docker Compose 本地启动，只需要 Docker 可用即可，MySQL、Redis、后端、前端和 Scrapling 都会由 Compose 拉起。

## 快速启动

复制环境变量示例：

```powershell
Copy-Item .env.example .env
```

编辑 `.env`，至少替换所有 `change-me` 和 `replace-with-*` 占位值。然后启动本地容器：

```powershell
docker compose up -d --build
```

访问：

- 前端：http://localhost
- 后端健康检查：http://localhost:8080/actuator/health/readiness
- Scrapling 健康检查：http://localhost:8000/health
- MySQL：`127.0.0.1:3307`
- Redis：`127.0.0.1:6380`

Windows 环境也可以使用项目脚本：

```powershell
.\start.ps1
```

停止服务：

```powershell
docker compose down
```

## 本地开发

前端开发：

```powershell
cd frontend
npm install
npm run dev
```

Vite 默认运行在 `http://localhost:5173`，并将 `/api`、`/images`、`/uploads` 代理到 `http://localhost:8080`。

后端开发：

```powershell
cd backend
mvn -q -DskipTests compile
mvn spring-boot:run
```

后端默认端口为 `8080`。本地 profile 可使用：

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
mvn spring-boot:run
```

Scrapling 微服务本地检查：

```powershell
scrapler\.venv\Scripts\python.exe -m unittest discover -s scrapler -p "test_*.py"
```

## 常用命令

前端：

```powershell
cd frontend
npm run typecheck
npm run build
npm run preview
```

后端：

```powershell
cd backend
mvn -q -DskipTests compile
mvn -q test
```

Docker：

```powershell
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
docker compose up -d --build
docker compose down
```

## 关键环境变量

常用变量集中在 `.env.example`。生产环境必须使用强随机密钥，不能沿用示例值。

| 变量 | 说明 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | 运行环境，开发常用 `local`，生产使用 `prod` |
| `VITE_API_BASE_URL` | 前端 API 基础路径，默认 `/api` |
| `MYSQL_DATABASE` / `MYSQL_USERNAME` / `MYSQL_PASSWORD` | 本地 Compose MySQL 配置 |
| `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | 生产 profile 数据库配置 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 配置 |
| `JWT_SECRET` | JWT 签名密钥，生产必须 64 字符以上 |
| `CSRF_SIGNING_SECRET` | CSRF Cookie 签名密钥 |
| `CACHE_KEY_HMAC_SECRET` | 缓存键标签 HMAC 密钥，生产必须独立于 JWT/CSRF 密钥 |
| `PII_KEYS` / `PII_ACTIVE_KID` | 敏感信息加密密钥集合与当前 key id |
| `ADMIN_ENCRYPTION_KEY` | 管理端敏感配置加密密钥 |
| `SUPER_ADMIN_USERNAME` / `SUPER_ADMIN_TOTP_SECRET` | 超级管理员账号与 TOTP 二次认证密钥 |
| `DOUBAO_API_KEY` / `ARK_API_KEY` | AI 路线生成所需模型 API Key |
| `VITE_AMAP_KEY` / `VITE_AMAP_SECURITY_CODE` | 高德地图前端配置 |
| `CORS_ALLOWED_ORIGINS` | 允许跨域来源，生产应设置为正式域名 |
| `NGINX_SERVER_NAME` / `NGINX_CERT_DOMAIN` | 生产 Nginx 域名与证书目录名 |

更多本地部署与运行配置请查看 [部署文档](docs/DEPLOYMENT.md)。

## API 与健康检查

- 后端 API 前缀：`/api`
- Actuator 健康检查：`/actuator/health/readiness`
- Prometheus 指标：`/actuator/prometheus`
- OpenAPI 文档：`/swagger-ui.html`，是否公开由 `PUBLIC_DOCS_ENABLED` 控制
- 前端容器健康检查：`/health`

## 上线前检查

生产发布前建议执行：

```powershell
cd frontend
npm run check

cd ..\backend
mvn -q test
```

涉及 Docker 或部署配置时，同时检查：

- `backend/Dockerfile`
- `frontend/Dockerfile`
- `docker-compose.yml`
- `docker-compose.prod.yml`
- `frontend/nginx.conf`
- `frontend/nginx.http.conf`

## 文档

- [部署文档](docs/DEPLOYMENT.md)
- [部署配置治理说明](docs/deployment-configuration.md)
- [Scrapling 票价服务说明](scrapler/README.md)
- [后端价格抓取说明](backend/PRICE_FETCH_GUIDE.md)
- [后端 Web 抓取说明](backend/WEB_SCRAPING_GUIDE.md)
