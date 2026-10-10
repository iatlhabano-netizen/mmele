# Mmele legal site (static, for Vercel)

Plain HTML + one stylesheet. No JavaScript, cookies, analytics, trackers or external fonts, so the site itself
collects nothing (Vercel still keeps its own standard request logs for the deployment).

| URL | File | Source |
|---|---|---|
| `/` | `index.html` | `build_pages.py` (landing text lives there) |
| `/privacy` (`/privacy#delete` = "Delete your online data") | `privacy.html` | `docs/release/PRIVACY_POLICY.md` |
| `/terms` | `terms.html` | `docs/release/TERMS.md` |
| anything else | `404.html` | `build_pages.py` |

`vercel.json` turns on `cleanUrls` (so `/privacy` serves `privacy.html` and `/privacy.html` redirects to it),
`trailingSlash: false`, and strict security headers (CSP allows only this site's own images and CSS).
`.vercelignore` keeps the tooling below out of the deployment.

## Edit and preview

```bash
python3 -m pip install markdown        # once; only needed to regenerate pages
python3 site/build_pages.py            # after editing the policy/terms Markdown or the landing text
python3 site/preview.py                # http://localhost:8000/ with the same clean-URL/404 behaviour as Vercel
```

## Placeholders

```bash
site/set-contact.sh --check                                     # what still needs filling
site/set-contact.sh --email you@example.com --address "Plot …, P.O. Box …"
site/set-domain.sh your-project.vercel.app                      # once the site is live: app + docs URLs
```

`set-contact.sh` replaces `CONTACT_EMAIL_PLACEHOLDER` / `POSTAL_ADDRESS_PLACEHOLDER` in the site, the docs and
`AppInfo.CONTACT_EMAIL`. `set-domain.sh` points `AppInfo.PRIVACY_URL`/`TERMS_URL` (and the docs) at the site.
Both touch app code, so rebuild the AAB afterwards. Commit the result.

## Deploy (you)

Vercel → Add New Project → import the repo (or `vercel` CLI from this folder) → **Root Directory: `site`**,
Framework Preset: **Other**, Build Command: *(empty)*, Output Directory: `.`. After deploying, check
`https://<domain>/privacy#delete`, then run `site/set-domain.sh <domain>`.
