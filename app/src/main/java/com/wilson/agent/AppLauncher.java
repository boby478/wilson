package com.wilson.agent;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class AppLauncher {
    private static final String TAG = "WilsonLauncher";

    private static void showToast(Context context, String message) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }

    public static void launchApp(Context context, String appName) {
        if (appName == null) {
            showToast(context, "Wilson: no app name given");
            return;
        }

        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        String target = appName.trim().toLowerCase();

        String exactPackage = null;
        String exactLabel = null;
        String partialPackage = null;
        String partialLabel = null;

        for (ApplicationInfo app : apps) {
            // Only consider apps that are actually launchable
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
            showToast(context, "Wilson: no launchable app found matching '" + appName + "'");
            return;
        }

        Intent launchIntent = pm.getLaunchIntentForPackage(foundPackage);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launchIntent);
        Log.d(TAG, "Launched: " + foundPackage);
        showToast(context, "Wilson: launched " + foundLabel);
    }
}
