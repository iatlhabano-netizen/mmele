#!/usr/bin/env python3
"""Generate the static Mmele legal site (index, privacy, terms, 404) from one template.

privacy.html and terms.html are rendered from docs/release/PRIVACY_POLICY.md and TERMS.md, so the
Markdown stays the single source of truth. Run after editing either document:

    python3 -m pip install markdown      # once
    python3 site/build_pages.py

The output is plain HTML committed to the repo; Vercel serves it as-is (no build step there).
Left out of the web pages: the Markdown's leading '>' note and any "[NOTE TO DEVELOPER: ...]".
"""
import html
import re
from pathlib import Path

import markdown

SITE = Path(__file__).resolve().parent
DOCS = SITE.parent / "docs" / "release"
EMAIL = "CONTACT_EMAIL_PLACEHOLDER"   # replaced by set-contact.sh
DEVELOPER = "Artful Tenant, trading as Tshipiworks"
PLACE = "Gabane, Botswana"
YEAR = "2026"

TEMPLATE = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title}</title>
<meta name="description" content="{description}">
<meta name="theme-color" content="#121417">
<meta name="color-scheme" content="dark">
<meta name="referrer" content="no-referrer">
<link rel="icon" type="image/png" sizes="32x32" href="/img/favicon-32.png">
<link rel="apple-touch-icon" href="/img/apple-touch-icon.png">
<link rel="stylesheet" href="/styles.css">
</head>
<body>
<a class="skip-link" href="#main">Skip to content</a>
<header class="site-header">
  <div class="wrap">
    <a class="brand" href="/"><img src="/img/favicon-32.png" srcset="/img/apple-touch-icon.png 5.6x" width="32" height="32" alt="">Mmele</a>
    <nav class="site-nav" aria-label="Site">
      <ul>
        <li><a href="/"{cur_home}>Home</a></li>
        <li><a href="/privacy"{cur_privacy}>Privacy</a></li>
        <li><a href="/terms"{cur_terms}>Terms</a></li>
      </ul>
    </nav>
  </div>
</header>
<main id="main" tabindex="-1">
  <div class="wrap">
{body}
  </div>
</main>
<footer class="site-footer">
  <div class="wrap">
    <p>&copy; {year} {developer} &middot; {place}</p>
    <p><a href="/privacy">Privacy policy</a> &middot; <a href="/terms">Terms of use</a> &middot; <a href="mailto:{email}">{email}</a></p>
    <p>This website uses no cookies, analytics or trackers, and loads nothing from other sites.</p>
  </div>
