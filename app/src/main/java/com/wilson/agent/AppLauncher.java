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
            Log.d(TAG, "No app name given.");
            showToast(context, "Wilson: no app name given");
            return;
        }

        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        String target = appName.trim().toLowerCase();
        String foundPackage = null;
        String foundLabel = null;

        for (ApplicationInfo app : apps) {
            String label = pm.getApplicationLabel(app).toString().toLowerCase();
            if (label.equals(target) || label.contains(target)) {
                foundPackage = app.packageName;
                foundLabel = label;
                Log.d(TAG, "Match found: " + label + " -> " + foundPackage);
                break;
            }
        }

        if (foundPackage == null) {
            Log.d(TAG, "No matching app found for: " + appName);
            showToast(context, "Wilson: no app found matching '" + appName + "'");
            return;
        }

        Intent launchIntent = pm.getLaunchIntentForPackage(foundPackage);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
            Log.d(TAG, "Launched: " + foundPackage);
            showToast(context, "Wilson: launched " + foundLabel);
        } else {
            Log.d(TAG, "Could not get launch intent for: " + foundPackage);
            showToast(context, "Wilson: found '" + foundLabel + "' but it has no launch intent");
        }
    }
}
