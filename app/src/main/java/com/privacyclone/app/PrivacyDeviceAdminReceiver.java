package com.privacyclone.app;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/** Receives Android's managed-profile provisioning callbacks. */
public final class PrivacyDeviceAdminReceiver extends DeviceAdminReceiver {
    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        DevicePolicyManager manager =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(context, PrivacyDeviceAdminReceiver.class);

        if (manager != null && manager.isProfileOwnerApp(context.getPackageName())) {
            try {
                manager.setProfileName(admin, "PrivacyClone");
            } catch (RuntimeException ignored) {
                // Profile naming is optional; provisioning must not fail because of it.
            }
        }

        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(launch);
        }
    }
}
