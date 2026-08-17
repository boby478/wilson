package com.wilson.agent;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
    public void onInterrupt() {}

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

    public static void clickByText(String text) {
        if (instance == null) return;
        instance.doClickByText(text);
    }

    public static void typeText(String text) {
        if (instance == null) return;
        instance.doTypeText(text);
    }

    public static void pressBack() {
        if (instance == null) return;
        instance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }

    public static void scrollDown() {
        if (instance == null) return;
        instance.doScroll(true);
    }

    public static void scrollUp() {
        if (instance == null) return;
        instance.doScroll(false);
    }

    public static void clickByTextIndex(String text, int index) {
        if (instance == null) return;
        instance.doClickByTextIndex(text, index);
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

    private void doClickByText(String text) {
        if (text == null) {
            showToast("Wilson: no click target given");
            return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            showToast("Wilson: no active screen to click on");
            return;
        }

        String target = text.trim().toLowerCase();

        AccessibilityNodeInfo clickable = findClickableNodeByText(root, target, true);
        if (clickable == null) {
            clickable = findClickableNodeByText(root, target, false);
        }

        if (clickable == null) {
            showToast("Wilson: could not find a clickable match for '" + text + "'");
            return;
        }

        boolean result = clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        showToast("Wilson: clicked '" + text + "' -> " + result);
    }

    private void doClickByTextIndex(String text, int index) {
        if (text == null) {
            showToast("Wilson: no click target given");
            return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            showToast("Wilson: no active screen to click on");
            return;
        }

        String target = text.trim().toLowerCase();
        java.util.List<AccessibilityNodeInfo> matches = new java.util.ArrayList<>();
        collectClickableMatches(root, target, matches);

        if (matches.isEmpty()) {
            showToast("Wilson: no clickable matches found for '" + text + "'");
            return;
        }

        if (index < 0 || index >= matches.size()) {
            showToast("Wilson: index " + index + " out of range (" + matches.size() + " matches)");
            return;
        }

        boolean result = matches.get(index).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        showToast("Wilson: clicked match #" + index + " of " + matches.size() + " -> " + result);
    }

    private void collectClickableMatches(AccessibilityNodeInfo node, String target, java.util.List<AccessibilityNodeInfo> out) {
        if (node == null) return;

        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();

        boolean matches = (text != null && text.toString().toLowerCase().contains(target))
                || (desc != null && desc.toString().toLowerCase().contains(target));

        if (matches) {
            AccessibilityNodeInfo clickableAncestor = findClickableAncestor(node);
            if (clickableAncestor != null && !out.contains(clickableAncestor)) {
                out.add(clickableAncestor);
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            collectClickableMatches(node.getChild(i), target, out);
        }
    }

    private AccessibilityNodeInfo findClickableNodeByText(AccessibilityNodeInfo node, String target, boolean exact) {
        if (node == null) return null;

        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();

        boolean matches;
        if (exact) {
            matches = (text != null && text.toString().equalsIgnoreCase(target))
                    || (desc != null && desc.toString().equalsIgnoreCase(target));
        } else {
            matches = (text != null && text.toString().toLowerCase().contains(target))
                    || (desc != null && desc.toString().toLowerCase().contains(target));
        }

        if (matches) {
            AccessibilityNodeInfo clickableAncestor = findClickableAncestor(node);
            if (clickableAncestor != null) return clickableAncestor;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findClickableNodeByText(node.getChild(i), target, exact);
            if (result != null) return result;
        }
        return null;
    }

    private void doTypeText(String text) {
        if (text == null) {
            showToast("Wilson: no text given to type");
            return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            showToast("Wilson: no active screen to type into");
            return;
        }

        AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focused == null) {
            focused = findFirstEditable(root);
        }

        if (focused == null) {
            showToast("Wilson: no text field found to type into");
            return;
        }

        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        boolean result = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
        showToast("Wilson: typed '" + text + "' -> " + result);
    }

    private void doScroll(boolean forward) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            showToast("Wilson: no active screen to scroll");
            return;
        }
        AccessibilityNodeInfo scrollable = findScrollable(root);
        if (scrollable == null) {
            showToast("Wilson: no scrollable element found");
            return;
        }
        int action = forward ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
        boolean result = scrollable.performAction(action);
        showToast("Wilson: scrolled " + (forward ? "down" : "up") + " -> " + result);
    }

    private AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isScrollable()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findScrollable(node.getChild(i));
            if (result != null) return result;
        }
        return null;
    }

    private AccessibilityNodeInfo findFirstEditable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isEditable()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findFirstEditable(node.getChild(i));
            if (result != null) return result;
        }
        return null;
    }

    private AccessibilityNodeInfo findNodeByText(AccessibilityNodeInfo node, String target) {
        AccessibilityNodeInfo exact = findNodeByTextExact(node, target);
        if (exact != null) return exact;
        return findNodeByTextPartial(node, target);
    }

    private AccessibilityNodeInfo findNodeByTextExact(AccessibilityNodeInfo node, String target) {
        if (node == null) return null;

        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();

        if (text != null && text.toString().equalsIgnoreCase(target)) return node;
        if (desc != null && desc.toString().equalsIgnoreCase(target)) return node;

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findNodeByTextExact(node.getChild(i), target);
            if (result != null) return result;
        }
        return null;
    }

    private AccessibilityNodeInfo findNodeByTextPartial(AccessibilityNodeInfo node, String target) {
        if (node == null) return null;

        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();

        if (text != null && text.toString().toLowerCase().contains(target)) return node;
        if (desc != null && desc.toString().toLowerCase().contains(target)) return node;

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findNodeByTextPartial(node.getChild(i), target);
            if (result != null) return result;
        }
        return null;
    }

    private AccessibilityNodeInfo findClickableAncestor(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        while (current != null) {
            if (current.isClickable()) return current;
            current = current.getParent();
        }
        return null;
    }

    private void writeToFile(String content) {
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "Download");
            File outFile = new File(dir, "wilson_screen.txt");
            FileWriter writer = new FileWriter(outFile);
            writer.write(content);
            writer.close();
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
