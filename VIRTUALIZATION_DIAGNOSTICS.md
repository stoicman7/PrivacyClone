# Virtualization diagnostics

This document describes a controlled comparison using the independent Virtualization Probe APK. It is for diagnosing PrivacyClone's own runtime compatibility, not for testing or bypassing another app's security controls.

## What the probe measures

The probe runs as an ordinary app and prints a local-only report containing:

- Android release, API level, security patch and build display.
- Manufacturer, device model, process bitness and supported ABIs.
- The process UID and the UID reported by PackageManager.
- The app's APK, data, files, cache and native-library paths.
- Whether Google Play services and the Play Store are visible to PackageManager.
- The current WebView provider returned by Android.
- Hardware feature availability, display metrics, sensor count and memory.

It deliberately does not read Android ID, advertising IDs, IMEI/IMSI, serial number, MAC/BSSID, IP address or location. The probe has no Internet permission and requests no runtime permissions. It does not contact a server or make a security/risk decision.

UID values are reported through Android APIs. A virtualization framework can intercept those APIs, so they are not proof of the kernel-level UID or SELinux identity.

## Native library packaging

VirtualApp 0.22 uses `NativeEngine.enableIORedirect()`, which requires `libva++.so` as a real file under Android's `nativeLibraryDir`. The host enables `packaging.jniLibs.useLegacyPackaging = true` so Android extracts native libraries during install. CI inspects the built APK and verifies that `lib/arm64-v8a/libva++.so` and `lib/x86_64/libva++.so` are present and compressed for extraction. If the host still reports the file missing after installing the newly built APK, collect the exact PrivacyClone version and the current startup message; do not disable the preflight check.

## Build artifacts

The Android CI workflow builds two APKs:

- PrivacyClone-debug-apk: the virtualization host.
- Virtualization-Probe-debug-apk: the independent test application.

Both are debug builds. Do not use a debug build as a production release.

## Same-phone comparison procedure

1. Install probe-debug.apk normally on the phone.
2. Open Virtualization Probe, tap COPY REPORT, and save the copied report as the normal result.
3. Open PrivacyClone and choose SELECT OR CLONE INSTALLED APP.
4. Select Virtualization Probe and wait until the clone appears.
5. Open that clone, tap COPY REPORT, and save this as the virtual result.
6. Compare the reports field by field. Data and cache paths should normally differ. Manufacturer, model, Android build, supported ABIs and physical features should describe the same phone. Check process bitness, both UID values, Play services visibility and WebView provider carefully.
7. Repeat after a phone reboot if startup or service availability is inconsistent.

Do not use HelloTalk or a real account as the diagnostic test. HelloTalk's own FAQ says it runs automated checks for accounts registered through emulators or modification tools and advises users not to register through virtual devices: https://www.hellotalk.com/en/faq/account/206.

## Interpreting likely outcomes

- Probe opens normally and as a clone: the basic Activity lifecycle and app-data container work for this test app. This does not prove compatibility with every app.
- Play services or WebView differs: record the difference as a possible compatibility dependency. It is not by itself proof of the cause of another app's behavior.
- Process bitness is 32-bit or the host reports no libva++.so: this build's native engine requirements are not met. The host now reports an explicit startup error instead of attempting to initialize an unsupported native runtime.
- Probe itself fails to launch in the clone: capture the exact PrivacyClone stage/error and Android version; investigate the VirtualApp activity/package bridge before changing anything else.
- HelloTalk displays Intelligent Detection but the probe works: this points away from a basic clone-launch failure and is consistent with HelloTalk's documented policy against registration through virtual devices. It does not identify which internal signal or SDK produced the decision.

## Compatibility boundary

The pinned VirtualApp 0.22.0 source is old and its documented Android support range does not guarantee compatibility with every modern app, Google component, vendor firmware or security service. Keep compatibility work focused on documented Android behavior, startup stability, permission handling, split APK installation, and correct lifecycle/service operation. Do not alter reported device identity or attempt to defeat another app's anti-abuse decision.
