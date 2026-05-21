(function () {
  var DEFAULT_MODEL = "persimmon-hippo-v2.model.json";
  var prefersReducedMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches;

  function CapybaraLuluLive2D(container, options) {
    options = options || {};
    this.container =
      typeof container === "string" ? document.querySelector(container) : container;
    if (!this.container) throw new Error("CapybaraLuluLive2D: container not found");

    this.options = {
      model: DEFAULT_MODEL,
      width: 288,
      height: 312,
      motion: null,
      autoInteract: true,
      autoAmbient: true,
      interpolate: true,
      transparent: true,
      pixelRatio: window.devicePixelRatio || 1
    };
    var keys = Object.keys(options);
    for (var i = 0; i < keys.length; i++) {
      this.options[keys[i]] = options[keys[i]];
    }

    this.canvas = document.createElement("canvas");
    this.canvas.className = "capybara-lulu-canvas";
    this.canvas.setAttribute("role", "img");
    this.canvas.setAttribute("aria-label", "水豚噜噜 animated mascot");
    this.ctx = this.canvas.getContext("2d", { alpha: true });
    this.container.appendChild(this.canvas);

    this.model = null;
    this.image = null;
    this.motionName = null;
    this.previousMotionName = null;
    this.frame = 0;
    this.nextFrame = 0;
    this.frameBlend = 0;
    this.lastFrameAt = 0;
    this.motionStartedAt = 0;
    this.raf = 0;
    this.disposed = false;
    this.pointer = { x: 0, y: 0, inside: false, pressure: 0 };
    this.inertia = { x: 0, y: 0, tilt: 0, scale: 1 };
    this._lastInertia = { x: 0, y: 0, tilt: 0, scale: 1 };
    this.ambientTimer = 0;
    this.clickToggle = 0;
    this.lastTapAt = 0;

    this.resize(this.options.width, this.options.height);
    this.onPointerMove = this.onPointerMove.bind(this);
    this.onPointerLeave = this.onPointerLeave.bind(this);
    this.onPointerDown = this.onPointerDown.bind(this);
    this.onVisibility = this.onVisibility.bind(this);
  }

  CapybaraLuluLive2D.prototype.load = function () {
    var self = this;
    var modelUrl = new URL(this.options.model, document.currentScript?.src || location.href);
    return fetch(modelUrl)
      .then(function (response) {
        if (!response.ok) throw new Error("Failed to load model: " + response.status);
        return response.json();
      })
      .then(function (model) {
        self.model = model;
        self.tuning = normalizeTuning(self.model.motionTuning || {});
        return loadImage(new URL(self.model.texture, modelUrl).href);
      })
      .then(function (image) {
        self.image = image;
        self.setMotion(self.options.motion || self.model.defaultMotion || "idle", { immediate: true });
        self.attachEvents();
        self.scheduleAmbient();
        self.start();
        return self;
      });
  };

  CapybaraLuluLive2D.prototype.attachEvents = function () {
    if (!this.options.autoInteract) return;
    this.canvas.addEventListener("pointermove", this.onPointerMove);
    this.canvas.addEventListener("pointerleave", this.onPointerLeave);
    this.canvas.addEventListener("pointerdown", this.onPointerDown);
    document.addEventListener("visibilitychange", this.onVisibility);
  };

  CapybaraLuluLive2D.prototype.detachEvents = function () {
    this.canvas.removeEventListener("pointermove", this.onPointerMove);
    this.canvas.removeEventListener("pointerleave", this.onPointerLeave);
    this.canvas.removeEventListener("pointerdown", this.onPointerDown);
    document.removeEventListener("visibilitychange", this.onVisibility);
  };

  CapybaraLuluLive2D.prototype.resize = function (width, height) {
    var ratio = this.options.pixelRatio;
    this.canvas.style.width = width + "px";
    this.canvas.style.height = height + "px";
    this.canvas.width = Math.round(width * ratio);
    this.canvas.height = Math.round(height * ratio);
    this.ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
    this.options.width = width;
    this.options.height = height;
    this.draw(performance.now());
  };

  CapybaraLuluLive2D.prototype.setMotion = function (name, config) {
    config = config || {};
    if (!this.model?.motions?.[name]) return false;
    if (name === this.motionName && !config.restart) return true;
    this.previousMotionName = config.immediate ? null : this.motionName;
    this.motionName = name;
    this.frame = 0;
    this.nextFrame = 0;
    this.frameBlend = 0;
    this.lastFrameAt = 0;
    this.motionStartedAt = performance.now();
    this._lastInertia = { x: Infinity, y: Infinity, tilt: Infinity, scale: Infinity };
    this.draw(this.motionStartedAt);
    return true;
  };

  CapybaraLuluLive2D.prototype.queueMotion = function (name) {
    return this.setMotion(name, { restart: true });
  };

  CapybaraLuluLive2D.prototype.start = function () {
    if (this.raf || this.disposed) return;
    var self = this;
    var tick = function (time) {
      self.raf = requestAnimationFrame(tick);
      self.update(time);
    };
    this.raf = requestAnimationFrame(tick);
  };

  CapybaraLuluLive2D.prototype.stop = function () {
    cancelAnimationFrame(this.raf);
    this.raf = 0;
  };

  CapybaraLuluLive2D.prototype.dispose = function () {
    this.stop();
    this.detachEvents();
    clearTimeout(this.ambientTimer);
    this.disposed = true;
    this.canvas.remove();
  };

  CapybaraLuluLive2D.prototype.update = function (time) {
    if (!this.model || !this.image || !this.motionName) return;
    var motion = this.model.motions[this.motionName];
    var frameMs = 1000 / motion.fps;
    if (!this.lastFrameAt) this.lastFrameAt = time;

    var frameChanged = false;

    if (this.options.interpolate) {
      var elapsed = time - this.motionStartedAt;
      var rawFrame = elapsed / frameMs;

      if (motion.loop) {
        rawFrame = rawFrame % motion.frames;
        if (rawFrame < 0) rawFrame += motion.frames;
        var newFrame = Math.floor(rawFrame);
        if (newFrame !== this.frame) frameChanged = true;
        this.frame = newFrame;
        this.nextFrame = (this.frame + 1) % motion.frames;
        this.frameBlend = rawFrame - this.frame;
      } else {
        if (rawFrame >= motion.frames - 0.001) {
          this.setMotion(motion.next || this.model.defaultMotion || "idle");
          return;
        }
        var newFrame2 = Math.floor(rawFrame);
        if (newFrame2 !== this.frame) frameChanged = true;
        this.frame = newFrame2;
        this.nextFrame = Math.min(this.frame + 1, motion.frames - 1);
        this.frameBlend = rawFrame - this.frame;
      }
    } else {
      if (time - this.lastFrameAt >= frameMs) {
        var skipped = Math.max(1, Math.floor((time - this.lastFrameAt) / frameMs));
        this.lastFrameAt += skipped * frameMs;
        this.frame += skipped;
        frameChanged = true;
        if (this.frame >= motion.frames) {
          if (motion.loop) {
            this.frame %= motion.frames;
          } else {
            this.setMotion(motion.next || this.model.defaultMotion || "idle");
            return;
          }
        }
      }
      this.frameBlend = 0;
    }

    this.updateInertia(time);

    if (this.options.interpolate) {
      this._lastInertia.x = this.inertia.x;
      this._lastInertia.y = this.inertia.y;
      this._lastInertia.tilt = this.inertia.tilt;
      this._lastInertia.scale = this.inertia.scale;
      this.draw(time);
    } else {
      var blending = this.previousMotionName != null;
      var physicsDrifted = !prefersReducedMotion && (
        Math.abs(this.inertia.x - this._lastInertia.x) > 0.25 ||
        Math.abs(this.inertia.y - this._lastInertia.y) > 0.25 ||
        Math.abs(this.inertia.tilt - this._lastInertia.tilt) > 0.12 ||
        Math.abs(this.inertia.scale - this._lastInertia.scale) > 0.001
      );
      if (frameChanged || blending || physicsDrifted) {
        this._lastInertia.x = this.inertia.x;
        this._lastInertia.y = this.inertia.y;
        this._lastInertia.tilt = this.inertia.tilt;
        this._lastInertia.scale = this.inertia.scale;
        this.draw(time);
      }
    }
  };

  CapybaraLuluLive2D.prototype.updateInertia = function (time) {
    if (prefersReducedMotion) {
      this.inertia = { x: 0, y: 0, tilt: 0, scale: 1 };
      return;
    }
    var width = this.options.width;
    var height = this.options.height;
    var nx = this.pointer.inside ? clamp(this.pointer.x / width - 0.5, -0.5, 0.5) : 0;
    var ny = this.pointer.inside ? clamp(this.pointer.y / height - 0.5, -0.5, 0.5) : 0;
    var pressure = this.pointer.inside ? 1 : 0;
    var breath = Math.sin(time * 0.001 * Math.PI * 2 * this.tuning.breathSpeed);
    var float = Math.sin(time * 0.001 * Math.PI * 2 * this.tuning.floatSpeed);
    var targetX = nx * this.tuning.maxPointerOffset;
    var targetY = ny * this.tuning.maxPointerOffset + float * this.tuning.floatAmplitude;
    var targetTilt = nx * this.tuning.maxPointerTilt + Math.sin(time * 0.0014) * 0.8;
    var targetScale = 1 + breath * this.tuning.breathAmplitude + pressure * 0.006;
    this.inertia.x += (targetX - this.inertia.x) * 0.08;
    this.inertia.y += (targetY - this.inertia.y) * 0.08;
    this.inertia.tilt += (targetTilt - this.inertia.tilt) * 0.07;
    this.inertia.scale += (targetScale - this.inertia.scale) * 0.08;
  };

  CapybaraLuluLive2D.prototype.draw = function (time) {
    if (!this.ctx || !this.model || !this.image || !this.motionName) return;
    time = time || performance.now();
    var atlas = this.model.atlas;
    var cellWidth = atlas.cellWidth;
    var cellHeight = atlas.cellHeight;
    var motion = this.model.motions[this.motionName];
    var sx = this.frame * cellWidth;
    var sy = motion.row * cellHeight;
    var scale = Math.min(this.options.width / cellWidth, this.options.height / cellHeight);
    var dw = cellWidth * scale;
    var dh = cellHeight * scale;
    var dx = (this.options.width - dw) / 2;
    var dy = (this.options.height - dh) / 2;
    var blend = this.blendAlpha(time);

    this.ctx.clearRect(0, 0, this.options.width, this.options.height);

    var hasTransform = Math.abs(this.inertia.x) > 0.3
      || Math.abs(this.inertia.y) > 0.3
      || Math.abs(this.inertia.tilt) > 0.15
      || Math.abs(this.inertia.scale - 1) > 0.001;

    if (hasTransform) {
      this.ctx.save();
      this.ctx.translate(this.options.width / 2 + this.inertia.x, this.options.height / 2 + this.inertia.y);
      this.ctx.rotate((this.inertia.tilt * Math.PI) / 180);
      this.ctx.scale(this.inertia.scale, this.inertia.scale);
      this.ctx.translate(-this.options.width / 2, -this.options.height / 2);
      this.ctx.imageSmoothingEnabled = true;

      this.ctx.globalAlpha = blend;
      this.ctx.drawImage(this.image, sx, sy, cellWidth, cellHeight, dx, dy, dw, dh);

      if (this.frameBlend > 0.005) {
        this.ctx.globalAlpha = blend * this.frameBlend;
        var nsx = this.nextFrame * cellWidth;
        this.ctx.drawImage(this.image, nsx, sy, cellWidth, cellHeight, dx, dy, dw, dh);
      }

      this.ctx.globalAlpha = 1;
      this.ctx.restore();
    } else {
      this.ctx.imageSmoothingEnabled = true;

      this.ctx.globalAlpha = blend;
      this.ctx.drawImage(this.image, sx, sy, cellWidth, cellHeight, dx, dy, dw, dh);

      if (this.frameBlend > 0.005) {
        this.ctx.globalAlpha = blend * this.frameBlend;
        var nsx2 = this.nextFrame * cellWidth;
        this.ctx.drawImage(this.image, nsx2, sy, cellWidth, cellHeight, dx, dy, dw, dh);
      }

      this.ctx.globalAlpha = 1;
    }
  };

  CapybaraLuluLive2D.prototype.blendAlpha = function (time) {
    if (!this.previousMotionName || prefersReducedMotion) return 1;
    var elapsed = time - this.motionStartedAt;
    var blend = clamp(elapsed / this.tuning.motionBlendMs, 0, 1);
    if (blend >= 1) this.previousMotionName = null;
    return easeOutCubic(blend);
  };

  CapybaraLuluLive2D.prototype.onPointerMove = function (event) {
    var rect = this.canvas.getBoundingClientRect();
    this.pointer.x = event.clientX - rect.left;
    this.pointer.y = event.clientY - rect.top;
    this.pointer.inside = true;
    this.pointer.pressure = event.pressure || 0;
    if (this.motionName === "idle" && this.model.interactions?.hover) {
      this.setMotion(this.model.interactions.hover);
    }
  };

  CapybaraLuluLive2D.prototype.onPointerLeave = function () {
    this.pointer.inside = false;
    if (this.model.interactions?.far) this.setMotion(this.model.interactions.far);
  };

  CapybaraLuluLive2D.prototype.onPointerDown = function () {
    var now = performance.now();
    if (now - this.lastTapAt < 280 && this.model.interactions?.doubleClick) {
      this.lastTapAt = 0;
      this.queueMotion(this.model.interactions.doubleClick);
      return;
    }
    this.lastTapAt = now;
    var clickMotions = this.model.interactions?.click || ["waving"];
    var motion = clickMotions[this.clickToggle % clickMotions.length];
    this.clickToggle += 1;
    this.queueMotion(motion);
  };

  CapybaraLuluLive2D.prototype.onVisibility = function () {
    if (document.hidden) this.stop();
    else this.start();
  };

  CapybaraLuluLive2D.prototype.scheduleAmbient = function () {
    clearTimeout(this.ambientTimer);
    if (!this.options.autoAmbient || prefersReducedMotion) return;
    var min = this.tuning.ambientMinMs;
    var max = this.tuning.ambientMaxMs;
    var delay = min + Math.random() * (max - min);
    var self = this;
    this.ambientTimer = setTimeout(function () {
      if (!self.pointer.inside && self.motionName === "idle") {
        var list = self.model.interactions?.ambient || ["idle"];
        var next = list[Math.floor(Math.random() * list.length)];
        self.setMotion(next, { restart: true });
      }
      self.scheduleAmbient();
    }, delay);
  };

  function normalizeTuning(raw) {
    return {
      breathAmplitude: raw.breathAmplitude ?? 0.018,
      breathSpeed: raw.breathSpeed ?? 1.1,
      floatAmplitude: raw.floatAmplitude ?? 2.4,
      floatSpeed: raw.floatSpeed ?? 0.85,
      maxPointerTilt: raw.maxPointerTilt ?? 5,
      maxPointerOffset: raw.maxPointerOffset ?? 5,
      motionBlendMs: raw.motionBlendMs ?? 180,
      ambientMinMs: raw.ambientMinMs ?? 6500,
      ambientMaxMs: raw.ambientMaxMs ?? 12000
    };
  }

  function loadImage(src) {
    return new Promise(function (resolve, reject) {
      var image = new Image();
      image.onload = function () { resolve(image); };
      image.onerror = function () { reject(new Error("Failed to load texture: " + src)); };
      image.src = src;
    });
  }

  function clamp(value, min, max) {
    return Math.min(max, Math.max(min, value));
  }

  function easeOutCubic(value) {
    return 1 - Math.pow(1 - value, 3);
  }

  window.CapybaraLuluV2 = {
    CapybaraLuluLive2D: CapybaraLuluLive2D,
    mount: function (container, options) {
      var widget = new CapybaraLuluLive2D(container, options);
      return widget.load();
    }
  };
})();
