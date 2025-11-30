# MySQL 配置指南

## 🔐 mysql_secure_installation 配置说明

`mysql_secure_installation` 是 MySQL 的安全配置脚本，会引导您完成以下安全设置。

## ⚠️ MySQL 8.0 认证方式说明

**重要提示**：MySQL 8.0 在 Ubuntu 上默认使用 `auth_socket` 插件进行认证，这意味着：
- ✅ 可以使用 `sudo mysql -u root` 直接登录，无需密码
- ⚠️ 如果看到 "Skipping password set for root as authentication with auth_socket is used by default" 消息，这是正常的
- 🔧 如果需要使用密码认证，需要手动设置（见下方说明）

### 使用 auth_socket 认证（默认方式）

```bash
# 直接登录，无需密码
sudo mysql -u root

# 在 MySQL 中执行操作
```

### 切换到密码认证（可选）

如果您希望使用密码登录，可以在 MySQL 中执行：

```sql
-- 登录 MySQL（使用 sudo）
sudo mysql -u root

-- 切换到密码认证
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'your_strong_password';

-- 刷新权限
FLUSH PRIVILEGES;

-- 退出
EXIT;
```

之后就可以使用密码登录：
```bash
mysql -u root -p
```

### 配置步骤详解

#### 1. 密码验证组件（VALIDATE PASSWORD COMPONENT）

**问题**：`Would you like to setup VALIDATE PASSWORD component?`

**选项说明**：
- **输入 `Y` 或 `y`**：启用密码验证组件
  - ✅ 优点：强制使用强密码，提高安全性
  - ⚠️ 缺点：如果后续设置的密码不符合要求，可能无法使用
  - 适合：生产环境，需要高安全性

- **输入其他键（如 `N`）**：不启用密码验证组件
  - ✅ 优点：更灵活，可以使用任意密码
  - ⚠️ 缺点：安全性稍低
  - 适合：开发环境，或需要简单密码的场景

**推荐**：
- **生产环境**：建议输入 `Y`，启用密码验证
- **开发/测试环境**：可以输入 `N`，跳过验证

#### 2. 密码验证级别（如果启用了验证组件）

如果选择了启用密码验证，会继续询问密码验证级别：

```
There are three levels of password validation policy:

LOW    Length >= 8
MEDIUM Length >= 8, numeric, mixed case, and special characters
STRONG Length >= 8, numeric, mixed case, special characters and dictionary file

Please enter 0 = LOW, 1 = MEDIUM and 2 = STRONG:
```

**推荐选择**：
- **0 (LOW)**：密码长度至少 8 个字符（推荐用于开发环境）
- **1 (MEDIUM)**：密码需要包含数字、大小写字母和特殊字符（推荐用于生产环境）
- **2 (STRONG)**：最严格，还需要通过字典检查（适合高安全要求）

#### 3. 设置 root 密码

**问题**：`Please set the password for root here.`

**⚠️ MySQL 8.0 特殊情况**：
- 在 Ubuntu 上，MySQL 8.0 默认使用 `auth_socket` 插件认证
- 可能会看到 "Skipping password set for root as authentication with auth_socket is used by default" 消息
- 这是**正常的**，意味着可以使用 `sudo mysql -u root` 直接登录，无需密码
- 如果需要使用密码认证，可以在配置完成后手动设置（见下方）

**如果正常设置密码**：
- 输入一个强密码（如果启用了验证组件，必须符合要求）
- 密码不会显示在屏幕上（安全考虑）
- 需要输入两次以确认

**示例密码要求（MEDIUM 级别）**：
- ✅ 至少 8 个字符
- ✅ 包含数字（0-9）
- ✅ 包含小写字母（a-z）
- ✅ 包含大写字母（A-Z）
- ✅ 包含特殊字符（!@#$%^&* 等）

**示例**：`MyP@ssw0rd123`

**切换到密码认证（如果跳过了密码设置）**：

```sql
-- 使用 sudo 登录（无需密码）
sudo mysql -u root

-- 切换到密码认证
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'your_strong_password';

-- 刷新权限
FLUSH PRIVILEGES;

-- 退出
EXIT;
```

之后就可以使用密码登录：
```bash
mysql -u root -p
```

#### 4. 删除匿名用户

**问题**：`Remove anonymous users?`

