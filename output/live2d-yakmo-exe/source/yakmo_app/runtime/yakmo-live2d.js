class YakmoLive2D {
  constructor(canvas, options = {}) {
    this.canvas = canvas;
    this.ctx = canvas.getContext("2d", { alpha: true });
    this.options = options;
    this.model = null;
    this.frames = new Map();
    this.parts = {};
    this.motionName = "idle";
    this.motionStartedAt = performance.now();
    this.lastTime = performance.now();
    this.pointer = { x: 0, y: 0, active: false };
    this.targetPointer = { x: 0, y: 0, active: false };
    this.pixelRatio = Math.min(window.devicePixelRatio || 1, 2);
    this.running = false;
    this.resizeObserver = null;
  }

  async load(modelUrl = "model/yakmo.live2d.json") {
    const response = await fetch(modelUrl);
    if (!response.ok) {
      throw new Error(`Unable to load model: ${response.status} ${response.statusText}`);
    }
    this.modelUrl = new URL(modelUrl, window.location.href);
    this.assetBaseUrl = new URL("../", this.modelUrl);
    this.model = await response.json();
    this.motionName = this.model.defaultMotion || "idle";

    await Promise.all([
      this.loadParts(),
      this.loadFrames(),
    ]);

    this.bindEvents();
    this.resize();
    return this;
  }

  async loadParts() {
    const partEntries = Object.entries(this.model.parts || {});
    await Promise.all(partEntries.map(async ([name, path]) => {
      this.parts[name] = await this.loadImage(path);
    }));
  }

  async loadFrames() {
    const motions = Object.entries(this.model.motions || {});
    await Promise.all(motions.map(async ([name, motion]) => {
      const images = await Promise.all(motion.frames.map((path) => this.loadImage(path)));
      this.frames.set(name, images);
    }));
  }

  loadImage(path) {
    return new Promise((resolve, reject) => {
      const image = new Image();
      image.onload = () => resolve(image);
      image.onerror = () => reject(new Error(`Unable to load image: ${path}`));
      image.src = new URL(path, this.assetBaseUrl).toString();
    });
  }

  bindEvents() {
    const updatePointer = (event) => {
      const rect = this.canvas.getBoundingClientRect();
      const x = ((event.clientX - rect.left) / rect.width - 0.5) * 2;
      const y = ((event.clientY - rect.top) / rect.height - 0.5) * 2;
      this.targetPointer = {
        x: Math.max(-1, Math.min(1, x)),
        y: Math.max(-1, Math.min(1, y)),
        active: true,
      };
    };

    this.canvas.addEventListener("pointermove", updatePointer);
    this.canvas.addEventListener("pointerenter", updatePointer);
    this.canvas.addEventListener("pointerleave", () => {
      this.targetPointer = { x: 0, y: 0, active: false };
    });

    window.addEventListener("resize", () => this.resize());
    this.resizeObserver = new ResizeObserver(() => this.resize());
    this.resizeObserver.observe(this.canvas);
  }

  resize() {
    const rect = this.canvas.getBoundingClientRect();
    const width = Math.max(1, Math.floor(rect.width * this.pixelRatio));
    const height = Math.max(1, Math.floor(rect.height * this.pixelRatio));
    if (this.canvas.width !== width || this.canvas.height !== height) {
      this.canvas.width = width;
      this.canvas.height = height;
    }
  }

  setMotion(name) {
    if (!this.model?.motions?.[name] || this.motionName === name) return;
    this.motionName = name;
    this.motionStartedAt = performance.now();
  }

  start() {
    if (this.running) return;
    this.running = true;
    requestAnimationFrame((time) => this.tick(time));
  }

  stop() {
    this.running = false;
  }

  tick(time) {
    if (!this.running) return;
    const dt = Math.min(48, time - this.lastTime) / 1000;
    this.lastTime = time;
    this.update(dt);
    this.draw(time);
    requestAnimationFrame((next) => this.tick(next));
  }

  update(dt) {
    const follow = 1 - Math.pow(0.05, dt * 8);
    this.pointer.x += (this.targetPointer.x - this.pointer.x) * follow;
    this.pointer.y += (this.targetPointer.y - this.pointer.y) * follow;
    this.pointer.active = this.targetPointer.active;
  }

  currentFrame(time) {
    const motion = this.model.motions[this.motionName];
    const images = this.frames.get(this.motionName);
    if (!motion || !images?.length) return null;
    const fps = motion.fps || 18;
    const elapsed = (time - this.motionStartedAt) / 1000;
    const index = Math.floor(elapsed * fps) % images.length;
    const nextIndex = (index + 1) % images.length;
    const mix = (elapsed * fps) % 1;
    return { image: images[index], next: images[nextIndex], mix, motion };
  }

  draw(time) {
    if (!this.model) return;
    const ctx = this.ctx;
    const width = this.canvas.width;
    const height = this.canvas.height;
    const params = this.model.parameters || {};
    const breath = params.breath || { amplitude: 0.018, speed: 0.86 };
    const sway = params.sway || { amplitude: 0.035, speed: 0.42 };
    const pointerFollow = params.pointerFollow || { x: 18, y: 10, rotation: 0.055 };
    const seconds = time / 1000;
    const breathWave = Math.sin(seconds * Math.PI * 2 * breath.speed);
    const swayWave = Math.sin(seconds * Math.PI * 2 * sway.speed);
    const frame = this.currentFrame(time);

    ctx.clearRect(0, 0, width, height);
    ctx.save();
    ctx.scale(this.pixelRatio, this.pixelRatio);

    const cssWidth = width / this.pixelRatio;
    const cssHeight = height / this.pixelRatio;
    const modelScale = Math.min(cssWidth / 460, cssHeight / 540);
    const centerX = cssWidth / 2 + this.pointer.x * pointerFollow.x;
    const baseY = cssHeight * 0.54 + this.pointer.y * pointerFollow.y;
    const rotation = this.pointer.x * pointerFollow.rotation + swayWave * sway.amplitude;
    const scaleX = modelScale * (1 - breath.amplitude * 0.45 * breathWave);
    const scaleY = modelScale * (1 + breath.amplitude * breathWave);

    if (this.parts.aura) {
      ctx.save();
      ctx.globalAlpha = 0.28 + 0.08 * breathWave;
      ctx.translate(centerX, baseY + 8);
      ctx.rotate(rotation * 0.35);
      ctx.scale(scaleX * 1.02, scaleY * 1.02);
      ctx.drawImage(this.parts.aura, -this.parts.aura.width / 2, -this.parts.aura.height / 2);
      ctx.restore();
    }

    if (frame) {
      this.drawWarpedFrame(ctx, frame.image, centerX, baseY, scaleX, scaleY, rotation, seconds);
      if (frame.mix > 0.76 && frame.next) {
        ctx.save();
        ctx.globalAlpha = (frame.mix - 0.76) / 0.24 * 0.18;
        this.drawWarpedFrame(ctx, frame.next, centerX, baseY, scaleX, scaleY, rotation, seconds + 0.12);
        ctx.restore();
      }
    }

    ctx.restore();
  }

  drawWarpedFrame(ctx, image, centerX, centerY, scaleX, scaleY, rotation, seconds) {
    const mesh = this.model.parameters?.mesh || { slices: 28, amplitude: 3.2 };
    const slices = mesh.slices || 28;
    const sourceW = image.width;
    const sourceH = image.height;
    const destW = sourceW * scaleX;
    const destH = sourceH * scaleY;
    const sliceW = sourceW / slices;
    const destSliceW = destW / slices;

    ctx.save();
    ctx.translate(centerX, centerY);
    ctx.rotate(rotation);
    ctx.translate(-destW / 2, -destH / 2);

    for (let i = 0; i < slices; i += 1) {
      const phase = i / (slices - 1);
      const softEdge = Math.sin(Math.PI * phase);
      const wave = Math.sin(seconds * Math.PI * 2 * 0.72 + phase * Math.PI * 1.8);
      const yOffset = wave * (mesh.amplitude || 3.2) * softEdge * scaleY;
      ctx.drawImage(
        image,
        Math.floor(i * sliceW),
        0,
        Math.ceil(sliceW) + 1,
        sourceH,
        i * destSliceW,
        yOffset,
        Math.ceil(destSliceW) + 1,
        destH,
      );
    }

    ctx.restore();
  }
}

window.YakmoLive2D = YakmoLive2D;
