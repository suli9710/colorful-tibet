# Git 配置指南

## 1. 基本配置（必需）

### 配置用户名和邮箱
这些信息会出现在你的每次提交中，是Git必需的配置。

```bash
# 配置全局用户名（所有仓库使用）
git config --global user.name "你的姓名"

# 配置全局邮箱（所有仓库使用）
git config --global user.email "your.email@example.com"

# 如果只想为当前项目配置（不使用--global）
git config user.name "你的姓名"
git config user.email "your.email@example.com"
```

### 验证配置
```bash
# 查看所有配置
git config --list

# 查看特定配置
git config user.name
git config user.email
```

## 2. 常用配置（推荐）

### 配置默认编辑器
```bash
# 使用VS Code作为默认编辑器
git config --global core.editor "code --wait"

# 使用记事本（Windows）
git config --global core.editor "notepad"

# 使用Vim
git config --global core.editor "vim"
```

### 配置默认分支名
```bash
# 设置默认分支为main（现代Git推荐）
git config --global init.defaultBranch main
```

### 配置行尾处理（Windows）
```bash
# 自动转换行尾（推荐Windows用户）
git config --global core.autocrlf true

# 或者保持原样（推荐Linux/Mac用户）
git config --global core.autocrlf input
```

### 配置颜色输出
```bash
# 启用彩色输出（默认已启用）
git config --global color.ui auto
```

### 配置别名（快捷命令）
```bash
# 常用别名
git config --global alias.st status
git config --global alias.co checkout
git config --global alias.br branch
git config --global alias.ci commit
git config --global alias.unstage 'reset HEAD --'
git config --global alias.last 'log -1 HEAD'
git config --global alias.visual '!gitk'
```

## 3. 高级配置

### 配置SSH密钥（用于GitHub/GitLab等）
```bash
# 生成SSH密钥
ssh-keygen -t ed25519 -C "your.email@example.com"

# 查看公钥（复制到GitHub/GitLab）
cat ~/.ssh/id_ed25519.pub
```

### 配置凭证存储
```bash
# Windows使用Windows凭证管理器（默认）
git config --global credential.helper manager

# 或者使用缓存（15分钟）
git config --global credential.helper cache
```

### 配置推送行为
```bash
# 设置默认推送行为（推荐）
git config --global push.default simple

# 或者使用upstream
git config --global push.default upstream
```

## 4. 项目初始化（如果还没有初始化）

```bash
# 初始化Git仓库
git init

# 添加远程仓库（可选）
git remote add origin https://github.com/username/repository.git

# 或者使用SSH
git remote add origin git@github.com:username/repository.git
```

## 5. 常用Git操作

### 首次提交
```bash
# 添加所有文件
git add .

# 提交
git commit -m "Initial commit"

# 推送到远程（如果已配置）
git push -u origin main
```

### 日常操作
```bash
# 查看状态
git status

# 查看差异
git diff

# 添加文件
git add <文件名>
git add .  # 添加所有更改

# 提交
git commit -m "提交信息"

# 查看提交历史
git log
git log --oneline  # 简洁模式

# 推送到远程
git push

# 从远程拉取
git pull
```

## 6. 检查当前配置

```bash
# 查看所有配置（包括来源）
git config --list --show-origin

# 查看全局配置
git config --global --list

# 查看本地配置
git config --local --list
```

## 7. 修改配置

```bash
# 修改配置
git config --global user.name "新姓名"

# 删除配置
git config --global --unset user.name
```

## 8. 配置文件位置

- **全局配置**: `~/.gitconfig` 或 `C:\Users\你的用户名\.gitconfig`
- **本地配置**: `.git/config`（项目目录下）

## 快速开始

如果你只是想快速配置并开始使用，执行以下命令：

```bash
# 1. 配置基本信息
git config --global user.name "你的姓名"
git config --global user.email "your.email@example.com"

# 2. 验证配置
git config --list

# 3. 如果项目还没有初始化，执行：
git init

# 4. 添加文件并提交
git add .
git commit -m "Initial commit"
```

## 注意事项

1. **用户名和邮箱**：这些信息会公开显示在提交历史中，请使用合适的名称
2. **`.gitignore`**：已为你创建，会忽略不需要版本控制的文件（如`node_modules`、`target`等）
3. **提交信息**：使用清晰、有意义的提交信息，便于后续追踪
4. **分支管理**：建议使用`main`作为主分支名（现代标准）

## 需要帮助？

- Git官方文档：https://git-scm.com/doc
- GitHub帮助：https://docs.github.com
- Git教程：https://git-scm.com/book

