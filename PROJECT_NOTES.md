# RikkaNap — Project Notes

This branch is intentionally documentation-only. Keep implementation changes on `master`; use this branch as the durable handoff log and project notebook.

## Current objective

Replace all remaining RikkaHub visual branding and image fallback artwork with the exact RikkaNap logo artwork supplied by the project owner. Do not invent or substitute a new logo.

## Non-negotiable branding requirements

- Use the owner's previously uploaded logo as the single source of truth.
- Preserve its appearance and identity. Do not redraw it, reinterpret it, or generate an alternative.
- Apply it consistently to Android launcher/adaptive icon assets at every density, monochrome icon, notification icon where appropriate, and in-app logo/branding artwork.
- Replace old placeholder/fallback images with coordinated RikkaNap artwork derived from the supplied logo.
- Keep functional fallback behavior intact; replace the displayed art, not the fallback mechanism.
- Do not overwrite AI provider icons in `app/src/main/assets/icons/`.
- Do not ship the temporary generated sleeping-cat/crescent concept. It was rejected by the owner.

## Repository

- GitHub: https://github.com/Cutie-Cookie/rikkanap
- Main implementation branch: `master`
- App display name: RikkaNap
- Application ID: `com.cutiecookie.rikkanap`
- Current known master HEAD at notebook creation: `8872d68ada7882e36989731d05dcb09f8aa8e537`

## Asset inventory to audit

- `launchericon.png` (root; likely owner-provided launcher artwork — inspect first)
- `noticon.png` (root; inspect as notification artwork)
- `docs/icon.png`
- `app/src/main/res/drawable-nodpi/ic_launcher_foreground.png`
- `app/src/main/res/drawable-nodpi/ic_stat_rikkahub.png`
- `app/src/main/res/drawable/placeholder.webp`
- `app/src/main/res/drawable/placeholder_dark.webp`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_{background,foreground,monochrome}.png`
- `app/src/main/res/mipmap-nodpi/ic_launcher.png`
- Search source for all usages of the above assets and any remaining RikkaHub-specific image references.

## Known app/build context

- Android ARM64 workflow: `.github/workflows/build-arm64-apk.yml`
- Workflow builds `assembleGithubRelease` and uploads artifact `rikkanap-arm64-v8a`.
- Previous successful build run `46090391672` predates the requested branding/fallback work and is NOT the final deliverable.
- Previous artifact ID `15241381314` is stale for this task and must not be presented as the updated APK.
- Signing secrets were configured by the owner; do not expose or commit signing material.

## Work log

### 2026-11-28 — Initial handoff

- Confirmed old launcher artwork still exists in the Android mipmap resource set.
- Confirmed adaptive icon XML referenced the legacy drawable foreground and black background.
- Identified the placeholder WebP resources and notification icon resource for replacement.
- A generated alternative icon concept was mistakenly proposed; owner explicitly rejected it. It must not be used.
- No branding/fallback implementation commit has been made on `master` yet.
- A temporary unreferenced Git tree object may exist from an abandoned attempt; it was never committed or pushed to a branch.

## Remaining tasks

1. Locate and verify the exact owner-supplied logo bytes. Inspect `launchericon.png`, `noticon.png`, and `docs/icon.png`; if the actual upload is not present in the repository, recover it from the original conversation attachment or ask the owner for that one file. Never substitute generated artwork.
2. Inspect image dimensions, transparency, safe areas, and visual appearance before preparing derivatives.
3. Generate density-correct launcher/adaptive/monochrome/notification assets from the verified source logo. Keep notification artwork Android-compatible and legible.
4. Identify all source references to placeholder/fallback resources and replace their image content with consistent, light/dark-appropriate variants derived from the supplied identity.
5. Search for any remaining old RikkaHub logo artwork in app UI, docs, and packaging; change visual assets only where appropriate, preserving provider logos.
6. Review the complete diff and ensure no generated rejected artwork or accidental deletions are included.
7. Commit the asset replacement to `master`.
8. Monitor the new ARM64 GitHub Actions run triggered by the commit; inspect job logs if it fails.
9. Verify the new artifact belongs to the post-branding commit, then report the exact run/artifact link and status.

## Update protocol

For each meaningful change, append a dated entry under Work log and update Remaining tasks. Record commit SHAs, workflow run IDs, artifact IDs, failures, and fixes. Keep secrets out of this file.
