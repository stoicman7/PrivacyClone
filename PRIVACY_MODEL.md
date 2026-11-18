# Privacy model and claims

## What this prototype can verify

- Whether this app is operating as the profile owner.
- Whether the current Android user is a managed profile.
- Whether Android returns a profile-scoped `ANDROID_ID` value to this app (the value itself is never shown or saved).
- Which selected sensitive runtime permissions are reported granted for packages visible in this profile.
- Whether the active network advertises VPN transport.
- Which sensor types Android reports on this device.
- The Android system device name when exposed through `Settings.Global.DEVICE_NAME`.
- The locally configured PrivacyClone profile label, stored in app-private preferences for the current Android profile.
- Which sensitive permissions in the audit list are reported as granted for apps visible in the current profile.
- Whether an installed app requests `READ_PHONE_STATE` and whether PrivacyClone has applied a profile-owner denial to that permission group.
- Whether the profile owner can apply or restore Android runtime-permission policy for a selected app in the managed profile.

## What it cannot prove

- That an app cannot fingerprint the physical phone.
- That the public IP, DNS path, or network metadata is hidden.
- That all app-to-app tracking is blocked.
- That sensor output is intercepted or anonymized.
- That every package on the phone is visible from this profile.
- That an app will accept a work-profile installation.
- That a clone is undetectable.
- That system settings shortcuts apply only to the managed profile; behavior depends on Android and the device manufacturer.
- That apps cannot read hardware, OS-build, SIM, network or other signals that Android exposes to them.
- That denying `READ_PHONE_STATE` prevents every possible fingerprinting technique or every vendor-specific access path.

## Design rules

1. Prefer Android's real per-user isolation over APK patching or runtime virtualization.
2. Request the smallest possible set of permissions.
3. Never collect or transmit device identifiers.
4. Do not falsify hardware, OS, or security properties.
5. Explain uncertainty and distinguish observed facts from estimates.
6. Require user confirmation for profile provisioning and app installation.
7. Keep the configurable profile label local to PrivacyClone; never describe it as a system device-name or identifier change.
8. Do not request phone-state or SIM permissions merely to display IMEI, MEID, IMSI or phone-number data.
9. Prefer Android-managed permission denial over fabricated identifiers. Explain that permission denial is not spoofing and can affect app functionality.
