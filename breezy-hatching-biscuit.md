# Colorful Tibet 项目审查报告

更新时间：2026-05-20

## 结论

原审查报告的方向是对的。此前能直接修复的安全、部署、异常语义、分页保护、环境变量、404、健康检查、限流和构建清理等问题已经处理过。本轮又继续推进了三个原本标为“建议单独规划”的事项：JWT 前端状态收口、`RoutePlanner.vue` 拆分，以及隐私政策/服务条款内容源迁移。

当前仍建议把这份文件视为“历史审查与整改记录”，后续只保留少量真正需要专项投入的优化项。

## 本轮新增修改

### 1. JWT / Cookie 认证兼容路径清理

状态：已完成前端 Cookie-only 收口，后端 Bearer 兼容暂时保留。

本轮删除了前端 auth store 中的 JWT 解析、过期判断、`cookie-session` 哨兵 token 和 token 状态。前端登录态现在只通过 `/auth/me`、httpOnly Cookie 和 Pinia 用户状态确认；`localStorage.user` 只作为无 token 的展示缓存，不再作为页面鉴权依据。

已同步调整这些页面，避免直接读取 `localStorage.user` 判断登录：

- `CreateRoute.vue`
- `Home.vue`
- `RouteDetail.vue`
- `QuestionDetail.vue`
- `ScenicSpotDetail.vue`

后端 `AuthController`、`AuthTokenFilter`、`JwtAuthSupport` 仍保留 `Authorization: Bearer ...` 兼容能力。这对外部 API/旧客户端更稳妥；如果未来要严格 Cookie-only，可以再单独做一次后端兼容路径移除和 API 文档更新。

### 2. `RoutePlanner.vue` 继续拆分

状态：已完成第一步低风险拆分。

本轮把路线规划草稿持久化逻辑抽到独立 composable：

```text
frontend/src/composables/useRoutePlannerDraft.ts
```

`RoutePlanner.vue` 不再直接维护草稿版本、表单归一化、localStorage 读写和恢复逻辑。当前仍建议后续继续拆分高耦合部分：

- `useRouteGeneration`
- `useStreamParser`
- `useBookableItinerary`
- 路线表单、结果展示、旅行锦囊、分享/预订等子组件

### 3. 隐私政策/服务条款 CMS 化

状态：已迁移为独立 Markdown 内容源。

本轮已将隐私政策和服务条款从 i18n 结构化对象渲染改为 Markdown 注入：

```text
frontend/src/content/legal/privacy.zh.md
frontend/src/content/legal/privacy.bo.md
frontend/src/content/legal/terms.zh.md
frontend/src/content/legal/terms.bo.md
frontend/src/content/legal/index.ts
```

`PrivacyPolicy.vue` 和 `TermsOfService.vue` 现在按当前语言读取 Markdown，并通过 `marked` + `DOMPurify` 渲染消毒。短期满足“内容独立维护”，中长期如果要运营后台发布，只需要把 `getLegalMarkdown()` 的来源替换为后端 API 或轻量 CMS。

## 仍建议后续专项处理

### 1. `LIKE '%keyword%'` 搜索效率

状态：仍是优化项。

当前数据量下可接受；如果搜索数据继续增长，建议改为全文索引、搜索服务或更可控的搜索策略。

### 2. `scrapler/Dockerfile` Python 基础镜像升级

状态：未在本轮处理。

`python:3.11-slim` 当前可运行，但中长期建议评估升级到更新 slim 镜像，并验证 `scrapling` 依赖兼容性。

### 3. 前端构建警告

状态：非阻塞。

构建仍提示 Browserslist/baseline 数据过旧，以及 `echarts` chunk 超过 500KB。建议后续更新浏览器数据，并继续拆分图表相关代码。

## 本轮复查结果

已执行并通过：

```text
frontend: npm run typecheck
frontend: npm run build
backend: mvn -q test
```

静态复扫结果：

- 前端页面未再直接用 `localStorage.user` 判断登录
- 前端 auth store 未再保留 JWT/token 解析和 token 登录态
- 隐私政策/服务条款已从页面内结构化 i18n 内容改为独立 Markdown 源
