# RikkaNap — Project Notes

This branch is documentation-only. Keep implementation changes on `master`; use this branch as the durable handoff log and project notebook.

## Current objective

Replace remaining RikkaHub visual branding and image fallback artwork with the exact artwork supplied by the project owner. Never invent or substitute a logo.

## Non-negotiable asset mapping

- `launchericon.png` is the **launcher/app icon source**. It supplies the home-screen icon, adaptive launcher icon, app drawer/recent-app identity, and related in-app launcher branding.
- `noticon.png` is the **notification icon source only**. It is not the launcher icon. Use it for the Android status-bar/notification small icon and corresponding notification-specific resources.
- Keep those two identities separate. Do not use noticon for launcher artwork or launchericon as a replacement for the notification glyph.
- Preserve the exact supplied artwork. Resize/crop/derive platform-specific variants only as technically necessary; do not redesign, reinterpret, or generate alternative art.
- **Launcher adaptive-icon background must be solid `#000000`** (not sampled from the logo or replaced with another color).
- Keep launcher artwork crisp: no blur, softening, glow, or low-resolution upscaling. Use proper-resolution exports and preserve sharp edges.
- The notification glyph must use the available notification canvas fully: scale/crop the actual `noticon.png` artwork so it visibly fills the notification small-icon area rather than appearing tiny with excessive padding. Apply Android-required monochrome/alpha treatment without changing its identity.
- Replace old placeholder/fallback image artwork with coordinated RikkaNap visuals based on the supplied brand assets; retain functional fallback behavior.
- Do not overwrite AI provider icons in `app/src/main/assets/icons/`.
- Do not ship the rejected generated sleeping-cat/crescent concept.

## Repository

- GitHub: https://github.com/Cutie-Cookie/rikkanap
- Implementation branch: `master`
- Notes branch: `project-notes`
- App display name: RikkaNap
- Application ID: `com.cutiecookie.rikkanap`
- Master HEAD when notes branch was created: `8872d68ada7882e36989731d05dcb09f8aa8e537`

## Asset inventory to audit

**Launcher artwork (use launchericon):**
- `launchericon.png` (root)
- `docs/icon.png` (now replaced with the exact `launchericon.png` bytes)
- `app/src/main/res/drawable-nodpi/ic_launcher_foreground.png`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_{background,foreground,monochrome}.png`
- `app/src/main/res/mipmap-nodpi/ic_launcher.png`

**Notification artwork (use noticon):**
- `noticon.png` (root)
- `app/src/main/res/drawable-nodpi/ic_stat_rikkahub.png`

**Image fallbacks:**
- `app/src/main/res/drawable/placeholder.png`
- `app/src/main/res/drawable/placeholder_dark.png`

Search source for all usages and any additional old RikkaHub visual references.

## Build context

- ARM64 workflow: `.github/workflows/build-arm64-apk.yml`
- Workflow builds `assembleGithubRelease` and uploads `rikkanap-arm64-v8a`.
- Previous successful run `46090391672` predates this work and is not the final deliverable.
- Previous artifact `15241381314` is stale for this task.
- Signing secrets are configured by the owner; never expose or commit signing material.

## Work log

### 2026-11-28 — Initial handoff and correction

- Confirmed old launcher artwork remains in Android mipmap resources and adaptive icon XML points to legacy foreground artwork.
- Identified placeholder WebP resources and notification-specific drawable.
- A generated alternative launcher concept was mistakenly proposed and explicitly rejected. It must not be used.
- Owner clarified exact mapping: `launchericon.png` is for all launcher/app icon uses; `noticon.png` is for notifications only.
- Branding and fallback implementation is now committed on `master`; see the dated work log below.
- A temporary unreferenced Git tree object from an abandoned attempt was never committed or pushed.

## Remaining tasks

1. Inspect the exact binary contents of `launchericon.png` and `noticon.png`; confirm dimensions, transparency, and visual appearance. If repository copies are not the owner's supplied files, recover the original uploads rather than guessing.
2. Review remaining old artwork and ensure provider logos are untouched.
3. Derive density-correct launcher/adaptive/monochrome resources exclusively from launchericon. Keep the adaptive background exactly `#000000`, maintain valid safe zones, and avoid blur/softening.
4. Derive the notification small icon exclusively from noticon, using Android-compatible monochrome/alpha treatment as required; scale/crop to fill the available glyph area without clipping important artwork.
5. Confirm the renamed PNG fallback resources resolve through the existing `R.drawable.placeholder` / `R.drawable.placeholder_dark` references.
6. Search for remaining old RikkaHub visual assets while preserving provider logos.
7. Review the diff for accidental deletions, wrong source mapping, and rejected generated art.
8. Monitor the new ARM64 workflow; verify APK signature and inspect the packaged resource output.
9. Verify the APK artifact is from the post-branding commit and report its exact workflow/artifact link.

