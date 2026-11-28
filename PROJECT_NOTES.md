# RikkaNap — Project Notes

This branch is documentation-only. Keep implementation changes on `master`; use this branch as the durable handoff log and project notebook.

## Current objective

Replace remaining RikkaHub visual branding and image fallback artwork with the exact artwork supplied by the project owner. Never invent or substitute a logo.

## Non-negotiable asset mapping

- `launchericon.png` is the **launcher/app icon source**. It supplies the home-screen icon, adaptive launcher icon, app drawer/recent-app identity, and related in-app launcher branding.
- `noticon.png` is the **notification icon source only**. It is not the launcher icon. Use it for the Android status-bar/notification small icon and corresponding notification-specific resources.
- Keep those two identities separate. Do not use noticon for launcher artwork or launchericon as a replacement for the notification glyph.
- Preserve the exact supplied artwork. Resize/crop/derive platform-specific variants only as technically necessary; do not redesign, reinterpret, or generate alternative art.
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
- `docs/icon.png` (verify whether it matches launchericon)
- `app/src/main/res/drawable-nodpi/ic_launcher_foreground.png`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_{background,foreground,monochrome}.png`
- `app/src/main/res/mipmap-nodpi/ic_launcher.png`

**Notification artwork (use noticon):**
- `noticon.png` (root)
- `app/src/main/res/drawable-nodpi/ic_stat_rikkahub.png`

**Image fallbacks:**
- `app/src/main/res/drawable/placeholder.webp`
- `app/src/main/res/drawable/placeholder_dark.webp`

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
- No branding/fallback implementation commit has been made on `master`.
- A temporary unreferenced Git tree object from an abandoned attempt was never committed or pushed.

## Remaining tasks

1. Inspect the exact binary contents of `launchericon.png` and `noticon.png`; confirm dimensions, transparency, and visual appearance. If repository copies are not the owner's supplied files, recover the original uploads rather than guessing.
2. Inspect `docs/icon.png` and current Android resources; compare to launchericon and identify all stale logo artwork.
3. Derive density-correct launcher/adaptive/monochrome resources exclusively from launchericon. Keep adaptive foreground/background composition and safe zones valid.
4. Derive the notification small icon exclusively from noticon, using Android-compatible monochrome/alpha treatment as required.
5. Find all placeholder/fallback usages and replace their image content with cohesive light/dark variants based on the approved launcher identity, retaining fallback logic.
6. Search for remaining old RikkaHub visual assets while preserving provider logos.
7. Review the diff for accidental deletions, wrong source mapping, and rejected generated art; commit the verified changes to master.
8. Monitor the new ARM64 workflow; inspect and fix any failures.
9. Verify the APK artifact is from the post-branding commit and report its exact workflow/artifact link.

## Update protocol

Append dated work-log entries for each meaningful change. Keep remaining tasks current. Record commit SHAs, workflow run IDs, artifact IDs, test results, and fixes. Never put secrets in this file.
