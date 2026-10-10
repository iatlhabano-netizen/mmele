#!/usr/bin/env bash
# Point the app and docs at the deployed site once you know its domain:
#   site/set-domain.sh mmele.vercel.app          (or a custom domain, with or without https://)
# Updates AppInfo.kt PRIVACY_URL / TERMS_URL (DELETE_DATA_URL follows PRIVACY_URL automatically)
# and the docs (old tshipiworks.com/mmele URLs and the YOUR-VERCEL-DOMAIN token).
# The app change only takes effect after a rebuild with a new versionCode if 1.0.0 was already uploaded.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
[ $# -eq 1 ] || { sed -n '2,6p' "$0"; exit 2; }
ROOT="$ROOT" DOMAIN="$1" python3 - <<'PY'
import os, re, sys
from pathlib import Path
root = Path(os.environ["ROOT"])
d = re.sub(r"^https?://", "", os.environ["DOMAIN"].strip()).rstrip("/")
if not re.fullmatch(r"[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+", d):
    sys.exit(f"Not a domain name: {d!r}")
base = f"https://{d}"
changed = []
app = root / "app/src/main/kotlin/bw/mmele/AppInfo.kt"
s = app.read_text(encoding="utf-8")
t = re.sub(r'(const val PRIVACY_URL = )"[^"]*"', rf'\g<1>"{base}/privacy"', s)
t = re.sub(r'(const val TERMS_URL = )"[^"]*"', rf'\g<1>"{base}/terms"', t)
if t != s: app.write_text(t, encoding="utf-8"); changed.append(app)
for f in sorted(root.glob("docs/**/*.md")):
    s = f.read_text(encoding="utf-8")
    t = (s.replace("https://www.tshipiworks.com/mmele/privacy", f"{base}/privacy")
          .replace("https://www.tshipiworks.com/mmele/terms", f"{base}/terms")
          .replace("https://YOUR-VERCEL-DOMAIN", base).replace("YOUR-VERCEL-DOMAIN", d))
    if t != s: f.write_text(t, encoding="utf-8"); changed.append(f)
for f in changed: print("updated", f.relative_to(root))
print(f"Site base is now {base}. Review with: git diff")
PY
