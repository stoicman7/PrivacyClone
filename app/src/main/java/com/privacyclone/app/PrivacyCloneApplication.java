package com.privacyclone.app;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Process;

import java.io.File;
import java.util.Arrays;

import com.lody.virtual.client.NativeEngine;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.ServiceManagerNative;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.os.VUserInfo;
import com.lody.virtual.os.VUserManager;

import java.util.List;

/**
 * Starts VirtualApp and waits for its Binder server before exposing the container as ready.
 */
public final class PrivacyCloneApplication extends Application {
    private static volatile String startupError;
    private static volatile boolean initializationComplete;
    private static volatile boolean startupPending;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (!isSupportedNativeRuntime(base)) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                NativeEngine.disableJit(Build.VERSION.SDK_INT);
            }
            VASettings.ENABLE_IO_REDIRECT = true;
            VASettings.ENABLE_INNER_SHORTCUT = false;
            VirtualCore.get().startup(base);
        } catch (Throwable error) {
            startupError = describe(error);
        }
    }

    /**
     * This VirtualApp build packages only 64-bit native ABIs (arm64-v8a and x86_64).
     * Fail clearly before loading native engine code on unsupported 32-bit runtimes.
     */
    private static boolean isSupportedNativeRuntime(Context base) {
        String[] abis = Build.SUPPORTED_ABIS;
        boolean hasSupportedAbi = false;
        if (abis != null) {
            for (String abi : abis) {
                if ("arm64-v8a".equals(abi) || "x86_64".equals(abi)) {
                    hasSupportedAbi = true;
                    break;
                }
            }
        }

        if (!Process.is64Bit() || !hasSupportedAbi) {
            startupError = "Unsupported native runtime. This build includes arm64-v8a and x86_64 only; "
                    + "process bitness is " + (Process.is64Bit() ? "64-bit" : "32-bit")
                    + " and Android reports ABIs " + Arrays.toString(abis) + ".";
            initializationComplete = false;
            startupPending = false;
            return false;
        }

        String nativeDirectory = base.getApplicationInfo().nativeLibraryDir;
        File nativeLibrary = new File(nativeDirectory, "libva++.so");
        if (!nativeLibrary.isFile()) {
            startupError = "VirtualApp native library libva++.so is missing from "
                    + nativeDirectory + ". Rebuild and inspect the APK's packaged native libraries.";
            initializationComplete = false;
            startupPending = false;
            return false;
        }
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        VirtualCore core = VirtualCore.get();
        if (!core.isStartup()) return;

        try {
            core.initialize(new VirtualCore.VirtualInitializer() {
                @Override
                public void onMainProcess() {
                    // Host initialization stays local and telemetry-free.
                }

                @Override
                public void onVirtualProcess() {
                    // Do not spoof device identifiers.
                }

                @Override
                public void onServerProcess() {
                    core.setAppRequestListener(new VirtualCore.AppRequestListener() {
                        @Override
                        public void onRequestInstall(String path) {
                            // APK installation is controlled by the explicit host UI.
                        }

                        @Override
                        public void onRequestUninstall(String packageName) {
                            // Clone removal is controlled by the explicit host UI.
                        }
                    });
                }
            });

            // VirtualApp's upstream launcher explicitly starts its Binder server before
            // making virtual app/user APIs available. Do the same off the UI thread.
            if (core.isMainProcess()) {
                startupPending = true;
                Thread serverStartup = new Thread(() -> ensureServerReady(),
                        "PrivacyClone-VirtualApp-Server");
                serverStartup.start();
            }
        } catch (Throwable error) {
            startupError = describe(error);
            startupPending = false;
        }
    }

    /**
     * Starts/reconnects VirtualApp's server and verifies a real Binder round-trip.
     * Call only from a worker thread; the provider startup can take time.
     */
    public static synchronized boolean ensureServerReady() {
        VirtualCore core = VirtualCore.get();
        if (!core.isStartup() || !core.isMainProcess()) {
            startupError = "VirtualApp is not running in the host process.";
            startupPending = false;
            return false;
        }

        startupPending = true;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                ServiceManagerNative.clearServerFetcher();

                if (!core.isEngineLaunched()) {
                    core.waitForEngine();
                }

                long deadline = System.currentTimeMillis() + 4000L;
                while (!core.isEngineLaunched() && System.currentTimeMillis() < deadline) {
                    Thread.sleep(100L);
                }
                if (!core.isEngineLaunched()) {
                    throw new IllegalStateException("VirtualApp server process did not start.");
                }

                List<VUserInfo> users = VUserManager.get().getUsers();
                if (users == null) {
                    throw new IllegalStateException("VirtualApp server started but its Binder service did not respond.");
                }

                startupError = null;
                initializationComplete = true;
                startupPending = false;
                return true;
            } catch (Throwable error) {
                startupError = describe(error);
                ServiceManagerNative.clearServerFetcher();
                if (attempt == 0) {
                    try {
                        Thread.sleep(300L);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        startupError = "Server startup was interrupted.";
                        break;
                    }
                }
            }
        }

        initializationComplete = false;
        startupPending = false;
        return false;
    }

    public static boolean isReady() {
        return initializationComplete && VirtualCore.get().isStartup();
    }

    public static boolean isStartupPending() {
        return startupPending;
    }

    public static String getStartupError() {
        return startupError;
    }

    private static String describe(Throwable error) {
        String message = error.getMessage();
        return error.getClass().getSimpleName()
                + (message == null || message.trim().isEmpty() ? "" : ": " + message);
    }
}