**推荐**：输入 `Y`
- 匿名用户是不需要密码就能登录的用户，存在安全风险
- 生产环境必须删除

#### 5. 禁止 root 远程登录

**问题**：`Disallow root login remotely?`

**推荐**：输入 `Y`
- 禁止 root 用户从远程登录，提高安全性
- 如果需要远程管理，可以创建专门的用户

#### 6. 删除测试数据库

**问题**：`Remove test database and access to it?`

**推荐**：输入 `Y`
- 删除默认的测试数据库，减少攻击面

#### 7. 重新加载权限表

**问题**：`Reload privilege tables now?`

**推荐**：输入 `Y`
- 使所有更改立即生效

## 📝 完整配置示例（生产环境）

```
Press y|Y for Yes, any other key for No: Y          # 启用密码验证
Please enter 0 = LOW, 1 = MEDIUM and 2 = STRONG: 1   # 选择 MEDIUM 级别
New password: MyP@ssw0rd123                          # 输入强密码
Re-enter new password: MyP@ssw0rd123                # 确认密码
Remove anonymous users? Y
Disallow root login remotely? Y
Remove test database and access to it? Y
Reload privilege tables now? Y
```

## 📝 完整配置示例（开发环境）

```
Press y|Y for Yes, any other key for No: N          # 不启用密码验证
New password: root123                                # 输入简单密码（开发用）
Re-enter new password: root123                      # 确认密码
Remove anonymous users? Y
Disallow root login remotely? N                     # 允许远程登录（开发方便）
Remove test database and access to it? Y
Reload privilege tables now? Y
```

## 🔧 创建数据库和用户

配置完成后，创建项目所需的数据库和用户：

```bash
# 登录 MySQL（使用刚才设置的 root 密码）
sudo mysql -u root -p
```

在 MySQL 中执行：

```sql
-- 创建数据库
CREATE DATABASE tibet_tourism CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户（替换 'your_strong_password' 为实际密码）
CREATE USER 'tibet_user'@'localhost' IDENTIFIED BY 'your_strong_password';

-- 授予权限
GRANT ALL PRIVILEGES ON tibet_tourism.* TO 'tibet_user'@'localhost';

-- 刷新权限
FLUSH PRIVILEGES;

-- 查看用户
SELECT user, host FROM mysql.user;

-- 退出
EXIT;
```

## 🔍 验证配置

```bash
# 测试 root 登录
sudo mysql -u root -p

# 测试应用用户登录
mysql -u tibet_user -p tibet_tourism

# 如果登录成功，说明配置正确
```

## ⚠️ 常见问题

### 1. 密码不符合要求

**问题**：如果启用了密码验证，但密码不符合要求，会提示：
```
ERROR 1819 (HY000): Your password does not satisfy the current policy requirements
```

**解决**：
- 使用更复杂的密码（包含数字、大小写字母、特殊字符）
- 或者重新运行 `mysql_secure_installation`，选择不启用密码验证

### 2. 忘记 root 密码

**解决**：
```bash
# 停止 MySQL 服务
sudo systemctl stop mysql

# 以安全模式启动 MySQL（跳过权限检查）
sudo mysqld_safe --skip-grant-tables &

# 登录 MySQL（无需密码）
mysql -u root

# 在 MySQL 中重置密码
ALTER USER 'root'@'localhost' IDENTIFIED BY 'new_password';
FLUSH PRIVILEGES;
EXIT;

# 重启 MySQL 服务
sudo systemctl restart mysql
```

### 3. 无法远程连接

**问题**：如果禁止了 root 远程登录，但需要远程管理

**解决**：创建专门的远程管理用户
```sql
CREATE USER 'admin'@'%' IDENTIFIED BY 'strong_password';
GRANT ALL PRIVILEGES ON *.* TO 'admin'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
```

## 🔐 安全建议

1. **使用强密码**：至少 12 个字符，包含大小写字母、数字和特殊字符
2. **定期更换密码**：建议每 3-6 个月更换一次
3. **限制远程访问**：只允许必要的 IP 地址访问
4. **使用专用用户**：不要使用 root 用户运行应用程序
5. **启用 SSL**：生产环境建议启用 MySQL SSL 连接

## 📚 相关文档

- [MySQL 官方文档](https://dev.mysql.com/doc/)
- [MySQL 安全指南](https://dev.mysql.com/doc/refman/8.0/en/security.html)

