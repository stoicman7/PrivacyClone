package com.privacyclone.app;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.os.AsyncTask;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.os.VUserInfo;
import com.lody.virtual.os.VUserManager;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;

/**
 * Local-first Android app cloner prototype.
 * The virtual engine supplies network capability for apps that the user clones.
 */
public final class PrivacyCloneActivity extends Activity {
    private static final int REQUEST_IMPORT_APK = 4201;
    private static final int REQUEST_CLONE_PERMISSIONS = 4202;
    private static final int BG = Color.rgb(11, 13, 16);
    private static final int PANEL = Color.rgb(22, 25, 30);
    private static final int PANEL_ALT = Color.rgb(29, 33, 39);
    private static final int TEXT = Color.rgb(242, 244, 247);
    private static final int MUTED = Color.rgb(157, 166, 178);
    private static final int RED = Color.rgb(149, 6, 6);
    private static final int GREEN = Color.rgb(91, 176, 126);
    private static final int AMBER = Color.rgb(218, 164, 77);

    private LinearLayout root;
    private LinearLayout content;
    private TextView profileStatus;
    private TextView profileDetails;
    private EditText packageInput;
    private EditText profileLabelInput;
    private LinearLayout installedAppsContainer;
    private LinearLayout permissionFindingsContainer;
    private TextView appCountValue;
    private TextView permissionCountValue;
    private TextView vpnValue;
    private TextView sensorValue;
    private AppEntry pendingCloneLaunch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        if (requestCode == REQUEST_IMPORT_APK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            importApkFromUri(data.getData());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CLONE_PERMISSIONS && pendingCloneLaunch != null) {
            AppEntry entry = pendingCloneLaunch;
            pendingCloneLaunch = null;
            launchCloneNow(entry);
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

        TextView footer = text("LOCAL-FIRST DESIGN  •  NO ANALYTICS  •  CLONED APPS MAY USE THE NETWORK", 10, MUTED, true);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = params(-1, -2);
        footerParams.topMargin = dp(24);
        content.addView(footer, footerParams);
    }

    private void addProfileCard() {
        LinearLayout card = card();
        sectionTitle(card, "VIRTUAL CONTAINER", "Separate app data without an Android Work Profile");
        profileStatus = text("Checking profile…", 19, TEXT, true);
        LinearLayout.LayoutParams statusParams = params(-1, -2);
        statusParams.topMargin = dp(13);
        card.addView(profileStatus, statusParams);
        profileDetails = text("Checking the local virtual-app engine.", 12, MUTED, false);
        LinearLayout.LayoutParams detailsParams = params(-1, -2);
        detailsParams.topMargin = dp(6);
        card.addView(profileDetails, detailsParams);
        addCard(card);
    }

    private void addCloneCard() {
        LinearLayout card = card();
        sectionTitle(card, "CLONE MANAGER", "Import installed apps or APK files into this phone's virtual container");

        TextView note = text("Each clone is placed in its own virtual user with a separate app-data directory. This is not an Android Work Profile or an OS security boundary. Runtime permissions are currently granted to the PrivacyClone host and may be shared across clones. Some split-APK apps and apps that depend on Google Play services may not work.", 12, MUTED, false);
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

        Button install = button("SELECT OR CLONE INSTALLED APP");
        LinearLayout.LayoutParams installParams = params(-1, dp(46));
        installParams.topMargin = dp(10);
        card.addView(install, installParams);
        install.setOnClickListener(v -> cloneInstalledApp());

        Button importApk = button("IMPORT APK FILE");
        LinearLayout.LayoutParams importParams = params(-1, dp(46));
        importParams.topMargin = dp(8);
        card.addView(importApk, importParams);
        importApk.setOnClickListener(v -> chooseApkFile());

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
        sectionTitle(card, "PRIVACY OVERVIEW", "What the host device exposes");
        addMetric(card, "Host Android ID availability", "Checking…");
        addMetric(card, "VPN transport", "Checking…");
        addMetric(card, "Available device sensors", "Checking…");
        TextView disclaimer = text("Virtual clones have separate app data, but they still run on this physical phone. This engine does not guarantee separate hardware identifiers, network identity, or invisibility from app-side virtualization checks.", 11, MUTED, false);
        LinearLayout.LayoutParams p = params(-1, -2);
        p.topMargin = dp(13);
        card.addView(disclaimer, p);

        TextView permissionNotice = text("The embedded engine declares broad permissions, including Internet and app capabilities such as location, contacts, camera, microphone, phone and SMS. Cloned apps may use the network. Runtime grants requested here belong to PrivacyClone and may be shared across clones; PrivacyClone does not upload its own diagnostics.", 11, MUTED, false);
        LinearLayout.LayoutParams permissionParams = params(-1, -2);
        permissionParams.topMargin = dp(10);
        card.addView(permissionNotice, permissionParams);
        addCard(card);
    }

