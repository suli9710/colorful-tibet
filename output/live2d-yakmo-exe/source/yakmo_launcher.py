#!/usr/bin/env python3
"""Portable launcher for the Yakmo Live2D-like browser package."""

from __future__ import annotations

import argparse
import contextlib
import functools
import http.server
import json
import socket
import socketserver
import sys
import threading
import time
import urllib.request
import webbrowser
from pathlib import Path


APP_DIR_NAME = "yakmo_app"
APP_TITLE = "牦牦 Yakmo Live2D"


class QuietHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, _format: str, *_args: object) -> None:
        return

    def end_headers(self) -> None:
        self.send_header("Cache-Control", "no-store")
        super().end_headers()


class ThreadedTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    allow_reuse_address = True
    daemon_threads = True


def bundled_root() -> Path:
    if getattr(sys, "frozen", False):
        return Path(getattr(sys, "_MEIPASS")) / APP_DIR_NAME
    return Path(__file__).resolve().parent / APP_DIR_NAME


def free_port() -> int:
    with contextlib.closing(socket.socket(socket.AF_INET, socket.SOCK_STREAM)) as sock:
        sock.bind(("127.0.0.1", 0))
        return int(sock.getsockname()[1])


def start_server(root: Path) -> tuple[ThreadedTCPServer, str]:
    if not (root / "index.html").is_file():
        raise FileNotFoundError(f"Missing bundled app: {root}")

    port = free_port()
    handler = functools.partial(QuietHandler, directory=str(root))
    server = ThreadedTCPServer(("127.0.0.1", port), handler)
    thread = threading.Thread(target=server.serve_forever, name="yakmo-http", daemon=True)
    thread.start()
    return server, f"http://127.0.0.1:{port}/"


def open_browser(url: str) -> None:
    webbrowser.open(url, new=2, autoraise=True)


def smoke_test(root: Path) -> int:
    server, url = start_server(root)
    try:
        index = urllib.request.urlopen(url, timeout=10).read().decode("utf-8", errors="replace")
        model_raw = urllib.request.urlopen(url + "model/yakmo.live2d.json", timeout=10).read()
        model = json.loads(model_raw.decode("utf-8"))
        first_frame = model["motions"]["idle"]["frames"][0]
        first_part = model["parts"]["base"]
        frame_status = urllib.request.urlopen(url + first_frame, timeout=10).status
        part_status = urllib.request.urlopen(url + first_part, timeout=10).status
        ok = (
            "<canvas" in index
            and len(model.get("motions", {})) == 9
            and frame_status == 200
            and part_status == 200
        )
        print(
            json.dumps(
                {
                    "ok": ok,
                    "url": url,
                    "motions": {key: len(value["frames"]) for key, value in model["motions"].items()},
                    "frame_status": frame_status,
                    "part_status": part_status,
                },
                ensure_ascii=False,
                indent=2,
            )
        )
        return 0 if ok else 1
    finally:
        server.shutdown()
        server.server_close()


def run_gui(url: str, server: ThreadedTCPServer) -> int:
    import tkinter as tk
    from tkinter import messagebox

    root = tk.Tk()
    root.title(APP_TITLE)
    root.geometry("420x220")
    root.minsize(380, 200)
    root.configure(bg="#fff7e5")

    title = tk.Label(
        root,
        text=APP_TITLE,
        bg="#fff7e5",
        fg="#2b2116",
        font=("Microsoft YaHei UI", 18, "bold"),
    )
    title.pack(pady=(22, 8))

    hint = tk.Label(
        root,
        text="已启动本地预览服务。关闭这个窗口会结束服务。",
        bg="#fff7e5",
        fg="#6d604f",
        font=("Microsoft YaHei UI", 10),
    )
    hint.pack()

    url_label = tk.Label(
        root,
        text=url,
        bg="#fff7e5",
        fg="#3a5d7a",
        font=("Consolas", 10),
    )
    url_label.pack(pady=(10, 16))

    buttons = tk.Frame(root, bg="#fff7e5")
    buttons.pack()

    def reopen() -> None:
        open_browser(url)

    def copy_url() -> None:
        root.clipboard_clear()
        root.clipboard_append(url)
        messagebox.showinfo(APP_TITLE, "链接已复制。")

    def close() -> None:
        server.shutdown()
        server.server_close()
        root.destroy()

    tk.Button(buttons, text="打开浏览器", width=13, command=reopen).grid(row=0, column=0, padx=6)
    tk.Button(buttons, text="复制链接", width=13, command=copy_url).grid(row=0, column=1, padx=6)
    tk.Button(buttons, text="退出", width=13, command=close).grid(row=0, column=2, padx=6)

    root.protocol("WM_DELETE_WINDOW", close)
    open_browser(url)
    root.mainloop()
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--smoke-test", action="store_true")
    args = parser.parse_args()

    app_root = bundled_root()
    if args.smoke_test:
        return smoke_test(app_root)

    server, url = start_server(app_root)
    try:
        return run_gui(url, server)
    except Exception as exc:
        server.shutdown()
        server.server_close()
        print(f"{APP_TITLE} failed: {exc}", file=sys.stderr)
        time.sleep(4)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
