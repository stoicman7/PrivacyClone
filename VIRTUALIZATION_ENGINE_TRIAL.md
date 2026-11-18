# Virtualization-engine integration trial

This branch is an isolated integration trial. The existing `main` branch is left unchanged.

## Why this branch exists

The previous PrivacyClone source is a managed-profile controller. Its app launcher delegates to Android's normal package manager and does not host guest apps inside a virtual Android process. It therefore cannot provide the requested same-user, no-work-profile cloning architecture.

This branch adds the Apache-2.0-licensed ISEKHON/VirtualApp project as the `engine` Git submodule. That upstream project contains an actual in-process virtualization engine, virtual process slots, framework service hooks, and native file-path redirection. It declares Android 5.0–14 support, but its documented test device is much newer than the target Android 8.1 Huawei device. Compatibility with this phone and with TikTok/HelloTalk is not yet verified.

## Build

The trial workflow uses the current Android command-line-tools setup action and installs the required SDK/NDK packages explicitly.

Open GitHub Actions and run **Build experimental PrivacyClone virtual engine**, or push another commit to this branch. The workflow checks out the pinned submodule revision, changes the host application ID to `com.privacyclone.probe`, changes the displayed app name, and builds APKs for the supported ABIs.

## Important test status

A successful CI build proves only that the APK compiles. It does not prove that guest apps launch correctly. Test the APK on the actual Android 8.1 device with a simple app first, then ToBe, HelloTalk, and TikTok. Capture the virtual engine and AndroidRuntime logs for any black screen.

## Upstream and license

- Upstream: https://github.com/ISEKHON/VirtualApp
- License: Apache License 2.0
- Submodule commit: b3c634ad7941765df3da84a207aca94b7861afae

The engine is an experimental starting point, not a claim that all apps or anti-tamper systems are supported. No stealth or undetectability guarantee is made.
