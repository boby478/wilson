package com.wilson.agent;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import java.util.List;

public class WilsonAccessibilityService extends AccessibilityService {
    private static final String TAG = "WilsonAccessibility";
    private static WilsonAccessibilityService instance;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Intentionally left minimal for now
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted.");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(TAG, "Wilson Accessibility Service connected.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    public static boolean isRunning() {
        return instance != null;
    }

    private void showToast(String message) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    public static void launchApp(String appName) {
        if (instance == null) {
            Log.d(TAG, "Accessibility service not running, cannot launch.");
            return;
        }
        instance.doLaunchApp(appName);
    }

    private void doLaunchApp(String appName) {
        if (appName == null) {
            showToast("Wilson: no app name given");
            return;
        }

        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        String target = appName.trim().toLowerCase();

        String exactPackage = null;
        String exactLabel = null;
        String partialPackage = null;
        String partialLabel = null;

        for (ApplicationInfo app : apps) {
            Intent testIntent = pm.getLaunchIntentForPackage(app.packageName);
            if (testIntent == null) continue;

            String label = pm.getApplicationLabel(app).toString().toLowerCase();

            if (label.equals(target) && exactPackage == null) {
                exactPackage = app.packageName;
                exactLabel = label;
            } else if (label.contains(target) && partialPackage == null) {
                partialPackage = app.packageName;
                partialLabel = label;
            }
        }

        String foundPackage = exactPackage != null ? exactPackage : partialPackage;
        String foundLabel = exactPackage != null ? exactLabel : partialLabel;

        if (foundPackage == null) {
            Log.d(TAG, "No launchable app found for: " + appName);
            showToast("Wilson: no launchable app found matching '" + appName + "'");
            return;
        }

        Intent launchIntent = pm.getLaunchIntentForPackage(foundPackage);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launchIntent);
        Log.d(TAG, "Launched: " + foundPackage);
        showToast("Wilson: launched " + foundLabel);
    }
}
