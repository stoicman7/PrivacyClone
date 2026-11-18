# Virtual container migration — experimental branch

This branch evaluates an in-app virtual Android container for PrivacyClone. It does not use Android Work Profile or DevicePolicyManager as the clone engine.

## Selected prototype engine

- Upstream: https://github.com/android-hacker/VirtualXposed
- Pinned commit: `122beb371519cb2d221ce06756361aaa30e2674f` (VirtualXposed 0.22.0)
- Engine module: `VirtualApp/lib`
- Documented host range: Android 5.0–10.0, which includes Android 8.1 / API 27.
- Native ABIs in the upstream library build: `arm64-v8a` and `x86_64`.
- Import paths in the upstream project: clone an installed app or install from an APK.
- Multi-instance model: VirtualApp virtual users. Install the same package for different virtual user IDs; each user receives a separate virtual app-data directory.

## Device constraints

The upstream native build does not declare `armeabi-v7a`. This prototype therefore targets 64-bit ARM devices (including ARM64 Huawei models), not 32-bit-only devices. The actual phone ABI must be confirmed before treating an APK as device-ready.

The engine is old (0.22.0, 2022) and its Android 8–10 compatibility claim is not a guarantee that every current app will run. Apps that depend on newer Android APIs, Play Integrity, DRM, special native code, or vendor-specific services may fail.

## Privacy and isolation model

Virtual user IDs isolate the container's package data and accounts as implemented by the engine. They are not Android OS users and do not create separate kernel UIDs or SELinux domains. Treat this as an app-compatibility / multi-account container, not a security boundary for hostile apps.

The engine exposes a `PhoneInfoDelegate` with a virtual user ID. PrivacyClone can use it to return stable, privacy-safe per-instance values for the identifiers that the engine intercepts. It cannot rewrite the phone's real IMEI, SIM identity, radio firmware, or every hardware / network signal. Privacy settings must state exactly which hooks are active.

## Licensing note

VirtualXposed's repository is GPL-3.0 and its README says commercial use is not allowed. The underlying VirtualApp project has also published a separate authorization notice. This branch is an experimental, personal-use evaluation only. Do not distribute a modified APK or publish a derivative release until the applicable upstream permissions and GPL source obligations are confirmed. Keep upstream notices and source available.

## Migration status

- [x] Create isolated `virtual-container-migration` branch.
- [x] Pin the VirtualXposed source revision as a Git submodule.
- [ ] Integrate the VirtualApp library module and its native build.
- [ ] Initialize the engine from PrivacyClone's Application class.
- [ ] Replace Work Profile provisioning with virtual-user and package-import flows.
- [ ] Store clone metadata and per-instance privacy settings locally.
- [ ] Build in GitHub Actions and test on the target Huawei Android 8.1 phone.
