# 常见问题 - Node.js 版本

## ⚠️ Node.js 18.x 弃用警告

### 问题描述

在安装 Node.js 18.x 时，可能会看到以下警告：

```
DEPRECATION WARNING
Node.js 18.x is no longer actively supported!
You will not receive security or critical stability updates for this version.
You should migrate to a supported version of Node.js as soon as possible.
```

### 解决方案

#### 方案一：使用 Node.js 20 LTS（推荐）

Node.js 20 是当前的 LTS（长期支持）版本，推荐使用：

```bash
# 卸载旧版本（如果已安装）
sudo apt remove nodejs -y

# 安装 Node.js 20 LTS
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo bash -
sudo apt install -y nodejs

# 验证版本
node -v  # 应该显示 v20.x.x
npm -v
```

#### 方案二：使用 Node.js 22（最新版本）

如果需要最新功能，可以使用 Node.js 22：

```bash
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo bash -
sudo apt install -y nodejs
```

#### 方案三：继续使用 Node.js 18（不推荐）

虽然会显示警告，但可以继续使用。不过**不建议**在生产环境使用，因为：
- 不再接收安全更新
- 不再接收稳定性修复
- 可能存在安全风险

### 使用 Docker 部署（推荐）

如果使用 Docker 部署，**无需手动安装 Node.js**。Dockerfile 会自动使用最新的 Node.js 版本。

当前项目的 Dockerfile 已更新为使用 Node.js 20：

```dockerfile
FROM node:20-alpine AS builder
```

### 检查当前 Node.js 版本

```bash
# 查看 Node.js 版本
node -v

# 查看 npm 版本
npm -v

# 查看所有已安装的 Node.js 版本（如果使用 nvm）
nvm list
```

### 使用 nvm 管理多个 Node.js 版本（可选）

如果需要管理多个 Node.js 版本，可以使用 nvm：

```bash
# 安装 nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash

# 重新加载 shell 配置
source ~/.bashrc

# 安装 Node.js 20
nvm install 20

# 使用 Node.js 20
nvm use 20

# 设置为默认版本
nvm alias default 20
```

### 验证项目兼容性

在升级 Node.js 版本后，建议测试项目是否正常工作：

```bash
cd /opt/colorful-tibet/frontend

# 清理旧的 node_modules
rm -rf node_modules package-lock.json

# 重新安装依赖
npm install

# 测试构建
npm run build
```

### 版本兼容性说明

本项目支持以下 Node.js 版本：
- ✅ **Node.js 20 LTS**（推荐）
- ✅ **Node.js 22**（最新版本）
- ⚠️ **Node.js 18**（已弃用，不推荐）

### 相关链接

- [Node.js 官方下载页面](https://nodejs.org/)
- [NodeSource 发行版说明](https://github.com/nodesource/distributions)
- [Node.js LTS 计划](https://nodejs.org/en/about/releases/)

---

**建议**：在生产环境中始终使用 LTS 版本（当前为 Node.js 20），以确保安全性和稳定性。

