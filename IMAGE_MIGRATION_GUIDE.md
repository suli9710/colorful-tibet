# 图片迁移指南

## 已完成的工作

### 1. 后端配置
- ✅ 配置了静态资源访问路径 `/images/**`
- ✅ 创建了统一的图片目录结构：
  - `images/spots/` - 景点图片
  - `images/heritage/` - 非遗项目图片
  - `images/news/` - 新闻图片
  - `images/common/` - 通用图片（背景、占位符等）

### 2. 前端更新
- ✅ 替换了 `Home.vue` 中的外部图片URL
- ✅ 替换了 `Login.vue` 中的外部图片URL
- ✅ 替换了 `Register.vue` 中的外部图片URL
- ✅ 替换了 `News.vue` 中的外部图片URL
- ✅ 替换了 `Heritage.vue` 中的外部图片URL

### 3. 代理配置
- ✅ Vite 已配置 `/images` 代理到后端

## 图片访问路径

### 后端访问
```
http://localhost:8080/images/spots/布达拉宫.jpg
http://localhost:8080/images/common/potala-palace-hero.jpg
```

### 前端访问
```html
<!-- 使用相对路径，会自动代理到后端 -->
<img src="/images/spots/布达拉宫.jpg" alt="布达拉宫">
<img src="/images/common/potala-palace-hero.jpg" alt="背景">
```

## 图片目录结构

```
backend/src/main/resources/static/images/
├── spots/              # 景点图片（已有）
│   ├── 布达拉宫.jpg
│   ├── 纳木错.jpg
│   └── ...
├── heritage/           # 非遗项目图片
│   ├── default-heritage.jpg
│   └── ...
├── news/               # 新闻图片
│   ├── default-news.jpg
│   └── ...
└── common/             # 通用图片
    ├── potala-palace-hero.jpg
    ├── potala-palace-bg.jpg
    └── tibet-landscape-bg.jpg
```

## 替换的图片URL

| 原URL | 新路径 | 用途 |
|-------|--------|------|
| `https://images.unsplash.com/photo-1545569341-9eb8b30979d9...` | `/images/common/potala-palace-hero.jpg` | 首页背景 |
| `https://images.unsplash.com/photo-1545569341-9eb8b30979d9...` | `/images/common/potala-palace-bg.jpg` | 登录页背景 |
| `https://images.unsplash.com/photo-1558981001-5864b3250a69...` | `/images/common/tibet-landscape-bg.jpg` | 注册页背景 |
| `https://images.unsplash.com/photo-1544735716-392fe2489ffa...` | `/images/news/default-news.jpg` | 新闻默认图片 |
| `https://images.unsplash.com/photo-1587280501635-68f3192d9494...` | `/images/heritage/default-heritage.jpg` | 非遗默认图片 |

## 添加新图片

### 步骤1：将图片放到对应目录
```bash
# 景点图片
backend/src/main/resources/static/images/spots/新景点.jpg

# 新闻图片
backend/src/main/resources/static/images/news/新闻标题.jpg

# 通用图片
backend/src/main/resources/static/images/common/图片名称.jpg
```

### 步骤2：在代码中使用
```vue
<!-- Vue组件中 -->
<img src="/images/spots/新景点.jpg" alt="新景点">
```

```java
// Java代码中
spot.setImageUrl("/images/spots/新景点.jpg");
```

## 注意事项

1. **图片格式**：支持 JPG、PNG、JPEG
2. **文件大小**：建议不超过 2MB
3. **文件命名**：
   - 使用中文或英文+连字符
   - 避免特殊字符
   - 保持文件名简洁
4. **路径使用**：
   - 前端：使用 `/images/...` 相对路径
   - 后端：使用 `/images/...` 路径（Spring会自动处理）
5. **错误处理**：已添加 `onerror` 属性，图片加载失败时会显示备用图片

## 验证图片访问

### 测试后端访问
```bash
# 在浏览器中访问
http://localhost:8080/images/spots/布达拉宫.jpg
```

### 测试前端访问
```bash
# 启动前端后访问
http://localhost:5173/images/spots/布达拉宫.jpg
```

## 后续优化建议

1. **图片压缩**：使用工具压缩图片，减少加载时间
2. **图片CDN**：如果图片较多，可以考虑使用CDN
3. **懒加载**：对列表中的图片实现懒加载
4. **响应式图片**：根据设备尺寸加载不同大小的图片
5. **WebP格式**：考虑使用WebP格式以获得更好的压缩比

## 故障排查

### 问题1：图片无法显示
**检查项**：
1. 图片文件是否存在
2. 路径是否正确（注意大小写）
3. 后端是否启动
4. 浏览器控制台是否有404错误

### 问题2：前端代理不工作
**检查项**：
1. `vite.config.ts` 中的代理配置是否正确
2. 前端开发服务器是否重启
3. 路径是否以 `/images` 开头

### 问题3：后端无法访问图片
**检查项**：
1. `WebMvcConfig.java` 中的资源处理器配置
2. 图片是否在 `src/main/resources/static/images/` 目录
3. 后端是否重新编译

## 总结

✅ 所有外部图片URL已替换为本地路径
✅ 统一的图片目录结构已创建
✅ 静态资源访问已配置
✅ 前端代理已配置

现在所有图片都存储在项目内部，不再依赖外部URL。







