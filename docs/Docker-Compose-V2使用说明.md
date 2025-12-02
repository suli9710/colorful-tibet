# Docker Compose V2 使用说明

## 版本说明

Docker Compose 有两个主要版本：
- **V1**：使用 `docker-compose` 命令（带连字符）
- **V2**：使用 `docker compose` 命令（带空格）

**注意**：新版本的 Docker Desktop 和 Docker Engine 默认使用 V2 版本。

## 检查版本

```bash
# 检查 Docker Compose V2
docker compose version

# 如果显示版本号，说明已安装 V2
# 如果显示 "command not found"，可能需要安装
```

## 命令对比

| V1 命令 | V2 命令 |
|---------|---------|
| `docker-compose up` | `docker compose up` |
| `docker-compose down` | `docker compose down` |
| `docker-compose build` | `docker compose build` |
| `docker-compose logs` | `docker compose logs` |
| `docker-compose ps` | `docker compose ps` |
| `docker-compose restart` | `docker compose restart` |
| `docker-compose start` | `docker compose start` |
| `docker-compose stop` | `docker compose stop` |

## 安装 Docker Compose V2

### Ubuntu/Debian

```bash
# 安装 Docker Compose 插件
sudo apt-get update
sudo apt-get install docker-compose-plugin

# 验证安装
docker compose version
```

### CentOS/RHEL

```bash
# 安装 Docker Compose 插件
sudo yum install docker-compose-plugin

# 验证安装
docker compose version
```

## 项目中的使用

本项目所有文档和脚本已更新为使用 `docker compose`（V2 语法）。

### 常用命令

```bash
# 启动服务
docker compose up -d

# 停止服务
docker compose down

# 查看日志
docker compose logs -f

# 查看服务状态
docker compose ps

# 重新构建
docker compose build

# 重新构建并启动
docker compose up -d --build
```

## 兼容性说明

- 如果系统只有 V1（`docker-compose`），可以创建别名：
  ```bash
  alias docker-compose='docker compose'
  ```

- 如果系统只有 V2（`docker compose`），但脚本使用 V1，可以创建符号链接：
  ```bash
  sudo ln -s /usr/libexec/docker/cli-plugins/docker-compose /usr/local/bin/docker-compose
  ```

## 故障排查

### 问题：`docker compose` 命令未找到

**解决方案**：
```bash
# 检查 Docker 版本
docker --version

# 安装 Docker Compose 插件
sudo apt-get update
sudo apt-get install docker-compose-plugin

# 或者使用 V1（如果已安装）
docker-compose --version
```

### 问题：权限错误

**解决方案**：
```bash
# 将用户添加到 docker 组
sudo usermod -aG docker $USER

# 重新登录或运行
newgrp docker
```

## 参考文档

- [Docker Compose V2 官方文档](https://docs.docker.com/compose/)
- [从 V1 迁移到 V2](https://docs.docker.com/compose/migrate/)




