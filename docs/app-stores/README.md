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

**Steps:**
1. Open: https://github.com/RookieEnough/Orion-Data/issues/new
2. Title: `App Submission [1 App]`
3. Body: Copy the JSON from `docs/app-stores/orion-submission.json`
4. Update the screenshot URLs if you added more than 2
5. Submit and wait for maintainer review

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
1. Fork https://gitlab.com/fdroid/fdroiddata
2. Create branch named `com.tsehla.exiferaser`
3. Add `metadata/com.tsehla.exiferaser.yml` using the template in `docs/app-stores/fdroid-metadata.yml`
4. Replace the `commit` field with the full commit hash of v1.0.0:
   ```bash
   git rev-parse v1.0.0
   ```
5. Test locally (optional but recommended):
   ```bash
   fdroid lint com.tsehla.exiferaser
   fdroid rewritemeta com.tsehla.exiferaser
   fdroid build com.tsehla.exiferaser
   ```
6. Open Merge Request at https://gitlab.com/fdroid/fdroiddata/-/merge_requests with label `New App`
7. Optionally file RFP at https://gitlab.com/fdroid/rfp/issues to notify community

**Timeline:** Days to weeks for metadata review. Then ~24-48h after merge to appear.

**Gotchas:**
- Build must be reproducible from source
- No committed .jar/.aar blobs (we're clean)
- No Firebase/GMS/tracking (we're clean)
- Fork needs different Application ID (ours is `com.tsehla.exiferaser`, upstream is different — fine)

---

## Recommended Order

1. **Orion Store** — fastest win, just an issue with JSON
2. **IzzyOnDroid** — reuse fastlane metadata, file issue on Codeberg
3. **F-Droid** — reuse fastlane metadata, create fdroiddata MR

The Fastlane structure created here satisfies all three stores.
