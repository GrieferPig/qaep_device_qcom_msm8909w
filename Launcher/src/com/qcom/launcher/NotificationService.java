/*
 * Copyright (c) 2024, The Linux Foundation. All rights reserved.
 *
 * Notification Listener Service for msm8909w
 * Listens for system notifications and displays them as fullscreen popups
 */

package com.qcom.launcher;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * Background service that listens for system notifications.
 * When a notification is posted, it wakes the screen and shows a fullscreen popup.
 */
public class NotificationService extends NotificationListenerService {

    private static final String TAG = "NotificationService";
    public static final String ACTION_SHOW_NOTIFICATION = "com.qcom.launcher.SHOW_NOTIFICATION";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_PACKAGE = "package";
    public static final String EXTRA_APP_NAME = "app_name";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "NotificationService created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "NotificationService destroyed");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;

        String packageName = sbn.getPackageName();
        
        // Skip notifications from our own package
        if (getPackageName().equals(packageName)) {
            return;
        }

        Notification notification = sbn.getNotification();
        if (notification == null) return;

        Bundle extras = notification.extras;
        if (extras == null) return;

        // Extract notification content
        CharSequence titleCs = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence textCs = extras.getCharSequence(Notification.EXTRA_TEXT);
        
        String title = titleCs != null ? titleCs.toString() : "";
        String text = textCs != null ? textCs.toString() : "";

        // Get app name
        String appName = "";
        try {
            appName = getPackageManager().getApplicationLabel(
                getPackageManager().getApplicationInfo(packageName, 0)).toString();
        } catch (Exception e) {
            appName = packageName;
        }

        // Skip empty notifications
        if (title.isEmpty() && text.isEmpty()) {
            return;
        }

        Log.d(TAG, "Notification received from " + appName + ": " + title + " - " + text);

        // Wake the screen
        wakeScreen();

        // Show the notification popup
        showNotificationPopup(title, text, packageName, appName);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Optional: handle notification removal
    }

    private void wakeScreen() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) {
            PowerManager.WakeLock wakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE,
                "Launcher:NotificationWakeLock");
            wakeLock.acquire(5000); // Hold for 5 seconds max
        }
    }

    private void showNotificationPopup(String title, String text, String packageName, String appName) {
        Intent intent = new Intent(this, NotificationPopupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                       Intent.FLAG_ACTIVITY_CLEAR_TOP |
                       Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_TEXT, text);
        intent.putExtra(EXTRA_PACKAGE, packageName);
        intent.putExtra(EXTRA_APP_NAME, appName);
        startActivity(intent);
    }
}
