#!/usr/bin/env python3
"""Yakmo desktop pet: transparent always-on-top animated Windows pet."""

from __future__ import annotations

import argparse
import json
import math
import random
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Callable

from PIL import Image, ImageTk

import tkinter as tk
from tkinter import messagebox


APP_TITLE = "牦牦 Yakmo 桌宠"
APP_DIR_NAME = "yakmo_pet"
TRANSPARENT_COLOR = "#00ff7f"


@dataclass
class Motion:
    name: str
    fps: int
    frames: list[Image.Image]
    loop: bool = True


def bundled_root() -> Path:
    if getattr(sys, "frozen", False):
        return Path(getattr(sys, "_MEIPASS")) / APP_DIR_NAME
    return Path(__file__).resolve().parent / APP_DIR_NAME


def clamp(value: int, lower: int, upper: int) -> int:
    return max(lower, min(upper, value))


class YakmoDesktopPet:
    def __init__(self, root: tk.Tk, asset_root: Path, scale: float = 0.72) -> None:
        self.root = root
        self.asset_root = asset_root
        self.scale = scale
        self.model = self.load_model()
        self.motions = self.load_motions()
        self.motion_name = "idle"
        self.frame_index = 0
        self.last_frame_at = time.perf_counter()
        self.motion_started_at = time.perf_counter()
        self.one_shot_return: str | None = None
        self.topmost = True
        self.drag_offset = (0, 0)
        self.walk_direction = 0
        self.next_random_at = time.perf_counter() + random.uniform(7, 13)
        self.current_photo: ImageTk.PhotoImage | None = None

        self.width, self.height = self.motions["idle"].frames[0].size
        self.configure_window()
        self.create_menu()
        self.position_initially()
        self.bind_events()

    def load_model(self) -> dict:
        path = self.asset_root / "model" / "yakmo.live2d.json"
        if not path.is_file():
            raise FileNotFoundError(f"Missing model file: {path}")
        return json.loads(path.read_text(encoding="utf-8"))

    def load_motions(self) -> dict[str, Motion]:
        motions: dict[str, Motion] = {}
        for name, spec in self.model["motions"].items():
            frames = []
            for rel in spec["frames"]:
                frame = Image.open(self.asset_root / rel).convert("RGBA")
                if self.scale != 1:
                    frame = frame.resize(
                        (
                            max(1, round(frame.width * self.scale)),
                            max(1, round(frame.height * self.scale)),
                        ),
                        Image.Resampling.LANCZOS,
                    )
                frames.append(frame)
            motions[name] = Motion(
                name=name,
                fps=int(spec.get("fps", 18)),
                frames=frames,
                loop=bool(spec.get("loop", True)),
            )
        return motions

    def configure_window(self) -> None:
        self.root.title(APP_TITLE)
        self.root.overrideredirect(True)
        self.root.configure(bg=TRANSPARENT_COLOR)
        self.root.attributes("-topmost", True)
        self.root.attributes("-transparentcolor", TRANSPARENT_COLOR)
        self.root.wm_attributes("-toolwindow", True)

        self.canvas = tk.Canvas(
            self.root,
            width=self.width,
            height=self.height,
            bg=TRANSPARENT_COLOR,
            highlightthickness=0,
            bd=0,
        )
        self.canvas.pack()
        self.sprite_id = self.canvas.create_image(0, 0, anchor="nw")

    def position_initially(self) -> None:
        self.root.update_idletasks()
        screen_w = self.root.winfo_screenwidth()
        screen_h = self.root.winfo_screenheight()
        x = screen_w - self.width - 80
        y = screen_h - self.height - 100
        self.root.geometry(f"{self.width}x{self.height}+{max(0, x)}+{max(0, y)}")

    def create_menu(self) -> None:
        self.menu = tk.Menu(self.root, tearoff=False)
        self.menu.add_command(label="待机", command=lambda: self.set_motion("idle"))
        self.menu.add_command(label="挥手", command=lambda: self.set_motion("waving", one_shot=True))
        self.menu.add_command(label="跳跃", command=lambda: self.set_motion("jumping", one_shot=True))
        self.menu.add_command(label="等待", command=lambda: self.set_motion("waiting"))
        self.menu.add_command(label="处理", command=lambda: self.set_motion("running"))
        self.menu.add_command(label="复核", command=lambda: self.set_motion("review"))
        self.menu.add_command(label="失败", command=lambda: self.set_motion("failed", one_shot=True))
        self.menu.add_separator()
        self.menu.add_command(label="向左走", command=lambda: self.start_walk("running-left"))
        self.menu.add_command(label="向右走", command=lambda: self.start_walk("running-right"))
        self.menu.add_separator()
        self.menu.add_command(label="放大", command=lambda: self.change_scale(1.1))
        self.menu.add_command(label="缩小", command=lambda: self.change_scale(0.9))
        self.menu.add_command(label="置顶开关", command=self.toggle_topmost)
        self.menu.add_command(label="关于", command=self.show_about)
        self.menu.add_separator()
        self.menu.add_command(label="退出", command=self.root.destroy)

    def bind_events(self) -> None:
        self.canvas.bind("<ButtonPress-1>", self.begin_drag)
        self.canvas.bind("<B1-Motion>", self.drag)
        self.canvas.bind("<ButtonRelease-1>", self.end_drag)
        self.canvas.bind("<Double-Button-1>", lambda _event: self.set_motion("waving", one_shot=True))
        self.canvas.bind("<Button-3>", self.show_menu)
        self.root.bind("<Escape>", lambda _event: self.root.destroy())

    def begin_drag(self, event: tk.Event) -> None:
        self.walk_direction = 0
        self.drag_offset = (event.x_root - self.root.winfo_x(), event.y_root - self.root.winfo_y())

    def drag(self, event: tk.Event) -> None:
        dx, dy = self.drag_offset
        x = event.x_root - dx
        y = event.y_root - dy
        self.root.geometry(f"+{x}+{y}")

    def end_drag(self, _event: tk.Event) -> None:
        if self.motion_name in {"running-left", "running-right"}:
            self.set_motion("idle")

    def show_menu(self, event: tk.Event) -> None:
        self.menu.tk_popup(event.x_root, event.y_root)

    def show_about(self) -> None:
        messagebox.showinfo(
            APP_TITLE,
            "牦牦桌宠\n\n左键拖拽，双击挥手，右键打开菜单。\n使用当前 Live2D-like 补帧资源制作。",
        )

    def toggle_topmost(self) -> None:
        self.topmost = not self.topmost
        self.root.attributes("-topmost", self.topmost)

    def change_scale(self, factor: float) -> None:
        self.scale = min(1.2, max(0.45, self.scale * factor))
        self.motions = self.load_motions()
        old_x, old_y = self.root.winfo_x(), self.root.winfo_y()
        self.width, self.height = self.motions["idle"].frames[0].size
        self.canvas.configure(width=self.width, height=self.height)
        self.root.geometry(f"{self.width}x{self.height}+{old_x}+{old_y}")
        self.frame_index = 0

    def set_motion(self, name: str, one_shot: bool = False) -> None:
        if name not in self.motions:
            return
        self.motion_name = name
        self.frame_index = 0
        self.motion_started_at = time.perf_counter()
        self.last_frame_at = 0
        self.one_shot_return = "idle" if one_shot else None
        if name not in {"running-left", "running-right"}:
            self.walk_direction = 0

    def start_walk(self, name: str) -> None:
        self.walk_direction = -1 if name == "running-left" else 1
        self.set_motion(name)

    def maybe_random_motion(self) -> None:
        now = time.perf_counter()
        if now < self.next_random_at or self.motion_name != "idle":
            return
        action = random.choice(["waving", "jumping", "waiting", "review", "running"])
        self.set_motion(action, one_shot=action in {"waving", "jumping"})
        self.next_random_at = now + random.uniform(9, 18)
        if action in {"waiting", "review", "running"}:
            self.root.after(random.randint(2200, 4200), lambda: self.set_motion("idle"))

    def move_if_walking(self) -> None:
        if not self.walk_direction:
            return
        x = self.root.winfo_x()
        y = self.root.winfo_y()
        screen_w = self.root.winfo_screenwidth()
        next_x = x + self.walk_direction * 4
        if next_x < 12:
            self.start_walk("running-right")
            next_x = 12
        elif next_x + self.width > screen_w - 12:
            self.start_walk("running-left")
            next_x = screen_w - self.width - 12
        self.root.geometry(f"+{next_x}+{y}")

    def draw_frame(self) -> None:
        motion = self.motions[self.motion_name]
        frame = motion.frames[self.frame_index]
        self.current_photo = ImageTk.PhotoImage(frame)
        self.canvas.itemconfigure(self.sprite_id, image=self.current_photo)

    def tick(self) -> None:
        self.maybe_random_motion()
        self.move_if_walking()
        motion = self.motions[self.motion_name]
        now = time.perf_counter()
        frame_duration = 1 / max(1, motion.fps)
        if now - self.last_frame_at >= frame_duration:
            self.last_frame_at = now
            self.draw_frame()
            self.frame_index += 1
            if self.frame_index >= len(motion.frames):
                if self.one_shot_return:
                    return_to = self.one_shot_return
                    self.one_shot_return = None
                    self.set_motion(return_to)
                else:
                    self.frame_index = 0
        self.root.after(16, self.tick)

    def run(self) -> None:
        self.draw_frame()
        self.root.after(16, self.tick)
        self.root.mainloop()


def smoke_test(asset_root: Path) -> int:
    model_path = asset_root / "model" / "yakmo.live2d.json"
    if not model_path.is_file():
        print(f"missing model: {model_path}", file=sys.stderr)
        return 1
    model = json.loads(model_path.read_text(encoding="utf-8"))
    counts = {}
    missing = []
    sizes = {}
    for name, spec in model["motions"].items():
        counts[name] = len(spec["frames"])
        for rel in spec["frames"]:
            path = asset_root / rel
            if not path.is_file():
                missing.append(rel)
        first = asset_root / spec["frames"][0]
        if first.is_file():
            with Image.open(first) as image:
                sizes[name] = list(image.size)
    ok = not missing and len(counts) == 9 and min(counts.values()) >= 12
    print(json.dumps({"ok": ok, "motions": counts, "sizes": sizes, "missing": missing}, ensure_ascii=False, indent=2))
    return 0 if ok else 1


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--smoke-test", action="store_true")
    parser.add_argument("--scale", type=float, default=0.72)
    args = parser.parse_args()

    asset_root = bundled_root()
    if args.smoke_test:
        return smoke_test(asset_root)

    root = tk.Tk()
    app = YakmoDesktopPet(root, asset_root, scale=args.scale)
    app.run()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
