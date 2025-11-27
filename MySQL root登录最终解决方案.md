# MySQL root 登录最终解决方案

## 🔍 问题

`ERROR 1045 (28000): Access denied for user 'root'@'localhost'`

在 Ubuntu 22.04 中，MySQL root 用户可能使用 `auth_socket` 认证，无法用密码登录。

## ✅ 解决方案

### 方法1：使用 sudo mysql（最简单，推荐）

**不需要密码，直接登录：**

```bash
sudo mysql
```

这会直接进入 MySQL，无需输入密码。

### 方法2：重置 root 密码为密码认证

如果想用密码登录 root，需要重置：

```bash
# 1. 停止MySQL
sudo systemctl stop mysql

# 2. 创建socket目录
sudo mkdir -p /var/run/mysqld
sudo chown mysql:mysql /var/run/mysqld
sudo chmod 755 /var/run/mysqld

# 3. 启动安全模式
sudo mysqld_safe --skip-grant-tables --skip-networking > /dev/null 2>&1 &
sleep 10

# 4. 进入MySQL
mysql -u root
```

在 `mysql>` 提示符下执行：

```sql
USE mysql;
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '1zh031224';
FLUSH PRIVILEGES;
EXIT;
```

然后：

```bash
# 5. 重启MySQL
sudo pkill mysqld
sudo pkill mysqld_safe
sleep 3
sudo systemctl start mysql
sleep 5

# 6. 测试登录
mysql -u root -p
# 输入密码：1zh031224
```

### 方法3：直接使用 tibet_user（推荐）

既然 `tibet_user` 已经创建好了，直接使用它：

```bash
mysql -u tibet_user -p tibet_tourism
```

输入密码：`lzh031224`

## 🎯 推荐做法

**对于日常操作：**

1. **管理数据库**：使用 `sudo mysql`（最简单）
2. **应用连接**：使用 `tibet_user`（密码：`lzh031224`）

## 📝 密码总结

- **MySQL root**：
  - 使用 `sudo mysql`（无需密码）
  - 或重置后使用密码：`1zh031224`
- **数据库用户**：`tibet_user`（密码：`lzh031224`）

## ✅ 快速操作

**现在直接使用：**

```bash
# 方法1：使用sudo mysql（推荐）
sudo mysql

# 方法2：使用tibet_user
mysql -u tibet_user -p tibet_tourism
# 输入密码：lzh031224
```

---

**建议：直接使用 `sudo mysql` 或 `tibet_user`，不需要纠结 root 密码！**

