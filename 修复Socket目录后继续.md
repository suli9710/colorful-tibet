# 修复 Socket 目录问题后继续

## 🔍 当前问题

错误：`Directory '/var/run/mysqld' for UNIX socket file don't exists.`

这说明 MySQL socket 目录不存在，需要先创建它。

## ✅ 解决步骤

### 步骤1：创建 socket 目录

在**当前终端**（看到 `ubuntu@VM-16-10-ubuntu:~$` 提示符的地方）执行：

```bash
# 创建目录
sudo mkdir -p /var/run/mysqld

# 设置权限
sudo chown mysql:mysql /var/run/mysqld
sudo chmod 755 /var/run/mysqld
```

### 步骤2：停止所有 MySQL 进程

```bash
# 停止所有MySQL进程
sudo pkill mysqld 2>/dev/null
sudo pkill mysqld_safe 2>/dev/null
sudo systemctl stop mysql 2>/dev/null
```

### 步骤3：重新启动安全模式

```bash
# 启动安全模式
sudo mysqld_safe --skip-grant-tables --skip-networking &

# 等待5秒
sleep 5
```

### 步骤4：检查是否启动成功

```bash
# 检查MySQL进程
ps aux | grep mysqld | grep -v grep
```

如果看到 `mysqld` 进程在运行，说明启动成功。

### 步骤5：进入 MySQL 客户端

```bash
mysql -u root
```

**重要**：应该看到 `mysql>` 提示符，说明成功进入 MySQL！

### 步骤6：在 MySQL 中执行命令

在 `mysql>` 提示符下执行（复制粘贴）：

```sql
USE mysql;
CREATE DATABASE IF NOT EXISTS tibet_tourism CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'tibet_user'@'localhost' IDENTIFIED BY 'Tibet2024!Tourism';
GRANT ALL PRIVILEGES ON tibet_tourism.* TO 'tibet_user'@'localhost';
GRANT ALL PRIVILEGES ON *.* TO 'tibet_user'@'localhost' WITH GRANT OPTION;
FLUSH PRIVILEGES;
EXIT;
```

### 步骤7：重启 MySQL

执行 `EXIT;` 后，回到 shell，执行：

```bash
# 停止安全模式
sudo pkill mysqld
sudo pkill mysqld_safe

# 正常启动
sudo systemctl start mysql
sleep 5
```

### 步骤8：测试连接

```bash
mysql -u tibet_user -p tibet_tourism
# 输入密码：Tibet2024!Tourism
```

## 🎯 完整命令序列（按顺序执行）

```bash
# === 1. 创建socket目录 ===
sudo mkdir -p /var/run/mysqld
sudo chown mysql:mysql /var/run/mysqld
sudo chmod 755 /var/run/mysqld

# === 2. 停止所有MySQL进程 ===
sudo pkill mysqld 2>/dev/null
sudo pkill mysqld_safe 2>/dev/null
sudo systemctl stop mysql 2>/dev/null

# === 3. 启动安全模式 ===
sudo mysqld_safe --skip-grant-tables --skip-networking &
sleep 5

# === 4. 检查是否启动成功 ===
ps aux | grep mysqld | grep -v grep

# === 5. 进入MySQL客户端 ===
mysql -u root

# === 6. 在MySQL中执行（看到 mysql> 后执行）===
# 复制以下所有内容，在 mysql> 提示符下粘贴
USE mysql;
CREATE DATABASE IF NOT EXISTS tibet_tourism CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'tibet_user'@'localhost' IDENTIFIED BY 'Tibet2024!Tourism';
GRANT ALL PRIVILEGES ON tibet_tourism.* TO 'tibet_user'@'localhost';
GRANT ALL PRIVILEGES ON *.* TO 'tibet_user'@'localhost' WITH GRANT OPTION;
FLUSH PRIVILEGES;
EXIT;

# === 7. 重启MySQL（回到shell后执行）===
sudo pkill mysqld
sudo pkill mysqld_safe
sudo systemctl start mysql
sleep 5

# === 8. 测试连接 ===
mysql -u tibet_user -p tibet_tourism
# 输入密码：Tibet2024!Tourism
```

## 🔍 如何判断当前状态？

**在 Shell 中（执行系统命令）：**
```
ubuntu@VM-16-10-ubuntu:~$ 
```

**在 MySQL 中（执行SQL命令）：**
```
mysql> 
```

## ⚠️ 重要提示

1. **先创建 socket 目录**，否则 MySQL 无法启动
2. **看到 `mysql>` 提示符**才能执行 SQL 命令
3. **每行 SQL 命令后面要加分号 `;`**
4. **执行 `EXIT;` 退出 MySQL**

---

**现在先执行步骤1-3创建目录并启动MySQL，然后再进入MySQL客户端执行SQL命令！**


