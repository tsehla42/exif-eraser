# App Store Submission Guide

How to submit exif-eraser to Orion Store, IzzyOnDroid, and F-Droid.

## Prerequisites (do once, before any submission)

### 1. Add your app icon

Rasterize the vector icon to 512x512 PNG and save as:
```
fastlane/metadata/android/en-US/images/icon.png
```

### 2. Take screenshots

Take 2-5 screenshots of the app and save them as:
```
fastlane/metadata/android/en-US/images/phoneScreenshots/1.png
fastlane/metadata/android/en-US/images/phoneScreenshots/2.png
...
```

See `NAMING.txt` in that directory for naming rules.

### 3. Commit and push

```bash
git add fastlane/ docs/app-stores/
git commit -m "Add app store metadata for Orion Store, IzzyOnDroid, and F-Droid"
git push
```

Make sure a GitHub Release exists with an APK (v1.0.0 already has this).

---

## Orion Store (easiest, do first)

**What:** Serverless open-source app store. Pulls APKs from GitHub Releases.

**Steps — submit from inside the Orion Store app.** The maintainer rejects hand-written
issues ("submit from the Orion Store app without manual edits"), so do not paste JSON into
a GitHub issue by hand.

1. Install Orion Store, open it → **Submit app**
2. Choose **From repo**, enter `https://github.com/tsehla42/exif-eraser`
3. Orion auto-detects name, package name, icon and screenshots straight from the repo's
   `fastlane/metadata/android/en-US/` paths — check they fit
4. It auto-picks a wrong Category (it chose *Media*); set it yourself — we used **Privacy**
5. Fill **Description** (the only field it does not detect)
6. Tap **Generate GitHub issue** → review the prefilled form → **Create**

**Known bug in v2.7.5:** `enc()` leaves `*` unescaped, so GitHub answers **HTTP 500** on the
generated link. Workaround: percent-encode the footer asterisks as `%2A`. Reported in
RookieEnough/Orion-Store#216, fix in PR #217.

**Timeline:** Days. Manual review for provenance and project health.

---

## IzzyOnDroid (medium effort)

**What:** Third-party F-Droid repo. Distributes developer-signed APKs. Updates within 24h.

**Steps:**
1. Open: https://codeberg.org/IzzyOnDroid/repodata/issues/new?template=.forgejo/issue_template/app-inclusion-request.yaml
2. Fill in the template:
   - Source code: https://github.com/tsehla42/exif-eraser
   - License: MIT
   - Category: Security or System
   - Summary and description (use the fastlane descriptions)
   - Build instructions: `./gradlew assembleRelease`
3. **AI disclosure:** They have a strict anti-AI policy. Be honest about AI usage.
4. Submit and wait for community review (binary scan, on-device testing)

**Timeline:** Days to weeks. They test the APK on real devices.

**Gotchas:**
- Must credit upstream (Tommy-Geenexus/exif-eraser) — already done in README
- APK must be release-signed, not debug
- APK size limit ~30MB (ours is 4.5MB, fine)

---

## F-Droid (most work, most prestigious)

**What:** Main FOSS app repository. Rebuilds APKs from source and signs with their keys.

**Steps:**
1. Register on GitLab, then fork https://gitlab.com/fdroid/fdroiddata
2. Create a branch named after the app id: `com.tsehla.exiferaser`
3. Add `metadata/com.tsehla.exiferaser.yml` — copy `docs/app-stores/fdroid-metadata.yml`
4. Point `commit:` at the **full 40-hex hash** of the release tag (not the tag name):
   ```bash
   git rev-parse v1.0.0
   ```
5. Push to **your fork**, then check the CI pipeline on your fork is green — that pipeline
   *is* the lint step, and it must pass before the MR
6. Open the Merge Request at https://gitlab.com/fdroid/fdroiddata/-/merge_requests and fill in
   the template (one branch per app; do not MR from `master`)
7. Optionally file an RFP first — CONTRIBUTING recommends it for first-time contributors:
   https://gitlab.com/fdroid/rfp/issues

**Timeline:** Days to weeks for metadata review. Then ~24-48h after merge to appear.

**Gotchas:**
- `commit:` must be a full hash — *"Don't use branch name or tag name."*
- `gradle:` must be `[yes]` when there is no flavor; `[full]` runs a nonexistent
  `assemblefullRelease` and fails the build
- `Categories` must exist in fdroiddata's `config/categories.yml` — there is no
  `Utilities` (we use `Security` + `Graphics`)
- Build must be reproducible from source; no committed .jar/.aar blobs (we're clean)
- No Firebase/GMS/tracking (we're clean)
- Fork needs a different Application ID from upstream (ours is `com.tsehla.exiferaser` — fine)

---

## Recommended Order

1. **Orion Store** — fastest: submit from inside the app, it detects the repo metadata
2. **IzzyOnDroid** — reuse fastlane metadata, file the inclusion issue on Codeberg
3. **F-Droid** — reuse fastlane metadata, push the prepared fdroiddata branch and open the MR

The Fastlane structure created here satisfies all three stores.
