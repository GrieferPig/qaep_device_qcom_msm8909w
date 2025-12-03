/*
 * Copyright (c) 2024, The Linux Foundation. All rights reserved.
 *
 * Placeholder ASSIST app for msm8909w
 * Shows a TODO toast and vibrates when ASSIST intent is received.
 * Power button: short press exits, long press shows TODO toast again.
 */

package com.qcom.assistplaceholder;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

/**
 * Placeholder activity for ASSIST intent.
 * Vibrates and shows a TODO toast when launched.
 * Power button short press: exit (via broadcast from PhoneWindowManager)
 * Power button long press: show TODO toast again
 */
public class AssistActivity extends Activity {

    private static final String ACTION_OVERLAY_POWER_KEY = "com.qcom.overlay.POWER_KEY";
    private static final String EXTRA_POWER_KEY_ACTION = "action";
    private static final String EXTRA_POWER_KEY_REPEAT_COUNT = "repeat_count";

    private static final long VIBRATE_DURATION_MS = 400;

    private FrameLayout mRootLayout;
    private View mContentLayout;
    private boolean mIsFinishing = false;
    private PowerManager.WakeLock mWakeLock;
    private Handler mHandler = new Handler();

    // Receive power button broadcasts from PhoneWindowManager
    private BroadcastReceiver mPowerKeyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_OVERLAY_POWER_KEY.equals(intent.getAction())) {
                int action = intent.getIntExtra(EXTRA_POWER_KEY_ACTION, KeyEvent.ACTION_UP);
                // Power key down - exit immediately (PhoneWindowManager only sends ACTION_DOWN)
                if (action == KeyEvent.ACTION_DOWN) {
                    finishImmediately();
                }
            }
        }
    };

    // Track power button via screen off broadcast (fallback)
    private BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                // Power button was pressed and screen is turning off
                // We need to keep the screen on and handle this as our exit
                if (!mIsFinishing) {
                    // Turn screen back on and exit
                    wakeUpScreen();
                    finishImmediately();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set window flags to intercept power button behavior and hide status bar
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // Hide status bar and navigation bar completely
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_assist);

        mRootLayout = (FrameLayout) findViewById(R.id.root_layout);
        mContentLayout = findViewById(R.id.content_layout);

        // Acquire wake lock to prevent screen off
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AssistPlaceholder:WakeLock");
        mWakeLock.acquire();

        // Register for power key broadcast from PhoneWindowManager
        IntentFilter powerFilter = new IntentFilter(ACTION_OVERLAY_POWER_KEY);
        registerReceiver(mPowerKeyReceiver, powerFilter);

        // Register for screen off broadcast as fallback
        IntentFilter screenFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenReceiver, screenFilter);

        // Vibrate to indicate activation
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VIBRATE_DURATION_MS);
        }

        // Show TODO toast
        showTodoToast();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mPowerKeyReceiver);
        } catch (Exception e) {
            // Ignore
        }
        try {
            unregisterReceiver(mScreenReceiver);
        } catch (Exception e) {
            // Ignore
        }
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private void wakeUpScreen() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AssistPlaceholder:WakeUp");
        wl.acquire(1000);
    }

    private void showTodoToast() {
        Toast.makeText(this, R.string.todo_message, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Power key events are now handled via broadcast from PhoneWindowManager
        // This is kept as a fallback but shouldn't receive power key events
        if (event.getKeyCode() == KeyEvent.KEYCODE_POWER) {
            return true; // Consume but ignore - handled by broadcast
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        finishImmediately();
    }

    private void finishImmediately() {
        if (mIsFinishing) return;
        mIsFinishing = true;
        finish();
        // Let the theme's fade_out animation play
    }
}
