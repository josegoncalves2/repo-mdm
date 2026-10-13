# HWMDM Web Filter companion

This is a separate Android app (`com.hwmdm.webfilter`). It applies website rules as native managed settings in the browser. It does not use `VpnService` and is not a second Device Owner.

## Authority and bootstrap

The launcher remains Device Owner. After it installs the companion, it verifies the package signature and grants only `DELEGATION_APP_RESTRICTIONS`. The companion then calls `DevicePolicyManager.setApplicationRestrictions()` for supported browser packages. The launcher source contains this handoff in `android-source/app/src/main/java/com/hmdm/launcher/util/Utils.java`.

The stock `hmdm-6.36-os.apk` does not contain that handoff. Devices using the separate agent need a launcher build containing the delegation change, signed with the same project certificate as this APK. The build in `android-source/` is version 1.11 and includes that change. Its certificate differs from the stock 6.36 certificate, so Android will not install it as an in-place update over a device currently enrolled with the stock binary; it is for bootstrap/provisioning with the project signing key.

## Policy delivery

When this app is selected in a device profile, the Web Filter sync hook sends merged browser settings in the signed `webfilterBrowserPolicies` field. The launcher ignores that field; the companion fetches the signed sync response and applies it. A config update asks the companion to refresh immediately; a 15-minute periodic sync is the fallback.

Current browser mappings:

- Chrome stable, beta, dev and canary use Chromium `URLBlocklist` and `URLAllowlist` managed settings.
- Microsoft Edge for Android uses its own managed restriction keys.

Other browsers need a documented managed-configuration schema and a package-specific mapping in the server plugin. An arbitrary browser does not automatically understand Chrome policies.

The accessibility service is used only to detect Chrome's native blocked page and display/report the configured block page. Native URL policy enforcement itself does not depend on accessibility being enabled.

## Build

From this directory, run `./gradlew --offline :app:assembleRelease`. Output: `app/build/outputs/apk/release/app-release.apk`.
