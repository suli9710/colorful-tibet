# 牦牦 Live2D 风格浏览器包

这是一个浏览器可直接运行的 Live2D-like 文件包，不依赖 Cubism 专有 `.moc3` 导出。

## 文件

- `index.html`：浏览器预览页
- `model/yakmo.live2d.json`：运行时模型、动作和参数
- `model/yakmo.model3.json`：兼容命名的说明性入口
- `runtime/yakmo-live2d.js`：Canvas 运行时
- `runtime/style.css`：预览页样式
- `assets/parts/`：基础图层
- `assets/frames/`：补帧后的动作序列
- `assets/yakmo-live2d-contact-sheet.jpg`：动作帧检查图

## 本地预览

在本目录运行：

```powershell
python -m http.server 4177
```

然后打开：

```text
http://127.0.0.1:4177/
```

## 嵌入

```html
<canvas id="yakmo"></canvas>
<script src="runtime/yakmo-live2d.js"></script>
<script>
  const pet = new YakmoLive2D(document.querySelector("#yakmo"));
  pet.load("model/yakmo.live2d.json").then(() => pet.start());
</script>
```

可用动作：`idle`、`running-right`、`running-left`、`waving`、`jumping`、`failed`、`waiting`、`running`、`review`。
