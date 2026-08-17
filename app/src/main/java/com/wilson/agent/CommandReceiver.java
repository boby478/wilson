package com.wilson.agent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CommandReceiver extends BroadcastReceiver {
    private static final String TAG = "WilsonReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String command = intent.getStringExtra("command");
        String target = intent.getStringExtra("target");

        Log.d(TAG, "Command: " + command + " | Target: " + target);

        if (command == null) return;
        if (!WilsonAccessibilityService.isRunning()) {
            Log.d(TAG, "Accessibility service not connected.");
            return;
        }

        switch (command) {
            case "open_app":
                WilsonAccessibilityService.launchApp(target);
                break;
            case "read_screen":
                WilsonAccessibilityService.readScreen();
                break;
            case "click_text":
                WilsonAccessibilityService.clickByText(target);
                break;
            case "type_text":
                WilsonAccessibilityService.typeText(target);
                break;
            case "press_back":
                WilsonAccessibilityService.pressBack();
                break;
            case "scroll_down":
                WilsonAccessibilityService.scrollDown();
                break;
            case "scroll_up":
                WilsonAccessibilityService.scrollUp();
                break;
            default:
                Log.d(TAG, "Unknown command: " + command);
        }
    }
}
