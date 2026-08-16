package com.wilson.agent;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.List;

public class AppLauncher {
    private static final String TAG = "WilsonLauncher";

    public static void launchApp(Context context, String appName) {
        if (appName == null) {
            Log.d(TAG, "No app name given.");
            return;
        }

        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        String target = appName.trim().toLowerCase();
        String foundPackage = null;

        for (ApplicationInfo app : apps) {
            String label = pm.getApplicationLabel(app).toString().toLowerCase();
            if (label.equals(target) || label.contains(target)) {
                foundPackage = app.packageName;
                Log.d(TAG, "Match found: " + label + " -> " + foundPackage);
                break;
            }
        }

        if (foundPackage == null) {
            Log.d(TAG, "No matching app found for: " + appName);
            return;
        }

        Intent launchIntent = pm.getLaunchIntentForPackage(foundPackage);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
            Log.d(TAG, "Launched: " + foundPackage);
        } else {
            Log.d(TAG, "Could not get launch intent for: " + foundPackage);
        }
    }
}
