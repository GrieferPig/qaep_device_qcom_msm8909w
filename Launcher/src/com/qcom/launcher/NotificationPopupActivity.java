/*
 * Copyright (c) 2024, The Linux Foundation. All rights reserved.
 *
 * Notification Popup Activity for msm8909w
 * Displays notifications as fullscreen popups with blur background
 */

package com.qcom.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Fullscreen activity to display notification content.
 * Shows with blur background, plays notification sound, and auto-dismisses.
 */
public class NotificationPopupActivity extends Activity {

    private static final int AUTO_DISMISS_DELAY = 10000; // 10 seconds
    
    private Handler mHandler = new Handler();
    private PowerManager.WakeLock mWakeLock;
    private Ringtone mRingtone;

    private Runnable mDismissRunnable = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set window flags to show over lock screen and turn on screen
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_BLUR_BEHIND |
            WindowManager.LayoutParams.FLAG_DIM_BEHIND
        );

        // Set blur amount
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.dimAmount = 0.6f;
        getWindow().setAttributes(params);

        // Hide system UI
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_notification_popup);

        // Acquire wake lock
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK |
            PowerManager.ACQUIRE_CAUSES_WAKEUP |
            PowerManager.ON_AFTER_RELEASE,
            "Launcher:PopupWakeLock");
        mWakeLock.acquire(AUTO_DISMISS_DELAY + 1000);

        // Get notification data from intent
        Intent intent = getIntent();
        String title = intent.getStringExtra(NotificationService.EXTRA_TITLE);
        String text = intent.getStringExtra(NotificationService.EXTRA_TEXT);
        String packageName = intent.getStringExtra(NotificationService.EXTRA_PACKAGE);
        String appName = intent.getStringExtra(NotificationService.EXTRA_APP_NAME);

        // Set up views
        TextView appNameView = (TextView) findViewById(R.id.notification_app_name);
        TextView titleView = (TextView) findViewById(R.id.notification_title);
        TextView textView = (TextView) findViewById(R.id.notification_text);
        ImageView iconView = (ImageView) findViewById(R.id.notification_icon);
        View dismissButton = findViewById(R.id.notification_dismiss);

        if (appName != null && !appName.isEmpty()) {
            appNameView.setText(appName);
        }
        if (title != null && !title.isEmpty()) {
            titleView.setText(title);
        } else {
            titleView.setVisibility(View.GONE);
        }
        if (text != null && !text.isEmpty()) {
            textView.setText(text);
        } else {
            textView.setVisibility(View.GONE);
        }

        // Try to get app icon
        if (packageName != null) {
            try {
                Drawable icon = getPackageManager().getApplicationIcon(packageName);
                iconView.setImageDrawable(icon);
            } catch (Exception e) {
                iconView.setVisibility(View.GONE);
            }
        }

        // Dismiss on tap anywhere or button
        View rootView = findViewById(R.id.notification_root);
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Play notification sound
        playNotificationSound();

        // Vibrate
        vibrate();

        // Auto-dismiss after delay
        mHandler.postDelayed(mDismissRunnable, AUTO_DISMISS_DELAY);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        
        // Reset auto-dismiss timer
        mHandler.removeCallbacks(mDismissRunnable);
        mHandler.postDelayed(mDismissRunnable, AUTO_DISMISS_DELAY);

        // Update content
        String title = intent.getStringExtra(NotificationService.EXTRA_TITLE);
        String text = intent.getStringExtra(NotificationService.EXTRA_TEXT);
        String appName = intent.getStringExtra(NotificationService.EXTRA_APP_NAME);
        String packageName = intent.getStringExtra(NotificationService.EXTRA_PACKAGE);

        TextView appNameView = (TextView) findViewById(R.id.notification_app_name);
        TextView titleView = (TextView) findViewById(R.id.notification_title);
        TextView textView = (TextView) findViewById(R.id.notification_text);
        ImageView iconView = (ImageView) findViewById(R.id.notification_icon);

        if (appName != null) appNameView.setText(appName);
        if (title != null) {
            titleView.setText(title);
            titleView.setVisibility(View.VISIBLE);
        }
        if (text != null) {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        }
        if (packageName != null) {
            try {
                Drawable icon = getPackageManager().getApplicationIcon(packageName);
                iconView.setImageDrawable(icon);
                iconView.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                // Ignore
            }
        }

        // Play sound again
        playNotificationSound();
        vibrate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mDismissRunnable);
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        if (mRingtone != null && mRingtone.isPlaying()) {
            mRingtone.stop();
        }
    }

    private void playNotificationSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            mRingtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
            if (mRingtone != null) {
                mRingtone.play();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private void vibrate() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(new long[]{0, 200, 100, 200}, -1);
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    @Override
    public void onBackPressed() {
        // Dismiss on back press
        finish();
    }
}
