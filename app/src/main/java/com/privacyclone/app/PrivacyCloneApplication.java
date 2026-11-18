package com.privacyclone.app;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import com.lody.virtual.client.NativeEngine;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.stub.VASettings;

/**
 * Starts the embedded VirtualApp engine before any Activity tries to use it.
 * The initializer intentionally installs no analytics, crash-upload, or Xposed hooks.
 */
public final class PrivacyCloneApplication extends Application {
    private static volatile String startupError;
    private static volatile boolean initializationComplete;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                NativeEngine.disableJit(Build.VERSION.SDK_INT);
            }
            VASettings.ENABLE_IO_REDIRECT = true;
            VASettings.ENABLE_INNER_SHORTCUT = false;
            VirtualCore.get().startup(base);
        } catch (Throwable error) {
            startupError = error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (!VirtualCore.get().isStartup()) return;

        try {
            VirtualCore core = VirtualCore.get();
            core.initialize(new VirtualCore.VirtualInitializer() {
                @Override
                public void onMainProcess() {
                    // Keep host-process initialization local and telemetry-free.
                }

                @Override
                public void onVirtualProcess() {
                    // Do not spoof device identifiers. The UI explains this limitation.
                }

                @Override
                public void onServerProcess() {
                    // Package install/update prompts from inside clones are not silently
                    // accepted. Import APKs through PrivacyClone's explicit import action.
                    core.setAppRequestListener(new VirtualCore.AppRequestListener() {
                        @Override
                        public void onRequestInstall(String path) {
                            // No automatic installation from a cloned app.
                        }

                        @Override
                        public void onRequestUninstall(String packageName) {
                            // Clone removal is controlled by the PrivacyClone UI.
                        }
                    });
                }
            });
            initializationComplete = true;
        } catch (Throwable error) {
            startupError = error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage());
        }
    }

    public static boolean isReady() {
        return initializationComplete && VirtualCore.get().isStartup();
    }

    public static String getStartupError() {
        return startupError;
    }
}
