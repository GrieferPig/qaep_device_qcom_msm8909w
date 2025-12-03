/*
 * Copyright (c) 2024, The Linux Foundation. All rights reserved.
 *
 * Service to monitor for MENU key and launch task switcher.
 * Uses InputManager to monitor key events system-wide.
 */

package com.qcom.taskswitcher;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.KeyEvent;

/**
 * Service that monitors for MENU key events using InputManager.
 * Requires platform signature to access hidden APIs.
 */
public class MenuKeyService extends Service {

    private static final String TAG = "TaskSwitcher";
    private InputManager mInputManager;
    private Handler mHandler;
    private KeyInputEventReceiver mInputEventReceiver;
    private InputChannel mInputChannel;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MenuKeyService created");
        
        mHandler = new Handler(Looper.getMainLooper());
        mInputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
        
        // Try to set up input monitoring
        try {
            setupInputMonitor();
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup input monitor: " + e.getMessage());
        }
    }

    private void setupInputMonitor() {
        // Note: This requires hidden API access which platform-signed apps can use
        // The actual implementation depends on the Android version
        // For now, we rely on the BroadcastReceiver approach
        Log.d(TAG, "Input monitor setup - relying on broadcast receiver");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MenuKeyService started");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MenuKeyService destroyed");
        
        if (mInputEventReceiver != null) {
            mInputEventReceiver.dispose();
            mInputEventReceiver = null;
        }
    }

    private void launchTaskSwitcher() {
        Log.d(TAG, "Launching TaskSwitcher from service");
        Intent switcherIntent = new Intent(this, TaskSwitcherActivity.class);
        switcherIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(switcherIntent);
    }

    private class KeyInputEventReceiver extends InputEventReceiver {
        public KeyInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
        }

        @Override
        public void onInputEvent(InputEvent event) {
            try {
                if (event instanceof KeyEvent) {
                    KeyEvent keyEvent = (KeyEvent) event;
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_MENU &&
                        keyEvent.getAction() == KeyEvent.ACTION_UP) {
                        Log.d(TAG, "MENU key detected via InputEventReceiver");
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                launchTaskSwitcher();
                            }
                        });
                    }
                }
            } finally {
                finishInputEvent(event, true);
            }
        }
    }
}
