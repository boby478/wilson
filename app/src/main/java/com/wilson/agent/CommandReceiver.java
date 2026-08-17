package com.wilson.agent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CommandReceiver extends BroadcastReceiver {
    private static final String TAG = "WilsonReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Broadcast received!");

        String command = intent.getStringExtra("command");
        String target = intent.getStringExtra("target");

        Log.d(TAG, "Command: " + command + " | Target: " + target);

        if (command == null) {
            Log.d(TAG, "No command extra found.");
            return;
        }

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
            default:
                Log.d(TAG, "Unknown command: " + command);
        }
    }
}
