package com.wilson.agent;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class WilsonAccessibilityService extends AccessibilityService {
    private static final String TAG = "WilsonAccessibility";

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
        Log.d(TAG, "Wilson Accessibility Service connected.");
    }
}