## Update protocol

Append dated work-log entries for each meaningful change. Keep remaining tasks current. Record commit SHAs, workflow run IDs, artifact IDs, test results, and fixes. Never put secrets in this file.


### 2026-11-28 — Owner's explicit visual requirements

- Launcher adaptive-icon background: solid `#000000`.
- Launcher icon must remain crisp; no blur or softening.
- Notification icon must fill the notification icon area, avoiding excessive transparent padding.
- The current adaptive icon XML already points to `@color/black`, whose value is `#FF000000`; retain this exact background.

### 2026-11-28 — First verified source-asset propagation

- Retrieved the repository's `launchericon.png` as base64 from GitHub and used its exact bytes; no generated substitute was used.
- Applied those exact launcher source bytes to mipmap foreground assets at mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi, and mipmap-nodpi.
- Retrieved `noticon.png` separately and applied its exact bytes to `drawable-nodpi/ic_stat_rikkahub.png` (notification-only).
- Implementation commit on master: `0e1471b7856c1d3f848365cfe32cbbcdd524f012`.
- The ARM64 workflow is configured to trigger on changes under `app/src/main/res/**`; check the resulting run.
- Still outstanding: adaptive background/monochrome treatment, verify actual in-app logo references, replace placeholder.webp and placeholder_dark.webp with appropriate image assets, review remaining old artwork, verify build artifact. Do not mark complete until tested.

### 2026-11-28 — Verified visual asset propagation and fallback replacement

- Inspected the supplied `launchericon.png`: the exact image is the RikkaNap sleeping-cat artwork on black. Its repository SHA is `1883baaaecdd37aa34a38e9299233d2997bd5f6c`.
- Confirmed the adaptive icon XML uses `@color/black`, and `colors.xml` defines that as `#FF000000`.
- Confirmed `drawable-nodpi/ic_launcher_foreground.png`, the mipmap foregrounds, and `mipmap-nodpi/ic_launcher.png` all use the exact launcher source bytes (same blob SHA); no generated logo or blur treatment.
- Confirmed `drawable-nodpi/ic_stat_rikkahub.png` uses the separate exact `noticon.png` source bytes (same blob SHA as root `noticon.png`). The notification source is 736×736 and visually fills its square artwork canvas.
- Replaced generic `placeholder.webp` and `placeholder_dark.webp` with `placeholder.png` and `placeholder_dark.png`, both using exact launcher artwork. The drawable resource names remain `placeholder` and `placeholder_dark`, so existing resource references remain valid.
- Replaced legacy bunny artwork in `docs/icon.png` with the exact launcher source. Provider icons under `app/src/main/assets/icons/` were not changed.
- Audited `app/src/main/assets/icons/rikkahub.svg`: `AIIconMatcher.kt` selects it for provider/model names matching `rikka|auto`, so it is a provider identity asset, not the app launcher logo; it was intentionally preserved.
- Master commits: `2887991937bbfea56ce67c7e69d4fad066ffd7f1` (fallbacks/docs icon), `c2216bff1be5b590487b85cddbcd2273b89f8125` (initial signing checks), `16e3ba4cba5bcc4c972f87be82bce4600d000f90` and `8b7ec2b643cb3b14d26bea50e5b81419ea19b005` (correct signing-property validation).
- The earlier successful build `46131200121` produced artifact `15258086671` from commit `0e1471b7856c1d3f848365cfe32cbbcdd524f012`. Its APK contains an APK Signature Scheme v2 signer; extracted certificate subject is `CN=RikkaNap, OU=Android, O=Cutie-Cookie, L=Vancouver, ST=British Columbia, C=CA`, SHA-256 fingerprint `3A:EA:06:4E:29:E9:C5:8F:D1:32:43:F5:39:90:8B:71:00:44:3C:58:EE:75:2B:76:59:D5:D3:65:0D:78:3B:A4`. (Jarsigner's `unsigned` report is not a valid check for v2-only APK signing; the v2 signer block and certificate were parsed directly.)
- Updated the ARM64 workflow to require all four local.properties signing fields and to run Android `apksigner verify --verbose --print-certs` before uploading. This prevents a future unsigned APK from being uploaded as a successful artifact.
- Current remaining work: wait for the workflow triggered by the latest master commits; verify its signature-check step, inspect the packaged output, and record its new artifact ID.
