#!/usr/bin/env python3
"""Local preview that behaves like Vercel's cleanUrls + trailingSlash:false + 404.html.
   python3 site/preview.py [port]   ->  http://localhost:8000/  /privacy  /terms"""
import http.server, os, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent

class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *a, **k):
        super().__init__(*a, directory=str(ROOT), **k)

    def do_GET(self):
        path = self.path.split("?", 1)[0].split("#", 1)[0]
        if path.endswith(".html"):                       # /privacy.html -> /privacy
            target = path[:-5] if path != "/index.html" else "/"
            self.send_response(308); self.send_header("Location", target); self.end_headers(); return
        if len(path) > 1 and path.endswith("/"):        # trailingSlash: false
            self.send_response(308); self.send_header("Location", path.rstrip("/")); self.end_headers(); return
        f = ROOT / path.lstrip("/")
        if path == "/":
            self.path = "/index.html"
        elif not f.is_file() and (ROOT / (path.lstrip("/") + ".html")).is_file():
            self.path = path + ".html"
        elif not f.is_file():
            body = (ROOT / "404.html").read_bytes()
            self.send_response(404); self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body))); self.end_headers(); self.wfile.write(body); return
        super().do_GET()

if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8000
    print(f"Previewing {ROOT} at http://localhost:{port}/")
    http.server.ThreadingHTTPServer(("127.0.0.1", port), Handler).serve_forever()
