# 前端第三方脚本 CSP 配置

本文记录前端动态加载 AMap 与 reCAPTCHA 时需要的 CSP、nonce 与可选 SRI 配置。相关代码入口：

- `frontend/src/utils/amap.ts` 动态加载 `https://webapi.amap.com/maps?v=2.0&key=...`
- `frontend/src/utils/recaptcha.ts` 动态加载 `https://www.recaptcha.net/recaptcha/api.js?render=...`

## 推荐 CSP

生产环境至少保留以下脚本策略来源：

```text
script-src 'self' 'nonce-{per-response-random-nonce}' https://webapi.amap.com https://*.amap.com https://*.autonavi.com https://www.recaptcha.net https://www.google.com https://www.gstatic.com https://www.gstatic.cn;
script-src-attr 'none';
object-src 'none';
base-uri 'self';
frame-ancestors 'self';
```

AMap 与 reCAPTCHA 运行时还会使用样式、图片、连接或 iframe。若启用这两个能力，现有 nginx 配置中的 `style-src`、`img-src`、`connect-src` 和 `frame-src` allowlist 也需要同步保留对应域名。

## Nonce 注入

严格 CSP 下，后端或边缘代理应为每个 HTML 响应生成随机 nonce，并同时写入 CSP header 与页面元数据：

```html
<meta name="csp-nonce" content="{per-response-random-nonce}">
```

前端加载器会按以下优先级读取 nonce：

1. `<meta name="csp-nonce" content="...">`
2. `window.__CSP_NONCE__`
3. `VITE_CSP_NONCE`

生产环境优先使用每个响应不同的 meta 或 runtime global。`VITE_CSP_NONCE` 只适合测试、静态预览或其他明确接受固定 nonce 的环境。

## 可选 SRI

两个第三方脚本都是带参数的远端脚本，提供方可能随版本、区域或配置更新响应内容。因此 SRI 不默认启用。若部署方固定了精确脚本响应并能接受提供方更新时需要同步 hash，可以配置：

```html
<meta name="amap-script-integrity" content="sha384-...">
<meta name="recaptcha-script-integrity" content="sha384-...">
<meta name="amap-script-crossorigin" content="anonymous">
<meta name="recaptcha-script-crossorigin" content="anonymous">
```

也可以在构建环境使用：

```text
VITE_AMAP_SCRIPT_INTEGRITY=sha384-...
VITE_AMAP_SCRIPT_CROSSORIGIN=anonymous
VITE_RECAPTCHA_SCRIPT_INTEGRITY=sha384-...
VITE_RECAPTCHA_SCRIPT_CROSSORIGIN=anonymous
```

当配置了 `integrity` 但没有显式配置 `crossorigin` 时，前端默认使用 `anonymous`。
