# CineJoy Android

An unofficial Android WebView wrapper for https://cinejoy.pk/.

## Included
- Android app source in `app/`
- Popup/new-window suppression using WebView settings
- Best-effort blocking of selected third-party ad/tracker hosts
- Android Safe Browsing where supported
- Fullscreen HTML5 video and landscape playback
- GitHub Actions workflow at `.github/workflows/android.yml`

## Build an APK
Every push to `main` triggers the **Build CineJoy APK** workflow. When it finishes successfully, open the workflow run under the repository's **Actions** tab and download the `CineJoy-debug-apk` artifact. You can also start a build manually using **Run workflow**.

## Limitations
Filtering is best-effort: it does not block every ad or popup, and blocking third-party hosts may affect site features. This app does not bypass DRM, subscriptions, logins, geographic restrictions, or other access controls. It is not affiliated with or endorsed by CineJoy.