# package.json JSON 格式错误修复

## ❌ 错误：EJSONPARSE

### 问题描述

执行 `docker compose build` 时出现：
```
npm error code EJSONPARSE
npm error path /app/package.json
npm error JSON.parse Unexpected string in JSON at position 849
npm error JSON.parse Note: package.json must be actual JSON, not just JavaScript.
```

### 原因

- JSON 格式错误（缺少逗号、引号等）
- 使用 sed 命令修改时格式不正确
- 文件中有语法错误

### 解决方案

#### 方案一：手动编辑修复（推荐）

```bash
# 进入前端目录
cd /opt/colorful-tibet/frontend

# 编辑 package.json
nano package.json
```

**确保格式正确**：
- 每个属性名都用双引号
- 每个属性值都用双引号（字符串）
- 最后一个属性后不要有逗号
- 确保所有括号和花括号匹配

**正确的 devDependencies 格式**：
```json
"devDependencies": {
    "@types/leaflet": "^1.9.21",
    "@types/node": "^20.11.24",
    "@vitejs/plugin-vue": "^5.0.4",
    "autoprefixer": "^10.4.18",
    "postcss": "^8.4.35",
    "tailwindcss": "^3.4.1",
    "terser": "^5.36.0",
    "typescript": "^5.2.2",
    "vite": "^5.1.4",
    "vue-tsc": "^1.8.27"
}
```

**正确的 build 脚本**：
```json
"build": "vite build",
```

#### 方案二：验证 JSON 格式

修改后验证 JSON 格式：

```bash
# 使用 Python 验证 JSON
python3 -m json.tool package.json > /dev/null && echo "JSON 格式正确" || echo "JSON 格式错误"

# 或者使用 node
node -e "JSON.parse(require('fs').readFileSync('package.json'))" && echo "JSON 格式正确" || echo "JSON 格式错误"
```

#### 方案三：从正确的文件恢复

如果修改出错，可以从 Git 恢复：

```bash
cd /opt/colorful-tibet/frontend

# 查看修改
git diff package.json

# 恢复文件
git checkout package.json

# 然后重新手动修改
nano package.json
```

## 🔧 完整的正确 package.json 示例

```json
{
    "name": "colorful-tibet-frontend",
    "private": true,
    "version": "0.0.0",
    "type": "module",
    "scripts": {
        "dev": "vite",
        "build": "vite build",
        "preview": "vite preview"
    },
    "dependencies": {
        "axios": "^1.6.7",
        "echarts": "^5.5.0",
        "leaflet": "^1.9.4",
        "lucide-vue-next": "^0.344.0",
        "marked": "^17.0.1",
        "pinia": "^2.1.7",
        "vue": "^3.4.21",
        "vue-i18n": "^9.14.5",
        "vue-router": "^4.3.0"
    },
    "devDependencies": {
        "@types/leaflet": "^1.9.21",
        "@types/node": "^20.11.24",
        "@vitejs/plugin-vue": "^5.0.4",
        "autoprefixer": "^10.4.18",
        "postcss": "^8.4.35",
        "tailwindcss": "^3.4.1",
        "terser": "^5.36.0",
        "typescript": "^5.2.2",
        "vite": "^5.1.4",
        "vue-tsc": "^1.8.27"
    }
}
```

## 🚀 快速修复步骤

```bash
# 1. 进入前端目录
cd /opt/colorful-tibet/frontend

# 2. 验证当前 JSON 格式
python3 -m json.tool package.json > /dev/null && echo "格式正确" || echo "格式错误，需要修复"

# 3. 如果格式错误，编辑文件
nano package.json

# 4. 修复后再次验证
python3 -m json.tool package.json > /dev/null && echo "格式正确" || echo "仍有错误"

# 5. 如果格式正确，返回项目根目录并重新构建
cd /opt/colorful-tibet
docker compose build --no-cache frontend
```

## ⚠️ 常见 JSON 错误

1. **缺少逗号**：每个属性（除了最后一个）后必须有逗号
2. **多余的逗号**：最后一个属性后不能有逗号
3. **引号错误**：必须使用双引号，不能使用单引号
4. **注释**：JSON 不支持注释（`//` 或 `/* */`）

## 📝 检查清单

修改 `package.json` 后，确保：
- ✅ 所有属性名都用双引号
- ✅ 所有字符串值都用双引号
- ✅ 每个属性后都有逗号（最后一个除外）
- ✅ 所有括号和花括号匹配
- ✅ 没有注释
- ✅ JSON 格式验证通过

