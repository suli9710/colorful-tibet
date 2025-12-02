# API 地址配置说明

本项目支持多种部署模式，可以通过环境变量灵活配置 API 地址。

## 配置方式

### 方式一：使用环境变量（推荐）

#### 本地开发模式（默认）
```bash
# 使用 Vite proxy，自动转发到 http://localhost:8080
# 不需要设置环境变量，默认使用 /api
npm run dev
```

#### 远程部署模式（FRP隧道）
```bash
# Windows
set VITE_API_BASE_URL=http://1.15.29.168:6000/api
npm run dev

# Linux/Mac
export VITE_API_BASE_URL=http://1.15.29.168:6000/api
npm run dev
```

#### 生产环境
```bash
# 设置生产环境API地址
set VITE_API_BASE_URL=https://your-backend.com/api
npm run build
```

### 方式二：使用启动脚本

#### 本地开发
使用项目根目录的 `start.bat` 脚本，会自动启动后端和前端（本地模式）。

#### 远程连接
使用 `frontend/start-remote.bat` 脚本，会自动设置远程API地址并启动前端。

### 方式三：创建 .env 文件（可选）

在 `frontend` 目录下创建 `.env.local` 文件：

```env
# 本地开发
VITE_API_BASE_URL=/api
```

或创建 `.env.remote` 文件：

```env
# 远程部署
VITE_API_BASE_URL=http://1.15.29.168:6000/api
```

然后使用对应的模式启动：
```bash
# 使用 .env.local
npm run dev

# 或使用 .env.remote（需要重命名为 .env.local）
# 注意：Vite 默认只读取 .env.local 和 .env
```

## 配置优先级

1. **环境变量** `VITE_API_BASE_URL`（最高优先级）
2. **默认值** `/api`（本地开发模式）

## 当前配置查看

启动前端服务后，在浏览器控制台会显示当前使用的 API 地址：
```
🌐 API Base URL: /api
```
或
```
🌐 API Base URL: http://1.15.29.168:6000/api
```

## 常见场景

### 场景1：本地开发（后端在 localhost:8080）
- **配置**：不设置环境变量，使用默认值 `/api`
- **启动**：`npm run dev` 或 `start.bat`

### 场景2：使用 FRP 隧道连接远程后端
- **配置**：`VITE_API_BASE_URL=http://1.15.29.168:6000/api`
- **启动**：`frontend/start-remote.bat` 或手动设置环境变量后运行 `npm run dev`

### 场景3：生产环境部署
- **配置**：`VITE_API_BASE_URL=https://your-production-backend.com/api`
- **构建**：`npm run build`（环境变量会在构建时注入）

## 注意事项

1. **环境变量命名**：必须以 `VITE_` 开头，Vite 才会将其暴露给客户端代码
2. **重启服务**：修改环境变量后需要重启前端服务才能生效
3. **代理配置**：本地开发时，`/api` 路径会被 Vite proxy 转发到 `http://localhost:8080`（见 `vite.config.ts`）
4. **CORS**：如果使用远程地址，确保后端已配置 CORS 允许前端域名访问






