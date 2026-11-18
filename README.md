# PrivacyClone

PrivacyClone is a local-first Android app-cloning prototype. It runs cloned apps inside an embedded virtual Android container instead of creating an Android Work Profile.

## What this branch implements

- Embedded VirtualApp engine from the VirtualXposed source tree, pinned as a Git submodule.
- Separate virtual user for each clone, so each clone gets its own virtual app-data directory.
- Clone an already-installed app by package name.
- Import a single-file APK through Android's document picker.
- Open a clone from PrivacyClone.
- Remove a clone and its virtual-user data without uninstalling the original app from Android.
- Local privacy diagnostics and settings dashboard.
- No analytics or remote telemetry code added by PrivacyClone.

This is an experimental migration branch. It is not yet a verified release. The GitHub Actions build and real-device tests are still required.

## Important compatibility limits

- The pinned VirtualApp/VirtualXposed source is version 0.22.0 from 2022. Its upstream README describes support through Android 10; Android 8.1 is within that stated range, but this does not guarantee compatibility with every app or Huawei firmware.
- The native library in this source build declares arm64-v8a and x86_64. A 32-bit-only ARM phone is not supported by this build. Confirm that the target phone is ARM64.
- This first version accepts single-file APKs. Apps installed as multiple split APK files are rejected by the installed-app import path.
- Apps that depend on Google Play services, Play Integrity, DRM, vendor frameworks, special native code, or newer Android APIs may fail to install or launch.
- A virtual user is not an Android OS user. It does not create a separate kernel UID, SELinux domain, hardware device, or network connection. Treat this as an app-data container, not a security boundary against a hostile app.
- The current prototype does not promise per-clone runtime-permission isolation, device-ID spoofing, IP separation, or undetectability. Cloned apps can still identify the physical device and may detect virtualization.

## Permission and network disclosure

The embedded VirtualApp library manifest declares a broad permission set because it is designed to host many kinds of Android apps. This includes Internet, network state, location, contacts, camera, microphone, phone, SMS, storage, and other permissions.

Those declarations become part of the merged PrivacyClone APK. They are required for compatibility with a broad range of cloned apps, but they mean the package is **not** a no-Internet-permission app. Android runtime-protected capabilities still depend on Android permission grants and the engine's compatibility behavior.

PrivacyClone adds no analytics or telemetry service and does not upload its own diagnostics. However, cloned apps may use the network as they normally would. Review the merged manifest and test permissions on the phone before trusting this build with sensitive accounts or data.

## Build on GitHub

The project is built in GitHub Actions; Android Studio is not required on your computer.

1. Open the virtual-container-migration branch.
2. Open **Actions** and choose **Android CI**.
3. Open the run triggered by the branch push.
4. Download the **PrivacyClone-debug-apk** artifact if the build succeeds.

The workflow checks out the VirtualXposed submodule recursively and installs Android SDK 28/35 plus NDK 21.4.7075529. It adds the missing Android Gradle Plugin namespace to the legacy library checkout during the CI job; this patch is not written back into the upstream submodule.

## Device test plan

Use a spare test app/account first. Do not start with a banking, work, or primary messaging account.

1. Install the APK on the ARM64 Android 8.1 phone.
2. Open PrivacyClone and check that the virtual container reports initialized.
3. Enter the package name of a small, non-sensitive app already installed on the phone.
4. Tap **CLONE INSTALLED APP**.
5. Confirm that the clone appears in the list and opens.
6. Sign into a test account and confirm its app data is separate from the original installation.
7. Close and reopen PrivacyClone; confirm the clone remains listed and still launches.
8. Create a second clone of the same app and confirm that its data/session is independent of the first clone.
9. Remove one clone and verify that the original Android installation and the other clone remain.

Also test APK import, notifications, background operation, reboot persistence, camera/microphone/location needs, and apps that use Google services. Record failures instead of assuming broad compatibility.

## Engine and licensing

- Upstream source: https://github.com/android-hacker/VirtualXposed
- Pinned upstream revision: 122beb371519cb2d221ce06756361aaa30e2674f
- Upstream README: https://github.com/android-hacker/VirtualXposed/blob/vxp/README.md
- VirtualApp licensing declaration: https://github.com/android-hacker/VirtualApp

The upstream VirtualXposed README identifies GPL-3.0 and explicitly says commercial use is not allowed; it also points to a separate VirtualApp declaration. This branch is for personal evaluation only. Do not distribute a modified APK or use this in a commercial product until the applicable permissions and source-distribution obligations have been checked. Retain all upstream notices and source.

## Main branch

The main branch has not been changed by this migration work. All changes are on virtual-container-migration.