package com.privacyclone.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Privacy-first Android 8.x prototype.
 * All inspection is performed locally. No INTERNET permission is declared.
 */
public final class PrivacyCloneActivity extends Activity {
    private static final int REQUEST_PROVISION_PROFILE = 4102;
    private static final int BG = Color.rgb(11, 13, 16);
    private static final int PANEL = Color.rgb(22, 25, 30);
    private static final int PANEL_ALT = Color.rgb(29, 33, 39);
    private static final int TEXT = Color.rgb(242, 244, 247);
    private static final int MUTED = Color.rgb(157, 166, 178);
    private static final int RED = Color.rgb(149, 6, 6);
    private static final int GREEN = Color.rgb(91, 176, 126);
    private static final int AMBER = Color.rgb(218, 164, 77);

    private DevicePolicyManager devicePolicyManager;
    private UserManager userManager;
    private LinearLayout root;
    private LinearLayout content;
    private TextView profileStatus;
    private TextView profileDetails;
    private Button provisionButton;
    private EditText packageInput;
    private EditText profileLabelInput;
    private LinearLayout installedAppsContainer;
    private LinearLayout permissionFindingsContainer;
    private TextView appCountValue;
    private TextView permissionCountValue;
    private TextView vpnValue;
    private TextView sensorValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        userManager = (UserManager) getSystemService(Context.USER_SERVICE);
        buildInterface();
        refreshAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (content != null) refreshAll();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PROVISION_PROFILE) {
            refreshAll();
            if (resultCode != RESULT_OK) {
                toast("Profile setup was cancelled or Android could not complete it.");
            }
        }
    }

    private void buildInterface() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(28));
        root.setBackgroundColor(BG);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView brand = text("PRIVACYCLONE", 23, TEXT, true);
        brand.setLetterSpacing(0.09f);
        root.addView(brand);
        TextView subtitle = text("Private app profiles. Clear privacy diagnostics.", 13, MUTED, false);
        LinearLayout.LayoutParams subtitleParams = params(-1, -2);
        subtitleParams.topMargin = dp(5);
        root.addView(subtitle, subtitleParams);

        LinearLayout topLine = new LinearLayout(this);
        topLine.setBackgroundColor(RED);
        LinearLayout.LayoutParams lineParams = params(dp(42), dp(3));
        lineParams.topMargin = dp(18);
        lineParams.bottomMargin = dp(20);
        root.addView(topLine, lineParams);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, params(-1, -2));
        setContentView(scroll);

        addProfileCard();
        addCloneCard();
        addPrivacyCard();
        addConfigurationCard();
        addPermissionCard();
        addDiagnosticsCard();

        TextView footer = text("LOCAL-FIRST DESIGN  •  NO ANALYTICS  •  NO INTERNET ACCESS", 10, MUTED, true);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = params(-1, -2);
        footerParams.topMargin = dp(24);
        content.addView(footer, footerParams);
    }

    private void addProfileCard() {
        LinearLayout card = card();
        sectionTitle(card, "PROFILE STATUS", "Android-managed isolation");
        profileStatus = text("Checking profile…", 19, TEXT, true);
        LinearLayout.LayoutParams statusParams = params(-1, -2);
        statusParams.topMargin = dp(13);
        card.addView(profileStatus, statusParams);
        profileDetails = text("Checking Android profile capabilities.", 12, MUTED, false);
        LinearLayout.LayoutParams detailsParams = params(-1, -2);
        detailsParams.topMargin = dp(6);
        card.addView(profileDetails, detailsParams);

        provisionButton = button("SET UP WORK PROFILE");
        LinearLayout.LayoutParams buttonParams = params(-1, dp(48));
        buttonParams.topMargin = dp(16);
        card.addView(provisionButton, buttonParams);
        provisionButton.setOnClickListener(v -> showProvisioningExplanation());
        addCard(card);
    }

    private void addCloneCard() {
        LinearLayout card = card();
        sectionTitle(card, "CLONE MANAGER", "Install another per-profile instance of an app");

        TextView note = text("Android keeps app data separate between personal and managed profiles. It uses the work-profile Play Store on Android 8.x and the profile-owner install API on Android 9+; it does not modify or patch APKs. For apps requesting READ_PHONE_STATE, use the per-app control below to deny access to Android's Phone permission group.", 12, MUTED, false);
        LinearLayout.LayoutParams noteParams = params(-1, -2);
        noteParams.topMargin = dp(12);
        card.addView(note, noteParams);

        packageInput = new EditText(this);
        packageInput.setSingleLine(true);
        packageInput.setHint("Package name, e.g. com.whatsapp");
        packageInput.setTextColor(TEXT);
        packageInput.setHintTextColor(MUTED);
        packageInput.setTextSize(14);
        packageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        packageInput.setPadding(dp(13), 0, dp(13), 0);
        packageInput.setBackground(round(PANEL_ALT, 10, Color.rgb(54, 60, 69)));
        LinearLayout.LayoutParams inputParams = params(-1, dp(48));
        inputParams.topMargin = dp(15);
        card.addView(packageInput, inputParams);

        Button install = button("INSTALL IN WORK PROFILE");
        LinearLayout.LayoutParams installParams = params(-1, dp(46));
        installParams.topMargin = dp(10);
        card.addView(install, installParams);
        install.setOnClickListener(v -> installIntoManagedProfile());

        appCountValue = text("—", 13, TEXT, true);
        LinearLayout.LayoutParams countParams = params(-1, -2);
        countParams.topMargin = dp(16);
        card.addView(appCountValue, countParams);
        installedAppsContainer = new LinearLayout(this);
        installedAppsContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = params(-1, -2);
        listParams.topMargin = dp(8);
        card.addView(installedAppsContainer, listParams);
        addCard(card);
    }

    private void addPrivacyCard() {
        LinearLayout card = card();
        sectionTitle(card, "PRIVACY OVERVIEW", "Signals visible from this Android profile");
        addMetric(card, "Profile-scoped Android ID", "Checking…");
        addMetric(card, "VPN transport", "Checking…");
        addMetric(card, "Available device sensors", "Checking…");
        TextView disclaimer = text("A work profile can separate app data and some profile-scoped identifiers. It cannot hide the phone's real model, OS build, hardware characteristics or public network address from every app.", 11, MUTED, false);
        LinearLayout.LayoutParams p = params(-1, -2);
        p.topMargin = dp(13);
        card.addView(disclaimer, p);
        addCard(card);
    }

    private void addConfigurationCard() {
        LinearLayout card = card();
        sectionTitle(card, "PRIVACY CONFIGURATION", "Profile controls that Android actually supports");

        TextView labelHelp = text(
                "Choose a local label to distinguish this PrivacyClone workspace. It changes only the label shown here; it does not rename the phone or alter identifiers seen by other apps.",
                11, MUTED, false);
        LinearLayout.LayoutParams helpParams = params(-1, -2);
        helpParams.topMargin = dp(12);
        card.addView(labelHelp, helpParams);

        profileLabelInput = new EditText(this);
        profileLabelInput.setSingleLine(true);
        profileLabelInput.setHint("For example: Work profile");
        profileLabelInput.setTextColor(TEXT);
        profileLabelInput.setHintTextColor(MUTED);
        profileLabelInput.setTextSize(14);
        profileLabelInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        profileLabelInput.setPadding(dp(13), 0, dp(13), 0);
        profileLabelInput.setBackground(round(PANEL_ALT, 10, Color.rgb(54, 60, 69)));
        SharedPreferences preferences = getSharedPreferences("privacyclone_settings", MODE_PRIVATE);
        profileLabelInput.setText(preferences.getString("profile_label", ""));
        LinearLayout.LayoutParams inputParams = params(-1, dp(48));
        inputParams.topMargin = dp(12);
        card.addView(profileLabelInput, inputParams);

        Button saveLabel = button("SAVE PROFILE LABEL");
        LinearLayout.LayoutParams saveParams = params(-1, dp(44));
        saveParams.topMargin = dp(9);
        card.addView(saveLabel, saveParams);
        saveLabel.setOnClickListener(v -> {
            String label = profileLabelInput.getText().toString().trim();
            if (label.length() > 40) {
                profileLabelInput.setError("Use 40 characters or fewer");
                return;
            }
            preferences.edit().putString("profile_label", label).apply();
            hideProfileKeyboard();
            toast(label.isEmpty() ? "Profile label cleared." : "Profile label saved on this profile.");
        });

        TextView settingsNote = text(
                "The following shortcuts open Android's own settings. Permission and location changes are enforced by Android, not by a hidden background service.",
                11, MUTED, false);
        LinearLayout.LayoutParams noteParams = params(-1, -2);
        noteParams.topMargin = dp(15);
        card.addView(settingsNote, noteParams);

        Button appSettings = button("MANAGE APP PERMISSIONS");
        LinearLayout.LayoutParams appParams = params(-1, dp(44));
        appParams.topMargin = dp(10);
        card.addView(appSettings, appParams);
        appSettings.setOnClickListener(v -> openSystemSettings(
                Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS,
                "Android app settings are not available on this build."));

        Button vpnSettings = button("OPEN VPN SETTINGS");
        LinearLayout.LayoutParams vpnParams = params(-1, dp(44));
        vpnParams.topMargin = dp(8);
        card.addView(vpnSettings, vpnParams);
        vpnSettings.setOnClickListener(v -> openSystemSettings(
                Settings.ACTION_VPN_SETTINGS,
                "VPN settings are not available on this build."));

        Button locationSettings = button("OPEN LOCATION SETTINGS");
        LinearLayout.LayoutParams locationParams = params(-1, dp(44));
        locationParams.topMargin = dp(8);
        card.addView(locationSettings, locationParams);
        locationSettings.setOnClickListener(v -> openSystemSettings(
                Settings.ACTION_LOCATION_SOURCE_SETTINGS,
                "Location settings are not available on this build."));

        addCard(card);
    }

    private void addPermissionCard() {
        LinearLayout card = card();
        sectionTitle(card, "PERMISSION AUDIT", "Sensitive permissions granted in this profile");
        permissionCountValue = text("Scanning…", 13, TEXT, true);
        LinearLayout.LayoutParams countParams = params(-1, -2);
        countParams.topMargin = dp(13);
        card.addView(permissionCountValue, countParams);
        permissionFindingsContainer = new LinearLayout(this);
        permissionFindingsContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = params(-1, -2);
        listParams.topMargin = dp(7);
        card.addView(permissionFindingsContainer, listParams);
        Button settings = button("OPEN ANDROID APP SETTINGS");
        LinearLayout.LayoutParams settingsParams = params(-1, dp(44));
        settingsParams.topMargin = dp(13);
        card.addView(settings, settingsParams);
        settings.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
            } catch (Exception e) {
                toast("Android settings could not be opened.");
            }
        });
        addCard(card);
    }

    private void addDiagnosticsCard() {
        LinearLayout card = card();
        sectionTitle(card, "DEVICE IDENTITY REPORT", "Read-only facts and access boundaries");
        addKeyValue(card, "Device name", getSystemDeviceName());
        addKeyValue(card, "Manufacturer", Build.MANUFACTURER);
        addKeyValue(card, "Model", Build.MODEL);
        addKeyValue(card, "Android version", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        addKeyValue(card, "Build", Build.DISPLAY);
        addKeyValue(card, "IMEI / MEID", "Not accessed by PrivacyClone");
        addKeyValue(card, "SIM identifiers / number", "Not accessed by PrivacyClone");
        addKeyValue(card, "Android ID", "Availability only; value is not collected");
        addKeyValue(card, "App permissions", "No phone-state or contacts permission requested");
        TextView warning = text(
                "This report does not change what other apps can see. Device model, Android build, hardware traits and SIM/network information may still be available to apps under Android's rules. The IMEI and SIM values are deliberately not read, stored or displayed.",
                11, MUTED, false);
        LinearLayout.LayoutParams p = params(-1, -2);
        p.topMargin = dp(13);
        card.addView(warning, p);
        addCard(card);
    }

    private String getSystemDeviceName() {
        try {
            String name = Settings.Global.getString(getContentResolver(), Settings.Global.DEVICE_NAME);
            if (name != null && name.trim().length() > 0) return name.trim();
        } catch (Exception ignored) { }
        return "Not exposed by Android settings";
    }

    private void refreshAll() {
        refreshProfileStatus();
        refreshInstalledApps();
        refreshIdentifierStatus();
        refreshNetworkStatus();
        refreshSensorStatus();
        refreshPermissionAudit();
    }

    private void refreshProfileStatus() {
        boolean isOwner = devicePolicyManager != null && devicePolicyManager.isProfileOwnerApp(getPackageName());
        boolean isManaged = userManager != null && userManager.isManagedProfile();
        if (provisionButton != null) {
            provisionButton.setEnabled(!isOwner);
            provisionButton.setText(isOwner ? "WORK PROFILE ACTIVE" : "SET UP WORK PROFILE");
            provisionButton.setAlpha(isOwner ? 0.55f : 1.0f);
        }
        if (isOwner && isManaged) {
            profileStatus.setText("Work profile is active");
            profileStatus.setTextColor(GREEN);
            profileDetails.setText("PrivacyClone is the profile owner in this managed profile. App data and profile-scoped settings are managed separately from your personal profile.");
        } else if (isOwner) {
            profileStatus.setText("Profile owner detected");
            profileStatus.setTextColor(GREEN);
            profileDetails.setText("Android granted profile-owner status. The current user context is not reported as a managed profile; inspect device settings before continuing.");
        } else {
            profileStatus.setText("Work profile not configured");
            profileStatus.setTextColor(AMBER);
            profileDetails.setText("Set-up is user-confirmed and depends on the phone's Android provisioning support. An existing work profile may prevent creating another one.");
        }
    }

    private void showProvisioningExplanation() {
        new AlertDialog.Builder(this)
                .setTitle("Set up a managed profile")
                .setMessage("Android will open its own profile-provisioning screen. Read each system prompt carefully. This requires your confirmation and may be unavailable if the device already has a managed profile, is restricted by the manufacturer, or has device-management policies. PrivacyClone cannot create a profile silently.")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("CONTINUE", (dialog, which) -> requestManagedProfile())
                .show();
    }

    private void requestManagedProfile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            toast("Managed profiles require Android 5.0 or newer.");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    && devicePolicyManager != null
                    && !devicePolicyManager.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)) {
                toast("Android currently does not allow a new managed profile on this device.");
                return;
            }
            Intent intent = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
            ComponentName admin = new ComponentName(this, PrivacyDeviceAdminReceiver.class);
            intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, admin);
            startActivityForResult(intent, REQUEST_PROVISION_PROFILE);
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Provisioning unavailable")
                    .setMessage("This Android build did not provide a usable managed-profile provisioning activity. No profile was created. You can still use the local diagnostics in PrivacyClone.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void installIntoManagedProfile() {
        if (devicePolicyManager == null || !devicePolicyManager.isProfileOwnerApp(getPackageName())) {
            toast("Set up the managed profile first.");
            return;
        }
        String packageName = packageInput.getText().toString().trim();
        if (!packageName.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")) {
            packageInput.setError("Enter a valid Android package name");
            return;
        }
        hideKeyboard();
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                boolean installed = devicePolicyManager.installExistingPackage(
                        new ComponentName(this, PrivacyDeviceAdminReceiver.class), packageName);
                if (installed) {
                    toast("Install request completed. Refreshing app list.");
                    refreshInstalledApps();
                } else {
                    toast("Package is not available for installation on this device.");
                }
            } catch (SecurityException e) {
                toast("Android denied this install request.");
            } catch (Exception e) {
                toast("Could not install this package in the managed profile.");
            }
        } else {
            // DevicePolicyManager.installExistingPackage() was added in API 28.
            // On Android 8.x the supported user-driven route is the Play Store
            // installed inside the managed profile.
            try {
                Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
                market.setPackage("com.android.vending");
                startActivity(market);
            } catch (Exception e) {
                new AlertDialog.Builder(this)
                        .setTitle("Install from the work profile")
                        .setMessage("Android 8.1 does not provide the profile-owner API needed to install an existing personal-profile package directly. Open the Play Store from the work-profile launcher, sign in there if needed, and install the app into the work profile. This keeps the installation and app data under Android's normal profile controls.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        }
    }

    private void refreshInstalledApps() {
        if (installedAppsContainer == null) return;
        installedAppsContainer.removeAllViews();
        try {
            Intent launcher = new Intent(Intent.ACTION_MAIN, null);
            launcher.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> activities = getPackageManager().queryIntentActivities(launcher, 0);
            Set<String> packages = new HashSet<>();
            List<AppEntry> entries = new ArrayList<>();
            for (ResolveInfo info : activities) {
                if (info.activityInfo == null) continue;
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || !packages.add(pkg)) continue;
                String label = String.valueOf(info.loadLabel(getPackageManager()));
                boolean requestsPhoneState = appRequestsPermission(pkg, "android.permission.READ_PHONE_STATE");
                entries.add(new AppEntry(label, pkg, requestsPhoneState));
            }
            Collections.sort(entries, (a, b) -> a.label.toLowerCase(Locale.US).compareTo(b.label.toLowerCase(Locale.US)));
            appCountValue.setText(entries.size() + " launchable apps visible in this profile");
            if (entries.isEmpty()) {
                addMutedLine(installedAppsContainer, "No other launchable apps were found in this profile.");
            }
            int limit = Math.min(entries.size(), 8);
            for (int i = 0; i < limit; i++) {
                AppEntry entry = entries.get(i);
                LinearLayout appRow = new LinearLayout(this);
                appRow.setOrientation(LinearLayout.VERTICAL);
                appRow.setPadding(dp(10), dp(9), dp(10), dp(10));
                appRow.setBackground(round(PANEL_ALT, 8, Color.TRANSPARENT));
                LinearLayout.LayoutParams p = params(-1, -2);
                p.bottomMargin = dp(7);
                installedAppsContainer.addView(appRow, p);

                TextView title = text(entry.label, 13, TEXT, true);
                appRow.addView(title, params(-1, -2));
                TextView packageName = text(entry.packageName, 10, MUTED, false);
                LinearLayout.LayoutParams packageParams = params(-1, -2);
                packageParams.topMargin = dp(3);
                appRow.addView(packageName, packageParams);

                if (entry.requestsPhoneState) {
                    TextView phoneStatus = text(getPhonePermissionStatus(entry.packageName), 11, MUTED, false);
                    LinearLayout.LayoutParams statusParams = params(-1, -2);
                    statusParams.topMargin = dp(8);
                    appRow.addView(phoneStatus, statusParams);

                    boolean policyBlocked = isPhonePermissionPolicyBlocked(entry.packageName);
                    Button phoneControl = button(policyBlocked ? "RESTORE USER CONTROL" : "BLOCK PHONE IDENTIFIER ACCESS");
                    LinearLayout.LayoutParams controlParams = params(-1, dp(40));
                    controlParams.topMargin = dp(7);
                    appRow.addView(phoneControl, controlParams);
                    phoneControl.setEnabled(devicePolicyManager != null
                            && devicePolicyManager.isProfileOwnerApp(getPackageName()));
                    phoneControl.setAlpha(phoneControl.isEnabled() ? 1.0f : 0.45f);
                    phoneControl.setOnClickListener(v -> {
                        if (isPhonePermissionPolicyBlocked(entry.packageName)) {
                            restorePhonePermissionControl(entry.packageName);
                        } else {
                            confirmBlockPhoneIdentifiers(entry);
                        }
                    });
                } else {
                    TextView noPhonePermission = text("Does not request READ_PHONE_STATE", 10, GREEN, false);
                    LinearLayout.LayoutParams statusParams = params(-1, -2);
                    statusParams.topMargin = dp(7);
                    appRow.addView(noPhonePermission, statusParams);
                }

                TextView launch = text("OPEN APP", 10, MUTED, true);
                launch.setGravity(Gravity.RIGHT);
                LinearLayout.LayoutParams launchParams = params(-1, -2);
                launchParams.topMargin = dp(8);
                appRow.addView(launch, launchParams);
                appRow.setOnClickListener(v -> {
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(entry.packageName);
                    if (launchIntent != null) startActivity(launchIntent);
                    else toast("This app has no launchable activity.");
                });
            }
            if (entries.size() > limit) addMutedLine(installedAppsContainer, "+ " + (entries.size() - limit) + " more apps");
        } catch (Exception e) {
            appCountValue.setText("App list unavailable");
            addMutedLine(installedAppsContainer, "Android did not provide the app list for this profile.");
        }
    }


    private boolean appRequestsPermission(String packageName, String permission) {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions == null) return false;
            for (String requested : info.requestedPermissions) {
                if (permission.equals(requested)) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    /**
     * Returns phone-group runtime permissions declared by this app. Denying the whole
     * requested group avoids leaving related phone permissions under a confusing,
     * partially policy-controlled state on Android 8.x.
     */
    private List<String> getRequestedPhonePermissions(String packageName) {
        List<String> permissions = new ArrayList<>();
        try {
            PackageInfo info = getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions == null) return permissions;
            for (String permission : info.requestedPermissions) {
                boolean isPhoneState = "android.permission.READ_PHONE_STATE".equals(permission);
                boolean isPhoneGroup = false;
                try {
                    PermissionInfo permissionInfo = getPackageManager().getPermissionInfo(permission, 0);
                    isPhoneGroup = "android.permission-group.PHONE".equals(permissionInfo.group);
                } catch (PackageManager.NameNotFoundException ignored) { }
                if ((isPhoneState || isPhoneGroup) && !permissions.contains(permission)) {
                    permissions.add(permission);
                }
            }
        } catch (Exception ignored) { }
        return permissions;
    }

    private boolean isPhonePermissionPolicyBlocked(String packageName) {
        if (devicePolicyManager == null || !devicePolicyManager.isProfileOwnerApp(getPackageName())) return false;
        try {
            ComponentName admin = new ComponentName(this, PrivacyDeviceAdminReceiver.class);
            return devicePolicyManager.getPermissionGrantState(
                    admin, packageName, "android.permission.READ_PHONE_STATE")
                    == DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String getPhonePermissionStatus(String packageName) {
        if (isPhonePermissionPolicyBlocked(packageName)) return "Phone permission group: blocked by PrivacyClone";
        int state = getPackageManager().checkPermission("android.permission.READ_PHONE_STATE", packageName);
        return state == PackageManager.PERMISSION_GRANTED
                ? "READ_PHONE_STATE is currently allowed"
                : "READ_PHONE_STATE is not currently granted";
    }

    private void confirmBlockPhoneIdentifiers(AppEntry entry) {
        if (devicePolicyManager == null || !devicePolicyManager.isProfileOwnerApp(getPackageName())) {
            toast("Set up the managed profile first. This control works only inside the profile PrivacyClone manages.");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Block phone and SIM permission?")
                .setMessage("PrivacyClone will deny this app's requested Android Phone-group permissions, including READ_PHONE_STATE. On Android 8.1, this blocks ordinary access to IMEI/MEID, IMSI and SIM serial through APIs protected by that permission. It does not create replacement identifiers or hide the phone model, Android build, IP address or every hardware signal. Calling, SIM checks or other app features may stop working.")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("BLOCK ACCESS", (dialog, which) -> applyPhonePermissionPolicy(entry.packageName, true))
                .show();
    }

    private void restorePhonePermissionControl(String packageName) {
        new AlertDialog.Builder(this)
                .setTitle("Return permission control?")
                .setMessage("This removes PrivacyClone's fixed denial and returns permission management to Android and the user. It does not automatically grant the permission again.")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("RESTORE CONTROL", (dialog, which) -> applyPhonePermissionPolicy(packageName, false))
                .show();
    }

    private void applyPhonePermissionPolicy(String packageName, boolean deny) {
        if (devicePolicyManager == null || !devicePolicyManager.isProfileOwnerApp(getPackageName())) {
            toast("Profile-owner control is not active.");
            return;
        }
        List<String> permissions = getRequestedPhonePermissions(packageName);
        if (!permissions.contains("android.permission.READ_PHONE_STATE")) {
            toast("This app does not declare READ_PHONE_STATE.");
            refreshInstalledApps();
            return;
        }
        ComponentName admin = new ComponentName(this, PrivacyDeviceAdminReceiver.class);
        int grantState = deny
                ? DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED
                : DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT;
        int success = 0;
        int failed = 0;
        for (String permission : permissions) {
            try {
                if (devicePolicyManager.setPermissionGrantState(admin, packageName, permission, grantState)) success++;
                else failed++;
            } catch (Exception e) {
                failed++;
            }
        }
        refreshInstalledApps();
        refreshPermissionAudit();
        if (failed == 0 && success > 0) {
            toast(deny ? "Phone permission access blocked for this app." : "User control restored. Review the permission in Android settings if needed.");
        } else if (success > 0) {
            toast(success + " permission(s) updated; " + failed + " could not be changed.");
        } else {
            toast("Android could not change this app's permissions. Check its target version and profile status.");
        }
    }

    private void refreshIdentifierStatus() {
        // Read only to confirm availability. Never render, persist, or transmit the identifier value.
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String status = androidId == null || androidId.length() == 0 ? "Not available" : "Available locally; value not collected";
        updateMetricByLabel("Profile-scoped Android ID", status, androidId == null ? AMBER : GREEN);
    }

    private void refreshNetworkStatus() {
        String status = "Could not determine";
        int color = AMBER;
        try {
            ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Network active = manager == null ? null : manager.getActiveNetwork();
            NetworkCapabilities caps = active == null || manager == null ? null : manager.getNetworkCapabilities(active);
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                status = "VPN transport detected";
                color = GREEN;
            } else if (caps != null) {
                status = "No VPN transport detected";
                color = AMBER;
            } else {
                status = "No active network";
                color = MUTED;
            }
        } catch (Exception ignored) {
            status = "Not available on this build";
        }
        updateMetricByLabel("VPN transport", status, color);
    }

    private void refreshSensorStatus() {
        String status = "Not available";
        try {
            SensorManager manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            List<Sensor> sensors = manager == null ? Collections.<Sensor>emptyList() : manager.getSensorList(Sensor.TYPE_ALL);
            status = sensors.size() + " sensor types reported by Android";
        } catch (Exception ignored) {
            // Keep the diagnostic non-blocking.
        }
        updateMetricByLabel("Available device sensors", status, TEXT);
    }

    private void refreshPermissionAudit() {
        if (permissionFindingsContainer == null) return;
        permissionFindingsContainer.removeAllViews();
        final Set<String> sensitive = new HashSet<>(Arrays.asList(
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.READ_CONTACTS",
                "android.permission.WRITE_CONTACTS",
                "android.permission.GET_ACCOUNTS",
                "android.permission.READ_CALL_LOG",
                "android.permission.WRITE_CALL_LOG",
                "android.permission.READ_PHONE_STATE",
                "android.permission.CALL_PHONE",
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.READ_SMS",
                "android.permission.RECEIVE_SMS",
                "android.permission.SEND_SMS",
                "android.permission.READ_CALENDAR",
                "android.permission.WRITE_CALENDAR",
                "android.permission.BODY_SENSORS"
        ));

        List<String> findings = new ArrayList<>();
        int grantedCount = 0;
        try {
            List<ApplicationInfo> apps = getPackageManager().getInstalledApplications(0);
            for (ApplicationInfo app : apps) {
                PackageInfo info;
                try {
                    info = getPackageManager().getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS);
                } catch (PackageManager.NameNotFoundException ignored) {
                    continue;
                }
                if (info.requestedPermissions == null) continue;
                List<String> granted = new ArrayList<>();
                for (String permission : info.requestedPermissions) {
                    if (sensitive.contains(permission)
                            && getPackageManager().checkPermission(permission, app.packageName) == PackageManager.PERMISSION_GRANTED) {
                        granted.add(shortPermission(permission));
                    }
                }
                if (!granted.isEmpty()) {
                    grantedCount += granted.size();
                    CharSequence label;
                    try { label = app.loadLabel(getPackageManager()); }
                    catch (Exception e) { label = app.packageName; }
                    findings.add(String.valueOf(label) + " — " + join(granted));
                }
            }
            permissionCountValue.setText(grantedCount + " granted sensitive permission entries found");
            if (findings.isEmpty()) {
                addMutedLine(permissionFindingsContainer, "No granted permissions from the audited list were found.");
            } else {
                int limit = Math.min(8, findings.size());
                for (int i = 0; i < limit; i++) addMutedLine(permissionFindingsContainer, findings.get(i));
                if (findings.size() > limit) addMutedLine(permissionFindingsContainer, "+ " + (findings.size() - limit) + " more apps with findings");
            }
        } catch (Exception e) {
            permissionCountValue.setText("Permission audit unavailable on this build");
            addMutedLine(permissionFindingsContainer, "Open Android settings to review app permissions directly.");
        }
    }

    private String shortPermission(String permission) {
        if (permission.contains("LOCATION")) return "Location";
        if (permission.contains("CONTACTS") || permission.contains("ACCOUNTS")) return "Contacts/accounts";
        if (permission.contains("CALL_LOG")) return "Call log";
        if (permission.contains("PHONE")) return "Phone";
        if (permission.contains("CAMERA")) return "Camera";
        if (permission.contains("RECORD_AUDIO")) return "Microphone";
        if (permission.contains("SMS")) return "SMS";
        if (permission.contains("CALENDAR")) return "Calendar";
        if (permission.contains("SENSORS")) return "Body sensors";
        return permission.substring(permission.lastIndexOf('.') + 1);
    }

    private String join(List<String> items) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) out.append(", ");
            out.append(items.get(i));
        }
        return out.toString();
    }

    private void updateMetricByLabel(String label, String value, int color) {
        // Find the metric row by its tag and update its value view.
        View target = content.findViewWithTag("metric:" + label);
        if (target instanceof TextView) {
            TextView valueView = (TextView) target;
            valueView.setText(value);
            valueView.setTextColor(color);
        }
    }

    private void addMetric(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowParams = params(-1, -2);
        rowParams.topMargin = dp(12);
        parent.addView(row, rowParams);
        row.addView(text(label, 11, MUTED, false));
        TextView valueView = text(value, 13, TEXT, true);
        valueView.setTag("metric:" + label);
        LinearLayout.LayoutParams valueParams = params(-1, -2);
        valueParams.topMargin = dp(3);
        row.addView(valueView, valueParams);
    }

    private void addKeyValue(LinearLayout parent, String key, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams p = params(-1, -2);
        p.topMargin = dp(10);
        parent.addView(row, p);
        TextView k = text(key, 12, MUTED, false);
        row.addView(k, new LinearLayout.LayoutParams(0, -2, 1));
        TextView v = text(value == null ? "Unknown" : value, 12, TEXT, true);
        v.setGravity(Gravity.RIGHT);
        row.addView(v, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private void sectionTitle(LinearLayout parent, String title, String subtitle) {
        TextView heading = text(title, 12, RED, true);
        heading.setLetterSpacing(0.08f);
        parent.addView(heading);
        TextView sub = text(subtitle, 15, TEXT, true);
        LinearLayout.LayoutParams p = params(-1, -2);
        p.topMargin = dp(5);
        parent.addView(sub, p);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(round(PANEL, 14, Color.rgb(39, 44, 52)));
        return card;
    }

    private void addCard(LinearLayout card) {
        LinearLayout.LayoutParams p = params(-1, -2);
        p.bottomMargin = dp(14);
        content.addView(card, p);
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(TEXT);
        button.setTextSize(12);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setLetterSpacing(0.04f);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(round(RED, 9, RED));
        return button;
    }

    private void addMutedLine(LinearLayout parent, String message) {
        TextView line = text(message, 11, MUTED, false);
        line.setPadding(dp(9), dp(8), dp(9), dp(8));
        LinearLayout.LayoutParams p = params(-1, -2);
        p.bottomMargin = dp(4);
        parent.addView(line, p);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setIncludeFontPadding(true);
        return view;
    }

    private GradientDrawable round(int color, int radiusDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeColor != Color.TRANSPARENT) drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void openSystemSettings(String action, String errorMessage) {
        try {
            startActivity(new Intent(action));
        } catch (Exception e) {
            toast(errorMessage);
        }
    }

    private void hideProfileKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (profileLabelInput != null) imm.hideSoftInputFromWindow(profileLabelInput.getWindowToken(), 0);
        } catch (Exception ignored) { }
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(packageInput.getWindowToken(), 0);
        } catch (Exception ignored) { }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private static final class AppEntry {
        final String label;
        final String packageName;
        final boolean requestsPhoneState;
        AppEntry(String label, String packageName, boolean requestsPhoneState) {
            this.label = label;
            this.packageName = packageName;
            this.requestsPhoneState = requestsPhoneState;
        }
    }
}
