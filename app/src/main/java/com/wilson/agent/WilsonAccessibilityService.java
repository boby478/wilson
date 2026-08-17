package com.wilson.agent;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class WilsonAccessibilityService extends AccessibilityService {
    private static final String TAG = "WilsonAccessibility";
    private static WilsonAccessibilityService instance;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

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
        if (instance == null) return;
        instance.doLaunchApp(appName);
    }

    public static void readScreen() {
        if (instance == null) return;
        instance.doReadScreen();
    }

    private void doLaunchApp(String appName) {
        if (appName == null) {
            showToast("Wilson: no app name given");
            return;
        }

        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        String target = appName.trim().toLowerCase();

        String exactPackage = null, exactLabel = null, partialPackage = null, partialLabel = null;

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
            showToast("Wilson: no launchable app found matching '" + appName + "'");
            return;
        }

        Intent launchIntent = pm.getLaunchIntentForPackage(foundPackage);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launchIntent);
        showToast("Wilson: launched " + foundLabel);
    }

    private void doReadScreen() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        StringBuilder summary = new StringBuilder();

        if (root == null) {
            summary.append("ERROR: root node is null\n");
        } else {
            int count = walkNode(root, summary, 0);
            summary.insert(0, "Found " + count + " elements\n");
        }

        writeToFile(summary.toString());
        showToast("Wilson: screen read saved to file");
    }

    private void writeToFile(String content) {
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "Download");
            File outFile = new File(dir, "wilson_screen.txt");
            FileWriter writer = new FileWriter(outFile);
            writer.write(content);
            writer.close();
            Log.d(TAG, "Wrote screen dump to " + outFile.getAbsolutePath());
        } catch (IOException e) {
            Log.d(TAG, "Failed to write screen dump: " + e.getMessage());
        }
    }

    private int walkNode(AccessibilityNodeInfo node, StringBuilder out, int count) {
        if (node == null) return count;

        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        String className = node.getClassName() != null ? node.getClassName().toString() : "?";

        if ((text != null && text.length() > 0) || (desc != null && desc.length() > 0) || node.isClickable()) {
            count++;
            out.append("[").append(count).append("] ")
               .append(className)
               .append(node.isClickable() ? " (clickable)" : "")
               .append(" text='").append(text).append("'")
               .append(" desc='").append(desc).append("'")
               .append("\n");
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            count = walkNode(node.getChild(i), out, count);
        }

        return count;
    }
}
