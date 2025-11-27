# 修复 MySQL 并重置 root 密码

## 🔍 当前问题

1. Socket 目录不存在
2. MySQL 服务未运行
3. SQL 命令在 shell 中执行（错误）

## ✅ 解决步骤

### 步骤1：创建 socket 目录并启动 MySQL

在 shell 中执行（`ubuntu@VM-16-10-ubuntu:/opt/colorful-tibet$`）：

```bash
# 创建socket目录
sudo mkdir -p /var/run/mysqld
sudo chown mysql:mysql /var/run/mysqld
sudo chmod 755 /var/run/mysqld

# 停止所有MySQL进程
sudo pkill mysqld 2>/dev/null
sudo pkill mysqld_safe 2>/dev/null
sudo systemctl stop mysql 2>/dev/null

# 启动安全模式
sudo mysqld_safe --skip-grant-tables --skip-networking > /dev/null 2>&1 &

# 等待MySQL启动
sleep 10

# 检查MySQL是否运行
ps aux | grep mysqld | grep -v grep
```

### 步骤2：进入 MySQL 客户端

```bash
mysql -u root
```

**重要**：必须看到 `mysql>` 提示符！

### 步骤3：在 MySQL 中重置 root 密码

在 `mysql>` 提示符下执行（**不是**在 shell 中）：

```sql
USE mysql;
ALTER USER 'root'@'localhost' IDENTIFIED BY '1zh031224';
FLUSH PRIVILEGES;
EXIT;
```

### 步骤4：重启 MySQL

执行 `EXIT;` 后，回到 shell：

```bash
# 停止安全模式
sudo pkill mysqld
sudo pkill mysqld_safe
sleep 3

# 正常启动
sudo systemctl start mysql
sleep 5
```

### 步骤5：测试 root 登录

```bash
mysql -u root -p
```

输入密码：`1zh031224`

## 🎯 完整命令序列

**在 shell 中执行：**

```bash
# === 1. 创建socket目录 ===
sudo mkdir -p /var/run/mysqld
sudo chown mysql:mysql /var/run/mysqld
sudo chmod 755 /var/run/mysqld

# === 2. 停止并启动安全模式 ===
sudo pkill mysqld 2>/dev/null
sudo pkill mysqld_safe 2>/dev/null
sudo systemctl stop mysql 2>/dev/null
sudo mysqld_safe --skip-grant-tables --skip-networking > /dev/null 2>&1 &
sleep 10

# === 3. 进入MySQL ===
mysql -u root
```

**在 `mysql>` 提示符下执行：**

```sql
USE mysql;
ALTER USER 'root'@'localhost' IDENTIFIED BY '1zh031224';
FLUSH PRIVILEGES;
EXIT;
```

**回到 shell 后执行：**

```bash
# === 4. 重启MySQL ===
sudo pkill mysqld
sudo pkill mysqld_safe
sleep 3
sudo systemctl start mysql
sleep 5

# === 5. 测试登录 ===
mysql -u root -p
# 输入密码：1zh031224
```

## ⚠️ 重要提示

1. **SQL 命令必须在 `mysql>` 提示符下执行**
2. **不能在 shell 中（`ubuntu@VM-16-10-ubuntu:~$`）执行 SQL 命令**
3. **必须先进入 MySQL 客户端：`mysql -u root`**
4. **看到 `mysql>` 提示符才能执行 SQL 命令**

## 🔍 如何判断

**在 Shell 中（执行系统命令）：**
```
ubuntu@VM-16-10-ubuntu:/opt/colorful-tibet$ 
```

**在 MySQL 中（执行SQL命令）：**
```
mysql> 
```

---

**现在先执行步骤1-2，进入MySQL后，再执行步骤3的SQL命令！**

