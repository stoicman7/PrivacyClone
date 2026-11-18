# PrivacyClone

A lightweight, local-first Android app-isolation and privacy-diagnostics prototype, designed to run on Android 8.0+ (API 26+), including Android 8.1 devices.

## Current state: prototype 0.3.0

Included in this first build:

- Native Android managed-profile provisioning entry point with explicit user confirmation.
- Profile-owner / managed-profile status checks.
- A profile-aware installation path: on Android 9+ it can request installation of an existing package through the profile-owner API; on Android 8.x it opens the managed-profile Play Store for user-driven installation.
- A launcher list for apps installed in the current profile.
- A local audit of selected sensitive runtime permissions granted to apps visible in the current profile.
- Per-app phone-permission controls for managed-profile apps that request `READ_PHONE_STATE`: a profile owner can deny requested Android Phone-group runtime permissions or return permission control to the user.
- Local display of Android ID availability without displaying, storing, or transmitting its value.
- VPN transport detection using Android network capabilities.
- Sensor inventory and device / OS diagnostics.
- A privacy configuration dashboard with a locally stored profile label and shortcuts to Android app-permission, VPN and location settings.
- A read-only identity report showing the device name, manufacturer, model, Android release/build, and explicit non-access status for IMEI/MEID and SIM identifiers.
- A minimal dark interface with a deep-red accent.
- No analytics, no remote service, and no `INTERNET` permission.
- GitHub Actions workflow to build and publish a debug APK artifact.

## Important Android limitations

PrivacyClone does not create a managed profile silently. It launches Android's own managed-profile provisioning flow and the user must approve the system screens. Provisioning may be unavailable if the device already has a managed profile, has management restrictions, or the manufacturer has disabled the relevant flow.

Android 8.x supports a limited managed-profile configuration; do not expect unlimited parallel clones. A work profile is an Android user/profile boundary, not a virtual Android emulator. The same physical phone, hardware, operating-system build and network connection remain observable to apps where Android exposes them. Apps may also be able to recognize that they are running in a managed/work profile; this project does not conceal or falsify that state.

PrivacyClone does not spoof identifiers, alter device properties, hook other apps, bypass app security checks, intercept arbitrary sensor calls, inspect encrypted network traffic, or promise clone undetectability. The profile label is a local label inside PrivacyClone only; it is not the Android device name. The settings shortcuts open Android system screens and do not silently change permissions or network settings.

### Phone identifier permission control

On Android 8.1, ordinary apps generally need the runtime `READ_PHONE_STATE` permission to access telephony identifiers such as IMEI/MEID, IMSI and SIM serial through the standard Android APIs. When PrivacyClone is installed as the profile owner inside the managed profile, it can use `DevicePolicyManager.setPermissionGrantState()` to deny an app's requested Phone-group runtime permissions. The app list exposes this control only for apps that request `READ_PHONE_STATE`. Denying the group can break calling, SIM checks or other app features. The user can return the permission state to Android's default/user-managed state from PrivacyClone; this does not automatically grant the permission again.

This is permission-based access restriction, not spoofing. It cannot hide device model, manufacturer, Android release/build, hardware characteristics, IP/network metadata, or identifiers exposed through other channels. It cannot guarantee that an app has no way to fingerprint the device. The control is available only when PrivacyClone is the active profile owner; it does not manage apps in the personal profile.

### Installing an app into the managed profile

The prototype provides a package-name field. On Android 9+ it can call `DevicePolicyManager.installExistingPackage()` from the profile-owner context. That API was added in API 28 and is not available on Android 8.1 (API 27). On Android 8.x, the prototype opens the managed-profile Play Store so the user can install the app into that profile. Some apps may refuse to run in a managed profile. App-specific behavior needs testing.

## Build an APK on GitHub (no Android Studio required)

1. Create a new GitHub repository named **PrivacyClone** under your account.
2. Upload the contents of this project folder to the repository root (not the enclosing folder itself).
3. Commit the files to the `main` branch.
4. Open **Actions** and select **Build PrivacyClone APK**. A push to `main` also starts a build.
5. Open the completed workflow run and download the `PrivacyClone-debug-apk` artifact.
6. Extract `app-debug.apk` and install it on your Android phone. Allow installation from your browser or file manager when Android asks.

The workflow uses JDK 17, Gradle 8.7, Android Gradle Plugin 8.6.1, compile SDK 35 and Android build tools 35.0.0. The Android Gradle Plugin 8.6 line requires Gradle 8.7 and JDK 17.

## Test plan for the first device

1. Install and launch PrivacyClone on the target phone.
2. Check whether the Android managed-profile provisioning screen opens.
3. Complete system provisioning only if Android offers it and the user agrees.
4. Reopen PrivacyClone inside the managed profile and verify that it reports profile-owner status.
5. Try installing an existing, non-critical test app by its package name.
6. Confirm that its profile data and login session are separate from the personal profile.
7. Reboot the phone and check the profile, app launch and notifications.
8. Review the permission audit against Android Settings; this initial audit is informational and not a replacement for system permission controls.

## Technical details

- Language: Java
- Minimum SDK: 26 (Android 8.0)
- Target SDK: 27 (initial device-compatibility prototype; raise and re-test before Play Store distribution)
- Compile SDK: 35
- Package: `com.privacyclone.app`
- No third-party runtime libraries

## Privacy model

All scans are performed on-device. The app has `ACCESS_NETWORK_STATE` only to detect whether the active network has VPN transport. It intentionally has no Internet permission and does not upload scan results. Android ID is read only to check whether a value is available; its actual value is never displayed, persisted or sent anywhere.
