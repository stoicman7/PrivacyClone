# Device test checklist

## Baseline
- [ ] Record exact phone model, Android version, EMUI version and available storage.
- [ ] Confirm there is no existing managed/work profile that must be retained.
- [ ] Install PrivacyClone and launch it.

## Provisioning
- [ ] Tap Set up Work Profile.
- [ ] Confirm Android's system provisioning UI opens.
- [ ] If it opens, read prompts and approve only if expected.
- [ ] If provisioning is unavailable, record the exact system error and stop; do not use ADB or root workarounds on a daily-use device.
- [ ] After successful provisioning, launch PrivacyClone in the work profile.
- [ ] Confirm the app reports profile-owner status.

## Isolation
- [ ] Choose a non-critical test app already installed on the personal side.
- [ ] Try installing it using its package name.
- [ ] Confirm the app appears in the work-profile launcher.
- [ ] Sign into a test account and verify the personal-profile session is unchanged.
- [ ] Create test data in each profile and verify it is not shared.

## Privacy diagnostics
- [ ] Compare the sensitive-permission report with Android Settings.
- [ ] Verify that VPN status changes when a VPN is connected/disconnected.
- [ ] Confirm the report does not display the Android ID value.
- [ ] Confirm the app has no Internet permission in Android App Info.

## Reliability
- [ ] Reboot and check that the profile remains available.
- [ ] Check app launching and notifications in both profiles.
- [ ] Test with the phone's battery optimization settings left at their normal values.

## Privacy configuration dashboard checks

1. Set a local profile label and save it; force-stop and reopen PrivacyClone to verify that it persists.
2. If the app is installed in both personal and managed profiles, set different labels and verify they remain separate.
3. Open the app-permissions, VPN and location shortcuts; confirm Android opens its own settings UI and that PrivacyClone does not silently change a setting.
4. Verify the identity report shows the actual manufacturer/model/Android build and does not display an IMEI, MEID, SIM identifier, phone number or raw Android ID.
5. Check the manifest to confirm that `READ_PHONE_STATE`, `READ_PHONE_NUMBERS`, `READ_PRIVILEGED_PHONE_STATE` and `INTERNET` are not requested.
6. On Android 8.1, confirm all dashboard controls remain usable and gracefully report unavailable settings screens if the manufacturer removed an activity.

## Phone-identifier permission controls (Android 8.1)

1. Complete managed-profile provisioning and launch PrivacyClone inside the managed profile.
2. Install a non-critical test app that requests `READ_PHONE_STATE` inside the managed profile.
3. Confirm the app row shows `READ_PHONE_STATE` status and the phone-identifier control.
4. Tap `BLOCK PHONE IDENTIFIER ACCESS` and review the impact warning before confirming.
5. Verify Android reports `READ_PHONE_STATE` denied for that app in the managed profile.
6. Test only non-critical app functions; confirm any feature requiring telephony permission fails gracefully.
7. Tap `RESTORE USER CONTROL`, then verify Android settings lets the user manage the permission again; it must not silently re-grant it.
8. Confirm apps not requesting `READ_PHONE_STATE` do not show the control.
9. Confirm PrivacyClone itself still does not request `READ_PHONE_STATE`, `READ_PHONE_NUMBERS` or privileged telephony permissions.
10. Repeat from the personal profile and confirm PrivacyClone cannot manage personal-profile apps.
