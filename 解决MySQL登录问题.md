# 解决 MySQL root 登录问题

## 🔍 问题

错误：`ERROR 1045 (28000): Access denied for user 'root'@'localhost' (using password: YES)`

这说明：
- MySQL root 密码可能没有正确设置
- 或者需要使用不同的方式登录

## ✅ 解决方案

### 方法1：使用 sudo mysql（推荐，最简单）

Ubuntu 22.04 默认允许使用 `sudo mysql` 直接登录，无需密码：

```bash
sudo mysql
```

然后就可以直接进入 MySQL 命令行，执行以下命令：

```sql
-- 1. 设置root密码（使用之前生成的密码）
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'mBh7FriAwM2uybEqQGU8yJwxVGnw6H1N2dxP/ncaIBs=';
FLUSH PRIVILEGES;

-- 2. 创建数据库
CREATE DATABASE IF NOT EXISTS tibet_tourism CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 3. 创建用户并设置密码（替换 your_password 为您想要的密码）
CREATE USER IF NOT EXISTS 'tibet_user'@'localhost' IDENTIFIED BY 'your_password';

-- 4. 授权
GRANT ALL PRIVILEGES ON tibet_tourism.* TO 'tibet_user'@'localhost';

-- 5. 刷新权限
FLUSH PRIVILEGES;

-- 6. 退出
EXIT;
```

### 方法2：重置 root 密码

如果方法1不行，可以重置密码：

```bash
# 1. 停止MySQL
sudo systemctl stop mysql

# 2. 以安全模式启动MySQL（跳过权限检查）
sudo mysqld_safe --skip-grant-tables &

# 3. 等待几秒
sleep 5

# 4. 登录MySQL（无需密码）
mysql -u root

# 5. 在MySQL中执行
USE mysql;
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'mBh7FriAwM2uybEqQGU8yJwxVGnw6H1N2dxP/ncaIBs=';
FLUSH PRIVILEGES;
EXIT;

# 6. 停止安全模式的MySQL
sudo pkill mysqld

# 7. 正常启动MySQL
sudo systemctl start mysql
```

## 🎯 推荐操作流程

**最简单的方法**：

```bash
# 1. 使用sudo mysql登录
sudo mysql

# 2. 在MySQL中执行（一次性执行所有命令）
CREATE DATABASE IF NOT EXISTS tibet_tourism CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'tibet_user'@'localhost' IDENTIFIED BY 'Tibet2024!Tourism';
GRANT ALL PRIVILEGES ON tibet_tourism.* TO 'tibet_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

**注意**：上面的密码 `Tibet2024!Tourism` 可以替换为您想要的任何密码。

## ✅ 验证

创建完成后，测试连接：

```bash
# 测试tibet_user连接
mysql -u tibet_user -p tibet_tourism
# 输入您设置的密码（如：Tibet2024!Tourism）
```

如果成功进入MySQL命令行，说明配置正确！

## 📝 配置 application.yml

现在您可以在 `application.yml` 中使用：

```yaml
spring:
  datasource:
    username: tibet_user
    password: Tibet2024!Tourism  # 您在上面设置的密码
```

## 🔐 密码建议

- **简单易记**：`Tibet2024!Tourism`
- **更安全**：`Tibet_DB_2024_Pass!`
- **随机生成**：`openssl rand -base64 16`

---

**总结**：直接使用 `sudo mysql` 登录，无需密码，然后创建数据库和用户即可！


