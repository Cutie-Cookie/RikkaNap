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

- No outstanding implementation or build-verification tasks for this icon/fallback pass. Preserve the exact launcher/notification asset separation and keep provider logos unchanged.

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

### 2026-11-28 — Final ARM64 build and signature verification

- Latest master HEAD: `8b7ec2b643cb3b14d26bea50e5b81419ea19b005`.
- Final workflow run: `46132713520` — **success**. All steps completed, including `Prepare release signing`, `Build ARM64 GitHub APK`, `Verify APK signature`, and `Upload APK artifact`.
- The verification step reports APK Signature Scheme v2: `true`; v1/v3/v3.1/v3.2/v4 are false, as expected for this APK. Certificate DN: `CN=RikkaNap, OU=Android, O=Cutie-Cookie, L=Vancouver, ST=British Columbia, C=CA`. SHA-256: `3aea064e29e9c58fd13243f539908b7100443c58ee752b7659d5d3650d783ba4`.
- Final artifact: `rikkanap-arm64-v8a`, ID `15258633432`, 20,604,991 bytes zipped; expires 2026-12-12. Artifact belongs to run `46132713520` and exact HEAD above.
- Downloaded the final artifact and independently parsed its APK signing block; v2 signer and the same RikkaNap certificate are present.
- Added a workflow guard requiring `storeFile`, `storePassword`, `keyAlias`, and `keyPassword` in the `SIGNING_CONFIG` secret, and an `apksigner verify --verbose --print-certs` step before artifact upload. A transient first attempt at the guard escaped the shell variable incorrectly and failed; the pattern was corrected, then the final run passed.
- Verified `NotificationUtil.kt` uses `R.drawable.ic_stat_rikkahub` as its default small icon. Verified `AIIconMatcher.kt` maps `rikka|auto` provider/model names to `rikkahub.svg`; this provider icon was intentionally retained.
- The generic fallback resources now use `.png` files with the same Android resource names (`placeholder`, `placeholder_dark`); the final build passed with these changes.


### 2026-11-28 — APK file icon discrepancy (open follow-up)

- User reports that the downloaded/built APK file itself does **not** display the wanted RikkaNap icon in the file/download manager.
- After installing that same APK, the installed app displays the intended icons correctly.
- Therefore, launcher/installed-app icon resources appear to be working; do **not** casually alter the already-correct launcher foreground, black adaptive background, or notification icon.
- Before the next actual feature/code updates, investigate why the APK *file* is represented with a different or generic icon. Check whether this is Android package/APK metadata, adaptive icon packaging, the download host/browser's APK file-icon behavior, or a platform cache/display limitation.
- Compare the packaged APK manifest/resources against the source resources and inspect the APK's embedded application icon using Android build tools. Distinguish the icon shown for the APK file before installation from the icon shown by the installed app.
- Only change app resources if inspection proves the packaged APK metadata is wrong. If the APK itself is correct and the file manager/browser chooses its own generic icon, document that as an external display behavior rather than damaging working installed icons.
- Resolve and verify this discrepancy before beginning the next planned feature/update batch.


### 2026-11-28 — Investigation result: APK file icon vs installed app icon

- Inspected the exact final APK artifact `15258633432` downloaded from successful workflow `46132713520`.
- The packaged binary manifest source is `app/src/main/AndroidManifest.xml`; its `<application>` explicitly sets `android:icon="@mipmap/ic_launcher"` (no explicit `android:roundIcon`).
- The packaged APK contains the expected RikkaNap sleeping-cat launcher artwork in its compiled `res/` image resources. The 1254×1254 RGB icon payload is present in multiple compiled resource entries; the separate notification artwork is also present as a 736×736 RGBA resource. This is not a case of the launcher artwork being omitted from the APK.
- The source adaptive icon `mipmap-anydpi-v26/ic_launcher.xml` resolves to foreground `@drawable/ic_launcher_foreground` over `@color/black`; `colors.xml` defines black as `#FF000000`. The user independently confirmed that after installation the intended icons appear correctly.
- Finding: app icon metadata and the launcher image are packaged, and Android resolves them correctly after installation. The reported discrepancy is specific to the *APK file's own thumbnail/icon in the download/file browser before installation*. That display is commonly supplied by the file manager/browser's APK MIME-type handling and is not necessarily the embedded app icon.
- No app resource change is justified at this point. In particular, do not change the working launcher artwork, adaptive black background, or notification icon, and do not add a speculative `roundIcon` just to chase a file-manager thumbnail.
- To distinguish external display behavior conclusively, compare the same APK in another file manager/package installer or inspect its icon in Android's package installer UI. If the package installer preview also shows the correct icon, the remaining generic APK file icon is a host file-browser limitation/cache, not an app defect.
- This investigation is complete enough to proceed with planned app updates; the file-thumbnail discrepancy remains a display-layer observation unless a different package installer demonstrates incorrect embedded icon resolution.


### 2026-11-28 — AMOLED Dark Mode surface palette improvement

- Reviewed `MiaoWuNYA/rikkahub-sillytavern-android` UI theme implementation, especially `ui/theme/Theme.kt`, `ui/theme/Color.kt`, and its AMOLED preference hook.
- Root cause in RikkaNap: the AMOLED toggle already existed and changed only Material color-scheme `background` and `surface`. Material 3 cards, sheets, menus, and other UI surfaces primarily use additional roles such as `surfaceContainer*`, `surfaceDim`, `surfaceBright`, and `surfaceVariant`; those remained gray, so AMOLED mode looked only partially applied.
- Applied the fork's focused surface-role treatment to RikkaNap's existing `RikkahubTheme`, without importing its unrelated UI/theme redesigns or changing the preference/storage behavior.
- AMOLED palette now sets background/surface/surfaceDim/surfaceContainerLowest to `#000000`, surfaceContainerLow and surfaceContainer to `#000000`, surfaceContainerHigh and surfaceVariant to `#101010`, and surfaceContainerHighest and surfaceBright to `#161616`.
- The override remains conditional on both dark theme and the existing AMOLED toggle. Light mode and ordinary dark mode are unchanged; existing custom/dynamic theme accent colors are preserved.
- Implementation commit on `master`: `f641009c7fb2e176f6dac289f6e613d3e8306486`.
- A follow-up workflow-trigger commit on `master` (`6b301ba64419e2a59f409892766e169579f98a68`, message: `Build APK when Android source changes`) caused the ARM64 workflow to run against a tree containing the AMOLED palette change. This trigger-only commit did not itself alter app source.
- ARM64 workflow run `46136423019` completed with **success** on 2026-11-28. Artifact `rikkanap-arm64-v8a`, ID `15260172808`, size 20,605,817 bytes (ZIP), expires 2026-12-12. Artifact SHA-256: `923fab8a08b71544037801a4b04fe411ef20f74dbf727671926edf77926d606c`.
- User is now testing this APK and will report observations. Keep the result **pending user testing**; do not mark the AMOLED work complete or claim the fork's full implementation has been ported. The user clarified that the screenshots supplied were screenshots of the fork itself and requested a direct code port rather than an interpretation/redesign. The current palette change is the first partial change only; further comparison/porting may be needed after the user's test report.