    private void addConfigurationCard() {
        LinearLayout card = card();
        sectionTitle(card, "PRIVACY CONFIGURATION", "Local settings for this virtual workspace");

        TextView labelHelp = text(
                "Choose a local label to distinguish this PrivacyClone workspace. It changes only the label shown here; it does not rename the phone or alter identifiers seen by other apps.",
                11, MUTED, false);
        LinearLayout.LayoutParams helpParams = params(-1, -2);
        helpParams.topMargin = dp(12);
        card.addView(labelHelp, helpParams);

        profileLabelInput = new EditText(this);
        profileLabelInput.setSingleLine(true);
        profileLabelInput.setHint("For example: Personal clones");
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
        sectionTitle(card, "HOST PERMISSION AUDIT", "Sensitive permissions granted to Android-installed apps");
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
        addKeyValue(card, "Container permissions", "Virtualized apps may request capabilities through the host");
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
        boolean ready = VirtualCore.get().isStartup();
        if (ready) {
            profileStatus.setText("Virtual container initialized");
            profileStatus.setTextColor(GREEN);
            profileDetails.setText("Clones run inside PrivacyClone. Each clone gets a separate virtual user and app-data directory; no Android Work Profile is created.");
        } else {
            profileStatus.setText("Virtual container unavailable");
            profileStatus.setTextColor(AMBER);
            profileDetails.setText("The virtual engine did not initialize. Restart PrivacyClone and check the build/device compatibility.");
        }
    }

    private void cloneInstalledApp() {
        String packageName = packageInput.getText().toString().trim();
        if (packageName.isEmpty()) {
            showInstalledAppPicker();
            return;
        }
        if (!packageName.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")) {
            packageInput.setError("Enter a valid Android package name");
            return;
        }
        try {
            PackageInfo info = getPackageManager().getPackageInfo(packageName, 0);
            if (info.applicationInfo == null || info.applicationInfo.sourceDir == null) {
                toast("Android did not expose the installed app's APK path.");
                return;
            }
            if (info.applicationInfo.splitSourceDirs != null && info.applicationInfo.splitSourceDirs.length > 0) {
                toast("This app uses split APKs. This prototype currently imports single-file APKs only.");
                return;
            }
            String label = String.valueOf(info.applicationInfo.loadLabel(getPackageManager()));
            hideKeyboard();
            installVirtualClone(info.applicationInfo.sourceDir, label);
        } catch (PackageManager.NameNotFoundException e) {
            packageInput.setError("This app is not installed on the phone");
        } catch (Exception e) {
            toast("Could not read the installed app. Try importing its APK.");
        }
    }

