# 正确执行 MySQL 命令的步骤

## ❌ 常见错误

**错误做法**：直接在 shell 中执行 SQL 命令
```bash
ubuntu@VM-16-10-ubuntu:~$ CREATE DATABASE ...  # ❌ 错误！这是shell，不是MySQL
```

**正确做法**：先进入 MySQL 客户端，再执行 SQL 命令

## ✅ 正确步骤

### 步骤1：启动 MySQL 安全模式

```bash
# 停止MySQL
sudo systemctl stop mysql

# 安全模式启动
sudo mysqld_safe --skip-grant-tables --skip-networking &

# 等待5秒
sleep 5
```

### 步骤2：进入 MySQL 客户端

**重要**：必须执行这个命令进入 MySQL！

```bash
mysql -u root
```

**看到这个提示才说明进入了MySQL：**
```
Welcome to the MySQL monitor.  Commands end with ; or \g.
Your MySQL connection id is X
Server version: X.X.X

mysql>
```

注意：提示符变成了 `mysql>`，不是 `ubuntu@VM-16-10-ubuntu:~$`

### 步骤3：在 MySQL 客户端中执行 SQL 命令

**现在**在 `mysql>` 提示符下执行（复制粘贴）：

```sql
USE mysql;
CREATE DATABASE IF NOT EXISTS tibet_tourism CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'tibet_user'@'localhost' IDENTIFIED BY 'Tibet2024!Tourism';
GRANT ALL PRIVILEGES ON tibet_tourism.* TO 'tibet_user'@'localhost';
GRANT ALL PRIVILEGES ON *.* TO 'tibet_user'@'localhost' WITH GRANT OPTION;
FLUSH PRIVILEGES;
EXIT;
```

**注意**：
- 每行命令后面要加分号 `;`
- 看到 `mysql>` 提示符说明在MySQL中
- 看到 `ubuntu@VM-16-10-ubuntu:~$` 说明在shell中（错误）

### 步骤4：退出 MySQL 并重启服务

执行 `EXIT;` 后，您会回到 shell 提示符 `ubuntu@VM-16-10-ubuntu:~$`

然后执行：

```bash
# 停止安全模式
sudo pkill mysqld
sudo pkill mysqld_safe

# 正常启动MySQL
sudo systemctl start mysql

# 等待几秒
sleep 5
```

### 步骤5：测试连接

```bash
# 测试新用户登录
mysql -u tibet_user -p tibet_tourism
# 输入密码：Tibet2024!Tourism
```

如果成功进入 MySQL，说明配置正确！

## 🎯 完整流程（复制执行）

```bash
# === 步骤1：启动安全模式 ===
sudo systemctl stop mysql
sudo mysqld_safe --skip-grant-tables --skip-networking &
sleep 5

# === 步骤2：进入MySQL客户端 ===
mysql -u root

# === 步骤3：在MySQL中执行（看到 mysql> 提示符后执行）===
# 复制以下所有内容，在 mysql> 提示符下粘贴执行
USE mysql;
CREATE DATABASE IF NOT EXISTS tibet_tourism CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'tibet_user'@'localhost' IDENTIFIED BY 'Tibet2024!Tourism';
GRANT ALL PRIVILEGES ON tibet_tourism.* TO 'tibet_user'@'localhost';
GRANT ALL PRIVILEGES ON *.* TO 'tibet_user'@'localhost' WITH GRANT OPTION;
FLUSH PRIVILEGES;
EXIT;

# === 步骤4：重启MySQL（回到shell后执行）===
sudo pkill mysqld
sudo pkill mysqld_safe
sudo systemctl start mysql
sleep 5

# === 步骤5：测试连接 ===
mysql -u tibet_user -p tibet_tourism
# 输入密码：Tibet2024!Tourism
```

## 🔍 如何判断是否在 MySQL 中？

**在 MySQL 中：**
```
mysql> 
```

**在 Shell 中：**
```
ubuntu@VM-16-10-ubuntu:~$ 
```

## ⚠️ 重要提示

1. **必须先执行 `mysql -u root` 进入MySQL**
2. **看到 `mysql>` 提示符才能执行SQL命令**
3. **每行SQL命令后面要加分号 `;`**
4. **执行 `EXIT;` 退出MySQL，回到shell**

---

**关键**：SQL命令必须在 `mysql>` 提示符下执行，不能在 `ubuntu@VM-16-10-ubuntu:~$` 下执行！


