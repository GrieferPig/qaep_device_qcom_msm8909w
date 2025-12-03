/*
 * Copyright (c) 2024, The Linux Foundation. All rights reserved.
 *
 * Receiver for recent apps toggle broadcast.
 */

package com.qcom.taskswitcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receives CLOSE_SYSTEM_DIALOGS broadcast which is sent when
 * toggleRecentApps is called. We check for "recentapps" reason.
 */
public class MenuKeyReceiver extends BroadcastReceiver {
    
    private static final String TAG = "TaskSwitcher";
    private static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action)) {
            String reason = intent.getStringExtra("reason");
            Log.d(TAG, "CLOSE_SYSTEM_DIALOGS reason: " + reason);
            
            // Check for recentapps reason - this is sent by toggleRecentApps()
            if (SYSTEM_DIALOG_REASON_RECENT_APPS.equals(reason)) {
                launchTaskSwitcher(context);
            }
        } else if ("com.qcom.taskswitcher.TOGGLE".equals(action)) {
            launchTaskSwitcher(context);
        }
    }
    
    private void launchTaskSwitcher(Context context) {
        Log.d(TAG, "Launching TaskSwitcher");
        Intent switcherIntent = new Intent(context, TaskSwitcherActivity.class);
        switcherIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(switcherIntent);
    }
}
