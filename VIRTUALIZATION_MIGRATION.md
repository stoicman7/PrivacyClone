# Virtual container migration

This branch replaces Android Work Profile / DevicePolicyManager cloning with an embedded virtual Android container. It does not create or require a managed profile.

## Selected engine

- Upstream: https://github.com/android-hacker/VirtualXposed
- Pinned source revision: 122beb371519cb2d221ce06756361aaa30e2674f (VirtualXposed 0.22.0)
- Gradle module: vendor/VirtualXposed/VirtualApp/lib
- Native ABIs declared by the upstream library: arm64-v8a and x86_64
- The upstream README describes Android 5.0–10.0 support. Android 8.1 falls within that published range, but individual apps and vendor firmware still need real-device testing.

## Implemented in this branch

- VirtualCore startup from PrivacyCloneApplication.
- VirtualApp library linked as a Gradle subproject.
- Import an already-installed single-file app by package name.
- Import a single-file APK through Android's document picker.
- Create a separate VirtualApp user for each clone.
- Install the imported package into that virtual user.
- List, launch, and remove clone instances.
- Persist local clone metadata in PrivacyClone's private SharedPreferences.
- Request missing dangerous runtime permissions before launching a clone. The grants belong to PrivacyClone's host package and are not per-clone isolated in this prototype.
- Removed Work Profile provisioning and the device-admin receiver from the app manifest.
- GitHub Actions checks out the engine submodule, installs the NDK, enables AIDL source generation, and supplies Android 8.1 hidden framework API signatures through a compile-only Robolectric framework JAR.

## Important limitations

- The upstream code is old (2022) and is not guaranteed to support every Android app, Huawei firmware build, Google Play service, Play Integrity check, DRM path, or modern API.
- The first import flow supports single-file APKs. Split APK applications are rejected by the installed-app import path.
- VirtualApp's manifest declares a large permission superset, including Internet, location, contacts, camera, microphone, phone, SMS, and storage permissions. These declarations are merged into the host APK to support hosted apps.
- Runtime permissions requested by a clone are requested for PrivacyClone itself. They may be shared across all clones and are not individually isolated.
- Some special permissions and app-specific background services are not covered by the current permission bridge.
- Virtual users separate virtual app data, but are not Android OS users. They do not create separate kernel UIDs, SELinux domains, hardware devices, or network connections.
- The current code does not spoof IMEI, SIM identifiers, Android build properties, IP addresses, or other hardware/network fingerprints. Apps may detect virtualization.
- This is an app compatibility/container feature, not a security boundary for untrusted apps.

## Privacy model

PrivacyClone adds no analytics, crash upload service, or telemetry. However, because the engine contributes Internet permission, cloned apps can use the network normally. Review permissions and test with non-sensitive accounts before relying on the prototype.

Do not use the initial prototype with banking, corporate, or primary messaging accounts. Test on a spare device or test account first.

## Build notes

The engine's original Android Gradle Plugin-era library build does not declare a modern namespace or NDK version and assumes AIDL generation. The CI workflow patches the checked-out submodule at build time to add the namespace, pin the NDK, and enable AIDL. It also places Robolectric's Android 8.1 framework JAR on the library compile classpath only, because the engine imports hidden Android framework interfaces omitted from the public SDK stubs. That JAR is not packaged in the APK. The pinned upstream submodule itself remains unchanged.

## Licensing

VirtualXposed's upstream repository identifies GPL-3.0 and its README explicitly disallows commercial use, referring to a separate VirtualApp declaration. This branch is for personal evaluation only. Do not distribute a modified APK or use it commercially until all upstream permissions, license conditions, and source-distribution obligations are confirmed. Keep all notices and corresponding source.

## Validation status

- [x] Work Profile implementation removed from the launcher flow and app manifest.
- [x] VirtualApp Gradle module linked and initialized.
- [x] Clone/import/list/launch/remove flows implemented.
- [x] CI updated to fetch the submodule and native NDK.
- [ ] GitHub Actions build verified successfully.
- [ ] Install and test on the actual ARM64 Android 8.1 Huawei phone.
- [ ] Verify separate data and login sessions across two clones of the same package.
- [ ] Verify runtime permission behavior, notifications, reboot persistence, and background launch.
