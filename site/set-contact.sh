#!/usr/bin/env bash
# Fill in the public contact details everywhere at once:
#   site/*.html, site/build_pages.py, docs/**/*.md (privacy policy, terms, listing...) and the app's AppInfo.kt.
#
#   site/set-contact.sh --email mmele@example.com
#   site/set-contact.sh --address "Plot 123, P.O. Box 456"
#   site/set-contact.sh --email mmele@example.com --address "Plot 123, P.O. Box 456"
#   site/set-contact.sh --check          # list files that still contain a placeholder
#
# Tokens replaced: CONTACT_EMAIL_PLACEHOLDER and POSTAL_ADDRESS_PLACEHOLDER. In AppInfo.kt the
# CONTACT_EMAIL constant is set directly (whatever its current value) and its TODO comment removed.
# Safe to re-run. Review with `git diff`, then commit. Re-deploy the site afterwards.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
EMAIL="" ADDRESS="" CHECK=0
while [ $# -gt 0 ]; do
  case "$1" in
    --email) EMAIL="${2:-}"; shift 2 ;;
    --address) ADDRESS="${2:-}"; shift 2 ;;
    --check) CHECK=1; shift ;;
    -h|--help) sed -n '2,13p' "$0"; exit 0 ;;
    *) echo "Unknown option: $1 (see --help)" >&2; exit 2 ;;
  esac
done
if [ "$CHECK" = 1 ]; then
  cd "$ROOT" && grep -rlE "CONTACT_EMAIL_PLACEHOLDER|POSTAL_ADDRESS_PLACEHOLDER" site docs app/src/main/kotlin --exclude=set-contact.sh || echo "No placeholders left."
  grep -n 'const val CONTACT_EMAIL' "$ROOT"/app/src/main/kotlin/bw/mmele/AppInfo.kt
  exit 0
fi
[ -n "$EMAIL$ADDRESS" ] || { echo "Nothing to do: pass --email and/or --address (see --help)" >&2; exit 2; }
ROOT="$ROOT" EMAIL="$EMAIL" ADDRESS="$ADDRESS" python3 - <<'PY'
import html, os, re, sys
from pathlib import Path
root, email, address = Path(os.environ["ROOT"]), os.environ["EMAIL"].strip(), os.environ["ADDRESS"].strip()
if email and not re.fullmatch(r"[^@\s\"'<>]+@[^@\s\"'<>]+\.[A-Za-z]{2,}", email):
    sys.exit(f"Not an email address: {email!r}")
if address and re.search(r"[\"<>\n]", address):
    sys.exit("The address must be one line without quotes or < >")
files = sorted(root.glob("site/*.html")) + [root / "site/build_pages.py"] + sorted(root.glob("docs/**/*.md"))
changed = []
for f in files:
    s = t = f.read_text(encoding="utf-8")
    if email:
        t = t.replace("CONTACT_EMAIL_PLACEHOLDER", email)
    if address:
        t = t.replace("POSTAL_ADDRESS_PLACEHOLDER", html.escape(address) if f.suffix == ".html" else address)
    if t != s:
        f.write_text(t, encoding="utf-8"); changed.append(f)
app = root / "app/src/main/kotlin/bw/mmele/AppInfo.kt"
if email and app.exists():
    s = app.read_text(encoding="utf-8")
    t = re.sub(r'(const val CONTACT_EMAIL = )"[^"]*"', lambda m: f'{m.group(1)}"{email}"', s)
    t = re.sub(r"\n[ \t]*// TODO\(release\): replace with the real[^\n]*\n[ \t]*//[^\n]*\n", "\n", t)
    if t != s:
        app.write_text(t, encoding="utf-8"); changed.append(app)
for f in changed:
    print("updated", f.relative_to(root))
print(f"{len(changed)} file(s) changed. Review with: git diff")
PY
