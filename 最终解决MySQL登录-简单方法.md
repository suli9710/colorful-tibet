# 最终解决 MySQL 登录问题 - 简单方法

## 🔍 问题

错误 `ERROR 1698 (28000): Access denied for user 'root'@'localhost'`

这是 Ubuntu 22.04 中 MySQL 8.0 的常见问题，root 用户可能使用 `auth_socket` 认证。

## ✅ 最简单解决方案

### 方法1：直接创建新管理员用户（推荐）

不需要 root 密码，直接创建新用户：

```bash
# 1. 停止MySQL
sudo systemctl stop mysql

# 2. 以安全模式启动
sudo mysqld_safe --skip-grant-tables --skip-networking &

# 3. 等待5秒
sleep 5

# 4. 连接MySQL（无需密码）
mysql -u root
```

在 MySQL 中执行：

```sql
USE mysql;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS tibet_tourism CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建应用用户（直接创建，不需要root）
CREATE USER IF NOT EXISTS 'tibet_user'@'localhost' IDENTIFIED BY 'Tibet2024!Tourism';

-- 给应用用户所有权限
GRANT ALL PRIVILEGES ON tibet_tourism.* TO 'tibet_user'@'localhost';
GRANT ALL PRIVILEGES ON *.* TO 'tibet_user'@'localhost' WITH GRANT OPTION;

FLUSH PRIVILEGES;
EXIT;
```

然后：

```bash
# 停止安全模式
sudo pkill mysqld
sudo pkill mysqld_safe

# 正常启动
sudo systemctl start mysql

# 现在可以使用 tibet_user 登录了
mysql -u tibet_user -p tibet_tourism
# 输入密码：Tibet2024!Tourism
```

### 方法2：修改 root 用户认证方式

```bash
# 1. 停止MySQL
sudo systemctl stop mysql

# 2. 安全模式启动
sudo mysqld_safe --skip-grant-tables --skip-networking &

# 3. 等待
sleep 5

# 4. 连接
mysql -u root
```

在 MySQL 中：

```sql
USE mysql;

-- 修改root用户使用密码认证
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'mBh7FriAwM2uybEqQGU8yJwxVGnw6H1N2dxP/ncaIBs=';
FLUSH PRIVILEGES;
EXIT;
```

然后：

```bash
# 停止安全模式
sudo pkill mysqld
sudo pkill mysqld_safe

# 正常启动
sudo systemctl start mysql

# 现在可以使用密码登录了
mysql -u root -p
# 输入密码：mBh7FriAwM2uybEqQGU8yJwxVGnw6H1N2dxP/ncaIBs=
```

## 🎯 推荐流程（最简单）

**直接创建应用用户，不需要 root 密码：**

```bash
# 1. 停止MySQL
sudo systemctl stop mysql

# 2. 安全模式启动
sudo mysqld_safe --skip-grant-tables --skip-networking &

# 3. 等待
sleep 5

# 4. 连接
mysql -u root
```

在 MySQL 中执行（一次性复制粘贴）：

```sql
USE mysql;
CREATE DATABASE IF NOT EXISTS tibet_tourism CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'tibet_user'@'localhost' IDENTIFIED BY 'Tibet2024!Tourism';
GRANT ALL PRIVILEGES ON tibet_tourism.* TO 'tibet_user'@'localhost';
GRANT ALL PRIVILEGES ON *.* TO 'tibet_user'@'localhost' WITH GRANT OPTION;
FLUSH PRIVILEGES;
EXIT;
```

然后：

```bash
# 停止安全模式
sudo pkill mysqld
sudo pkill mysqld_safe

# 正常启动
sudo systemctl start mysql

# 测试连接
mysql -u tibet_user -p tibet_tourism
# 输入密码：Tibet2024!Tourism
```

## ✅ 配置 application.yml

现在您可以在配置文件中使用：

```yaml
spring:
  datasource:
    username: tibet_user
    password: Tibet2024!Tourism
```

## 🔧 如果安全模式启动失败

如果 `mysqld_safe` 启动失败，检查：

```bash
# 检查MySQL是否在运行
ps aux | grep mysql

# 检查socket目录
ls -la /var/run/mysqld/

# 如果目录不存在，创建它
sudo mkdir -p /var/run/mysqld
sudo chown mysql:mysql /var/run/mysqld
```

## 📝 重要提示

1. **不需要 root 密码**：直接创建应用用户即可
2. **保存密码**：`Tibet2024!Tourism` 是应用数据库密码
3. **安全模式**：完成后务必正常重启 MySQL

---

**总结**：使用安全模式创建数据库和用户，不需要 root 密码，这是最简单的方法！


