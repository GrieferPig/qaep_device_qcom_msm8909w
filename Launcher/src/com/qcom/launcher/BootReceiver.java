/*
 * Copyright (c) 2024, The Linux Foundation. All rights reserved.
 *
 * Boot receiver to ensure notification listener service is enabled
 */

package com.qcom.launcher;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

/**
 * Receives BOOT_COMPLETED broadcast to enable notification listener.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed, enabling notification listener");
            
            // Enable the notification listener service
            enableNotificationListener(context);
        }
    }

    private void enableNotificationListener(Context context) {
        try {
            String packageName = context.getPackageName();
            ComponentName cn = new ComponentName(packageName, 
                packageName + ".NotificationService");
            String flat = Settings.Secure.getString(context.getContentResolver(),
                "enabled_notification_listeners");
            
            if (flat == null || !flat.contains(cn.flattenToString())) {
                if (flat == null || flat.isEmpty()) {
                    flat = cn.flattenToString();
                } else {
                    flat = flat + ":" + cn.flattenToString();
                }
                Settings.Secure.putString(context.getContentResolver(),
                    "enabled_notification_listeners", flat);
                Log.d(TAG, "Notification listener enabled: " + cn.flattenToString());
            } else {
                Log.d(TAG, "Notification listener already enabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable notification listener", e);
        }
    }
}