</footer>
</body>
</html>
"""


def normalise_lists(md: str) -> str:
    """The docs use 2-space nested lists and hanging indents; Python-Markdown wants 4-space nesting."""
    out = []
    for line in md.splitlines():
        stripped = line.lstrip(" ")
        indent = len(line) - len(stripped)
        is_item = bool(re.match(r"([-*+]|\d+\.)\s", stripped))
        if indent and not is_item and out and out[-1].strip() and not line.startswith("    ```"):
            out[-1] = out[-1].rstrip() + " " + stripped   # hanging continuation -> same line
        elif indent and is_item:
            out.append("    " * max(1, indent // 2) + stripped)
        else:
            if re.match(r"\*\*[^*]+:\*\*", line) and out and out[-1].strip():
                out[-1] = out[-1].rstrip() + "  "                # "**Label:** value" lines -> one per line
            out.append(line)
    return "\n".join(out) + "\n"


def render_doc(name: str):
    src = (DOCS / name).read_text(encoding="utf-8")
    lines = src.splitlines()
    title = next(l[2:].strip() for l in lines if l.startswith("# "))
    body = "\n".join(l for l in lines if not l.startswith("> ") and l != ">" and not l.startswith("# "))
    body = re.sub(r"\s*\[NOTE TO DEVELOPER:.*?\]", "", body, flags=re.S)
    body = re.sub(r"^\s*[-*]\s*$", "", body, flags=re.M)          # list items emptied by the line above
    m = re.search(r"\*\*Effective date:\*\*\s*([0-9]{1,2} \w+ [0-9]{4})", body)
    effective = m.group(1) if m else ""
    html_body = markdown.markdown(normalise_lists(body), extensions=["tables", "attr_list", "sane_lists"], output_format="html")
    def wrap_table(m):  # scrollable on phones; labelled by the heading above it
        heads = re.findall(r"<h[23][^>]*>(.*?)</h[23]>", html_body[: m.start()])
        label = re.sub(r"<[^>]+>", "", heads[-1]) if heads else "Table"
        return f'<div class="table-wrap" role="region" aria-label="{html.escape(label)}" tabindex="0"><table>'
    html_body = re.sub(r"<table>", wrap_table, html_body)
    html_body = html_body.replace("</table>", "</table></div>")
    # mailto links for the contact address (not inside existing tags/attributes)
    html_body = re.sub(rf"(?<![\w\"/:@.]){re.escape(EMAIL)}(?![\w\"])", f'<a href="mailto:{EMAIL}">{EMAIL}</a>', html_body)
    # external links open normally; mark them so screen-reader users know they leave the site
    html_body = re.sub(r'<a href="(https?://[^"]+)">', r'<a href="\1" rel="noopener">', html_body)
    return title, effective, html_body


def date_iso(d: str) -> str:
    import datetime
    return datetime.datetime.strptime(d, "%d %B %Y").date().isoformat()


def page(path, title, description, body, current=None):
    cur = {k: (' aria-current="page"' if k == current else "") for k in ("home", "privacy", "terms")}
    out = TEMPLATE.format(
        title=html.escape(title), description=html.escape(description), body=body,
        cur_home=cur["home"], cur_privacy=cur["privacy"], cur_terms=cur["terms"],
        year=YEAR, developer=DEVELOPER, place=PLACE, email=EMAIL,
    )
    (SITE / path).write_text(out, encoding="utf-8")
    print("wrote", path)


def legal(path, docname, short, description):
    title, effective, body = render_doc(docname)
    title = title.replace(" — ", ": ")
    meta = f'    <p class="meta">Last updated: <time datetime="{date_iso(effective)}">{effective}</time></p>\n' if effective else ""
    page(path, f"{title} | Mmele", description, f"    <h1>{html.escape(title)}</h1>\n{meta}{body}", short)


LANDING = """    <section class="hero" aria-labelledby="title">
      <img class="icon" src="/img/icon-512.png" width="128" height="128" alt="Mmele app icon: a Morabaraba board with a mill of three dark cows, against a Kalahari sunset">
      <h1 id="title">Mmele: Morabaraba of Botswana</h1>
      <p class="tagline">The cattle game &middot; Tshameka Mmele</p>
    </section>
    <img class="feature" src="/img/feature-graphic.jpg" width="1024" height="500" alt="The Mmele board over an Okavango Delta sunset, with the words: Mmele, Morabaraba of Botswana. Place your cows, form a mill, shoot.">
    <p>Mmele brings Morabaraba, the traditional cattle game of Botswana and Southern Africa, to your phone. Place your
    twelve cows, slide them across the board, line up three in a row to form a mill and shoot one of your opponent&rsquo;s
    cows, or play Khutlotharo, the quick three-cow triangle game. Play the computer at three levels, pass the phone to a
    friend, or play online with a six-letter room code, on boards set against the Okavango Delta, Chobe, the
    Makgadikgadi Pans, the Kalahari and the Tsodilo Hills. No ads, no purchases and no sign-up.</p>
    <p class="center"><span class="badge">Coming soon to Google Play</span></p>
    <ul class="cards">
      <li><h2>Two boards</h2><p>Classic Morabaraba (24 points, 12 cows) and Khutlotharo (7 points, 3 cows).</p></li>
      <li><h2>Play your way</h2><p>Against the computer, Pass &amp; Play on one phone, or online with a friend.</p></li>
      <li><h2>Private by design</h2><p>Offline games never leave your phone. Online play uses only a random anonymous ID.</p></li>
    </ul>
    <h2>Legal</h2>
    <ul>
      <li><a href="/privacy">Privacy policy</a> (including <a href="/privacy#delete">how to delete your online data</a>)</li>
      <li><a href="/terms">Terms of use</a></li>
      <li>Contact: <a href="mailto:{email}">{email}</a></li>
    </ul>
""".replace("{email}", EMAIL)

NOT_FOUND = """    <h1>Page not found</h1>
    <p class="meta">Error 404</p>
    <p>Sorry, there&rsquo;s no page at this address. Like a cow that wandered off the board.</p>
    <ul>
      <li><a href="/">Mmele home</a></li>
      <li><a href="/privacy">Privacy policy</a></li>
      <li><a href="/terms">Terms of use</a></li>
    </ul>
"""

if __name__ == "__main__":
    page("index.html", "Mmele: Morabaraba of Botswana",
         "Mmele is Morabaraba, the traditional cattle board game of Botswana, for Android: vs computer, pass & play and online. Coming soon to Google Play.",
         LANDING, "home")
    legal("privacy.html", "PRIVACY_POLICY.md", "privacy", "How the Mmele app handles data, and how to delete your online data.")
    legal("terms.html", "TERMS.md", "terms", "Terms of use for the Mmele app.")
    page("404.html", "Page not found | Mmele", "This page does not exist.", NOT_FOUND)