    private void showInstalledAppPicker() {
        try {
            Intent launcher = new Intent(Intent.ACTION_MAIN);
            launcher.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> activities = getPackageManager().queryIntentActivities(launcher, 0);
            Collections.sort(activities, (a, b) ->
                    String.valueOf(a.loadLabel(getPackageManager())).toLowerCase(Locale.US)
                            .compareTo(String.valueOf(b.loadLabel(getPackageManager())).toLowerCase(Locale.US)));

            List<ResolveInfo> choices = new ArrayList<>();
            Set<String> packages = new HashSet<>();
            for (ResolveInfo info : activities) {
                if (info.activityInfo == null) continue;
                String packageName = info.activityInfo.packageName;
                if (packageName.equals(getPackageName()) || !packages.add(packageName)) continue;
                choices.add(info);
            }
            if (choices.isEmpty()) {
                toast("No launchable installed apps were found.");
                return;
            }
            String[] labels = new String[choices.size()];
            for (int i = 0; i < choices.size(); i++) {
                ResolveInfo info = choices.get(i);
                labels[i] = String.valueOf(info.loadLabel(getPackageManager()));
            }
            new AlertDialog.Builder(this)
                    .setTitle("Choose an installed app to clone")
                    .setItems(labels, (dialog, which) -> {
                        String packageName = choices.get(which).activityInfo.packageName;
                        packageInput.setText(packageName);
                        cloneInstalledApp();
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
        } catch (Exception e) {
            toast("Android could not list installed apps.");
        }
    }

    private void chooseApkFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.android.package-archive");
        try {
            startActivityForResult(intent, REQUEST_IMPORT_APK);
        } catch (Exception e) {
            toast("The Android file picker is not available.");
        }
    }

    private void importApkFromUri(final Uri uri) {
        new AsyncTask<Void, Void, File>() {
            @Override
            protected File doInBackground(Void... ignored) {
                File directory = new File(getFilesDir(), "apk-imports");
                if (!directory.exists() && !directory.mkdirs()) return null;
                File output = new File(directory, UUID.randomUUID().toString() + ".apk");
                try (InputStream input = getContentResolver().openInputStream(uri);
                     FileOutputStream stream = new FileOutputStream(output)) {
                    if (input == null) return null;
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) stream.write(buffer, 0, count);
                    return output;
                } catch (Exception e) {
                    output.delete();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(File apkFile) {
                if (apkFile == null || !apkFile.isFile() || apkFile.length() == 0) {
                    toast("The selected APK could not be read.");
                    return;
                }
                PackageInfo archive = getPackageManager().getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
                if (archive == null || archive.packageName == null) {
                    apkFile.delete();
                    toast("The selected file is not a valid APK.");
                    return;
                }
                archive.applicationInfo.sourceDir = apkFile.getAbsolutePath();
                archive.applicationInfo.publicSourceDir = apkFile.getAbsolutePath();
                String label = String.valueOf(archive.applicationInfo.loadLabel(getPackageManager()));
                installVirtualClone(apkFile.getAbsolutePath(), label);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void installVirtualClone(final String apkPath, final String appLabel) {
        appCountValue.setText("Importing " + appLabel + "…");
        new AsyncTask<Void, Void, CloneTaskResult>() {
            @Override
            protected CloneTaskResult doInBackground(Void... ignored) {
                try {
                    VirtualCore core = VirtualCore.get();
                    PackageInfo archive = getPackageManager().getPackageArchiveInfo(apkPath, 0);
                    if (archive == null || archive.packageName == null) {
                        return CloneTaskResult.failure("The APK package information could not be read.");
                    }
                    String packageName = archive.packageName;
                    InstalledAppInfo existing = core.getInstalledAppInfo(packageName, 0);
                    if (existing == null) {
                        InstallResult installed = core.installPackage(
                                apkPath, InstallStrategy.COMPARE_VERSION | InstallStrategy.SKIP_DEX_OPT);
                        if (installed == null || !installed.isSuccess) {
                            return CloneTaskResult.failure(installed == null || installed.error == null
                                    ? "The virtual engine could not import this APK."
                                    : installed.error);
                        }
                    }
                    VUserInfo user = VUserManager.get().createUser(appLabel + " clone", VUserInfo.FLAG_ADMIN);
                    if (user == null) return CloneTaskResult.failure("The virtual engine could not create a separate clone space.");
                    if (!core.installPackageAsUser(user.id, packageName)) {
                        VUserManager.get().removeUser(user.id);
                        return CloneTaskResult.failure("The app was imported, but could not be installed into its clone space.");
                    }
                    return CloneTaskResult.success(new AppEntry(appLabel, packageName, user.id));
                } catch (Throwable e) {
                    return CloneTaskResult.failure("Import failed: " + e.getClass().getSimpleName());
                }
            }

            @Override
            protected void onPostExecute(CloneTaskResult result) {
                if (result == null || result.entry == null) {
                    toast(result == null ? "Import failed." : result.message);
                    refreshInstalledApps();
                    return;
                }
                saveCloneEntry(result.entry);
                toast(appLabel + " was added as a separate clone.");
                refreshInstalledApps();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void saveCloneEntry(AppEntry entry) {
        SharedPreferences preferences = getSharedPreferences("privacyclone_instances", MODE_PRIVATE);
        Set<String> saved = new HashSet<>(preferences.getStringSet("clones", Collections.<String>emptySet()));
        saved.add(entry.userId + "|" + entry.packageName + "|" + entry.label.replace("|", ""));
        preferences.edit().putStringSet("clones", saved).apply();
    }

    private List<AppEntry> loadCloneEntries() {
        SharedPreferences preferences = getSharedPreferences("privacyclone_instances", MODE_PRIVATE);
        Set<String> saved = preferences.getStringSet("clones", Collections.<String>emptySet());
        List<AppEntry> entries = new ArrayList<>();
        for (String item : saved) {
            String[] parts = item.split("\\|", 3);
            if (parts.length != 3) continue;
            try {
                entries.add(new AppEntry(parts[2], parts[1], Integer.parseInt(parts[0])));
            } catch (NumberFormatException ignored) { }
        }
        return entries;
    }

    private void removeClone(final AppEntry entry) {
        new AlertDialog.Builder(this)
                .setTitle("Remove clone?")
                .setMessage("This removes the virtual user and its local app data for " + entry.label + ". The original app installed on Android is not removed.")
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("REMOVE", (dialog, which) -> {
                    boolean removed = VUserManager.get().removeUser(entry.userId);
                    SharedPreferences preferences = getSharedPreferences("privacyclone_instances", MODE_PRIVATE);
                    Set<String> saved = new HashSet<>(preferences.getStringSet("clones", Collections.<String>emptySet()));
                    saved.remove(entry.userId + "|" + entry.packageName + "|" + entry.label.replace("|", ""));
                    preferences.edit().putStringSet("clones", saved).apply();
                    refreshInstalledApps();
                    toast(removed ? "Clone and its virtual user data were removed." : "The virtual user could not be removed.");
                })
                .show();
    }

    private void openClone(AppEntry entry) {
        List<String> missing = getMissingClonePermissions(entry);
        if (missing.isEmpty()) {
            launchCloneNow(entry);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Permissions requested by this clone")
                .setMessage("The cloned app requests: " + join(missing) + ". Android will grant these permissions to PrivacyClone, which hosts the virtual apps. These grants are not isolated per clone in this prototype and may be shared by other clones. You can skip this and try launching with those permissions denied.")
                .setNegativeButton("SKIP", (dialog, which) -> launchCloneNow(entry))
                .setPositiveButton("CONTINUE", (dialog, which) -> {
                    pendingCloneLaunch = entry;
                    requestPermissions(missing.toArray(new String[0]), REQUEST_CLONE_PERMISSIONS);
                })
                .show();
    }

    private List<String> getMissingClonePermissions(AppEntry entry) {
        List<String> missing = new ArrayList<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return missing;
        try {
            InstalledAppInfo installed = VirtualCore.get().getInstalledAppInfo(entry.packageName, 0);
            if (installed == null) return missing;
            PackageInfo virtualPackage = installed.getPackageInfo(entry.userId);
            if (virtualPackage == null || virtualPackage.requestedPermissions == null) return missing;

            PackageInfo hostPackage = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            Set<String> hostDeclared = new HashSet<>();
            if (hostPackage.requestedPermissions != null) {
                hostDeclared.addAll(Arrays.asList(hostPackage.requestedPermissions));
            }

            for (String permission : virtualPackage.requestedPermissions) {
                if (!hostDeclared.contains(permission) || checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                    continue;
                }
                try {
                    PermissionInfo permissionInfo = getPackageManager().getPermissionInfo(permission, 0);
                    int protection = permissionInfo.protectionLevel & PermissionInfo.PROTECTION_MASK_BASE;
                    if (protection == PermissionInfo.PROTECTION_DANGEROUS && !missing.contains(permission)) {
                        missing.add(permission);
                    }
                } catch (PackageManager.NameNotFoundException ignored) {
                    // Custom permissions are not requested through Android's runtime dialog.
                }
            }
        } catch (Exception ignored) {
            // Permission discovery must not prevent the user from launching the clone.
        }
        return missing;
    }

    private void launchCloneNow(AppEntry entry) {
        try {
            Intent launch = VirtualCore.get().getLaunchIntent(entry.packageName, entry.userId);
            if (launch == null) {
                toast("This clone has no launchable activity.");
                return;
            }
            VActivityManager.get().startActivity(launch, entry.userId);
        } catch (Throwable e) {
            toast("The clone could not be started on this device.");
        }
    }

    private void refreshInstalledApps() {
        if (installedAppsContainer == null) return;
        installedAppsContainer.removeAllViews();
        List<AppEntry> entries = loadCloneEntries();
        List<AppEntry> active = new ArrayList<>();
        for (AppEntry entry : entries) {
            if (VirtualCore.get().isAppInstalledAsUser(entry.userId, entry.packageName)) active.add(entry);
        }
        Collections.sort(active, (a, b) -> a.label.toLowerCase(Locale.US).compareTo(b.label.toLowerCase(Locale.US)));
        appCountValue.setText(active.size() + " virtual clone(s)");
        if (active.isEmpty()) {
            addMutedLine(installedAppsContainer, "No clones yet. Clone an installed app or import a single-file APK above.");
            return;
        }
        for (AppEntry entry : active) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(10), dp(9), dp(10), dp(10));
            row.setBackground(round(PANEL_ALT, 8, Color.TRANSPARENT));
            LinearLayout.LayoutParams rowParams = params(-1, -2);
            rowParams.bottomMargin = dp(8);
            installedAppsContainer.addView(row, rowParams);
            row.addView(text(entry.label, 14, TEXT, true), params(-1, -2));
            TextView details = text(entry.packageName + "  •  Virtual user " + entry.userId, 10, MUTED, false);
            LinearLayout.LayoutParams detailsParams = params(-1, -2);
            detailsParams.topMargin = dp(3);
            row.addView(details, detailsParams);
            Button open = button("OPEN CLONE");
            LinearLayout.LayoutParams openParams = params(-1, dp(40));
            openParams.topMargin = dp(8);
            row.addView(open, openParams);
            open.setOnClickListener(v -> openClone(entry));
            Button remove = button("REMOVE CLONE");
            LinearLayout.LayoutParams removeParams = params(-1, dp(38));
            removeParams.topMargin = dp(6);
            row.addView(remove, removeParams);
            remove.setOnClickListener(v -> removeClone(entry));
        }
    }

    private void refreshIdentifierStatus() {
        // Read only to confirm availability. Never render, persist, or transmit the identifier value.
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String status = androidId == null || androidId.length() == 0 ? "Not available" : "Available locally; value not collected";
        updateMetricByLabel("Host Android ID availability", status, androidId == null ? AMBER : GREEN);
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
        final int userId;
        AppEntry(String label, String packageName, int userId) {
            this.label = label;
            this.packageName = packageName;
            this.userId = userId;
        }
    }

    private static final class CloneTaskResult {
        final AppEntry entry;
        final String message;
        private CloneTaskResult(AppEntry entry, String message) {
            this.entry = entry;
            this.message = message;
        }
        static CloneTaskResult success(AppEntry entry) {
            return new CloneTaskResult(entry, "Clone created.");
        }
        static CloneTaskResult failure(String message) {
            return new CloneTaskResult(null, message);
        }
    }
}
