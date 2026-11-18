package com.privacyclone.probe;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.webkit.WebView;
import android.util.DisplayMetrics;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * A local-only probe for comparing a normal Android installation with the
 * same app hosted by PrivacyClone's VirtualApp container.
 *
 * It deliberately does not read device identifiers or contact any server.
 */
public final class ProbeActivity extends Activity {
    private static final int BACKGROUND = 0xFFF3F4F6;
    private static final int PANEL = 0xFFFFFFFF;
    private static final int TEXT = 0xFF17202A;
    private static final int MUTED = 0xFF536273;

    private TextView reportView;
    private String currentReport = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildInterface();
        refreshReport();
    }

    private void buildInterface() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView title = new TextView(this);
        title.setText("VIRTUALIZATION PROBE");
        title.setTextColor(TEXT);
        title.setTextSize(21);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Run once normally, then clone this same test app in PrivacyClone. Compare the two local reports.");
        subtitle.setTextColor(MUTED);
        subtitle.setTextSize(13);
        LinearLayout.LayoutParams subtitleParams = params(-1, -2);
        subtitleParams.topMargin = dp(7);
        root.addView(subtitle, subtitleParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionsParams = params(-1, dp(46));
        actionsParams.topMargin = dp(16);
        root.addView(actions, actionsParams);

        Button refresh = new Button(this);
        refresh.setText("REFRESH");
        refresh.setOnClickListener(v -> refreshReport());
        actions.addView(refresh, new LinearLayout.LayoutParams(0, -1, 1f));

        Button copy = new Button(this);
        copy.setText("COPY REPORT");
        copy.setOnClickListener(v -> copyReport());
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -1, 1f);
        copyParams.leftMargin = dp(8);
        actions.addView(copy, copyParams);

        reportView = new TextView(this);
        reportView.setTextColor(TEXT);
        reportView.setTextSize(12);
        reportView.setTypeface(android.graphics.Typeface.MONOSPACE);
        reportView.setTextIsSelectable(true);
        reportView.setPadding(dp(13), dp(14), dp(13), dp(14));
        reportView.setBackgroundColor(PANEL);
        LinearLayout.LayoutParams reportParams = params(-1, -2);
        reportParams.topMargin = dp(14);
        root.addView(reportView, reportParams);

        TextView privacy = new TextView(this);
        privacy.setText("LOCAL-ONLY TEST. This app has no Internet permission and requests no runtime permissions. It does not read Android ID, advertising IDs, IMEI/IMSI, serial number, MAC/BSSID, IP address, or location. UID and path values are shown only on this screen so you can compare Android's API view between the normal and virtual launch.");
        privacy.setTextColor(MUTED);
        privacy.setTextSize(11);
        LinearLayout.LayoutParams privacyParams = params(-1, -2);
        privacyParams.topMargin = dp(13);
        root.addView(privacy, privacyParams);

        setContentView(scroll);
    }

    private void refreshReport() {
        StringBuilder out = new StringBuilder(1800);
        append(out, "Captured locally", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(new Date()));
        append(out, "App package", getPackageName());
        append(out, "App version", ownVersion());
        append(out, "Android release / API", Build.VERSION.RELEASE + " / " + Build.VERSION.SDK_INT);
        append(out, "Security patch", Build.VERSION.SECURITY_PATCH);
        append(out, "Build display", Build.DISPLAY);
        append(out, "Manufacturer / brand", Build.MANUFACTURER + " / " + Build.BRAND);
        append(out, "Device model", Build.MODEL);
        append(out, "Process bitness", Process.is64Bit() ? "64-bit" : "32-bit");
        append(out, "Supported ABIs", Arrays.toString(Build.SUPPORTED_ABIS));
        append(out, "Process UID (Android API)", String.valueOf(Process.myUid()));

        try {
            ApplicationInfo app = getApplicationInfo();
            append(out, "PackageManager ApplicationInfo UID", String.valueOf(app.uid));
            append(out, "Application process name", app.processName);
            append(out, "APK source path", app.sourceDir);
            append(out, "Split APK count", app.splitSourceDirs == null ? "0" : String.valueOf(app.splitSourceDirs.length));
            append(out, "Application data directory", app.dataDir);
            append(out, "Files directory", getFilesDir().getAbsolutePath());
            append(out, "Cache directory", getCacheDir().getAbsolutePath());
            append(out, "Native library directory", app.nativeLibraryDir);
        } catch (Exception e) {
            append(out, "Application paths", "Unavailable: " + e.getClass().getSimpleName());
        }

        append(out, "Google Play services", packageStatus("com.google.android.gms"));
        append(out, "Google Play Store", packageStatus("com.android.vending"));
        append(out, "WebView provider", webViewProvider());
        append(out, "Camera feature", feature(PackageManager.FEATURE_CAMERA_ANY));
        append(out, "GPS feature", feature(PackageManager.FEATURE_LOCATION_GPS));
        append(out, "Microphone feature", feature(PackageManager.FEATURE_MICROPHONE));
        append(out, "Telephony feature", feature(PackageManager.FEATURE_TELEPHONY));
        append(out, "Bluetooth feature", feature(PackageManager.FEATURE_BLUETOOTH));

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        append(out, "Display pixels / density", metrics.widthPixels + " x " + metrics.heightPixels + " / " + metrics.densityDpi + " dpi");

        try {
            SensorManager manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            append(out, "Reported sensor count", manager == null ? "Unavailable" : String.valueOf(manager.getSensorList(Sensor.TYPE_ALL).size()));
        } catch (Exception e) {
            append(out, "Reported sensor count", "Unavailable: " + e.getClass().getSimpleName());
        }

        try {
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
            if (manager != null) {
                manager.getMemoryInfo(memory);
                append(out, "Total device memory", android.text.format.Formatter.formatFileSize(this, memory.totalMem));
                append(out, "Available device memory", android.text.format.Formatter.formatFileSize(this, memory.availMem));
            }
        } catch (Exception e) {
            append(out, "Memory information", "Unavailable: " + e.getClass().getSimpleName());
        }

        out.append("\nINTERPRETATION NOTES\n");
        out.append("- Compare the normal and cloned reports; do not assume one field alone explains an app decision.\n");
        out.append("- Process UID and ApplicationInfo UID are Android API values. A virtual framework may intercept APIs, so these are not proof of the kernel credential.\n");
        out.append("- Different data paths are expected for isolated app data. Matching physical-device facts are expected because both runs use the same phone.\n");
        out.append("- Missing Play services or a WebView provider can indicate a compatibility dependency, but does not prove why another app displays a warning.\n");

        currentReport = out.toString();
        reportView.setText(currentReport);
    }

    private String ownVersion() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName + " (" + info.versionCode + ")";
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    private String packageStatus(String packageName) {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(packageName, 0);
            return "Present, version " + info.versionName + " (" + info.versionCode + ")";
        } catch (PackageManager.NameNotFoundException e) {
            return "Not visible to this app's PackageManager";
        } catch (Exception e) {
            return "Check failed: " + e.getClass().getSimpleName();
        }
    }

    private String webViewProvider() {
        try {
            PackageInfo info = WebView.getCurrentWebViewPackage();
            if (info == null) return "No provider returned by Android";
            return info.packageName + " " + String.valueOf(info.versionName);
        } catch (Throwable e) {
            return "Unavailable: " + e.getClass().getSimpleName();
        }
    }

    private String feature(String name) {
        return getPackageManager().hasSystemFeature(name) ? "Yes" : "No";
    }

    private void append(StringBuilder out, String label, String value) {
        out.append(label).append(": ")
                .append(value == null ? "Unavailable" : value)
                .append('\n');
    }

    private void copyReport() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard == null) {
                Toast.makeText(this, "Clipboard unavailable.", Toast.LENGTH_SHORT).show();
                return;
            }
            clipboard.setPrimaryClip(ClipData.newPlainText("Virtualization Probe report", currentReport));
            Toast.makeText(this, "Report copied locally. Nothing was transmitted.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not copy the report.", Toast.LENGTH_SHORT).show();
        }
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
