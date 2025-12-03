/*
 * Copyright (c) 2024, The Linux Foundation. All rights reserved.
 *
 * Boot receiver to start the MenuKeyService.
 */

package com.qcom.taskswitcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receives boot completed broadcast and starts the MenuKeyService.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "TaskSwitcher";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed, starting MenuKeyService");
            
            // Start the key monitoring service
            Intent serviceIntent = new Intent(context, MenuKeyService.class);
            context.startService(serviceIntent);
        }
    }
}
