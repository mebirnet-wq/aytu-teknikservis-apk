# AYTU Bobinaj Teknik Servis — Android (Kotlin)

Native Android app project (no Flutter / React Native). The app opens the existing WordPress portal at `https://aytubobinaj.com/teknikservis/` inside a restricted WebView.

## Current configuration

- Application ID: `com.aytubobinaj.teknikservis`
- Debug application ID: `com.aytubobinaj.teknikservis.debug`
- Version: `1.0.0` (debug display suffix `-debug`)
- Minimum Android: 9 / API 28
- Compile / target SDK: API 35
- JDK: 17
- Language: Kotlin
- Internet permission only; no direct-call or SMS-send permission

## Security and behavior

- HTTPS only; cleartext HTTP disabled.
- Only `aytubobinaj.com` and the `/teknikservis/` portal are accepted as normal WebView navigation.
- WordPress login/logout routes are allowed only through `wp-login.php` and a limited list of authentication actions. `admin-ajax.php` is allowed for the existing portal's AJAX requests.
- Other hosts, paths and schemes are blocked. `tel:`, `sms:` and `smsto:` links are handed to the Android phone/SMS app; the app does not start calls or send SMS automatically.
- SSL certificate errors are always cancelled, never bypassed.
- WordPress cookies are used so the existing WordPress login/session flow can be reused. No second password store is added to the APK.
- JavaScript is enabled because the current portal uses AJAX. File/content access, mixed content and popups are disabled.

## Build on GitHub Actions

1. Create a GitHub repository and upload the project files.
2. Open **Actions → Android APK → Run workflow** (or push to `main` / `master`). The workflow provisions Gradle 8.9 and the Android SDK.
3. Download the `aytu-bobinaj-debug-apk` artifact from the completed workflow run.

## Release signing

A release APK is not signed by this workflow yet. Do not commit keystore files or passwords. For a production release, configure `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD` as GitHub Actions secrets and add a release-signing step. Keep the keystore backup in a secure location separate from GitHub.

## Important limitation of the first version

This is a WebView wrapper around the existing portal, not a new native REST client. It depends on the site's existing portal and WordPress server-side capabilities. It does not itself add server-side administrator/technician permissions or an audit-log screen. Those require a compatible, reviewed update to the existing WordPress plugin and a live-site test. No live site has been modified by creating this project.

## Test checklist

- [ ] Confirm portal login/logout on a test WordPress account.
- [ ] Confirm all required portal routes remain within the allowlist. If the current plugin legitimately redirects to another WordPress path, review that exact path before adding it.
- [ ] Confirm phone links open the dialer with a number prefilled.
- [ ] Confirm SMS links open the SMS composer with recipient/body prefilled and do not auto-send.
- [ ] Test offline mode, back navigation, certificate errors and different screen sizes on a physical Android 9+ device.
- [ ] Build and install the APK from the GitHub Actions artifact.
