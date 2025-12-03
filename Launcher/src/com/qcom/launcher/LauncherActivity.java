/*
 * Copyright (c) 2024, The Linux Foundation. All rights reserved.
 *
 * Simple Launcher Activity for msm8909w with horizontal paging and watchface
 */

package com.qcom.launcher;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Simple launcher activity with horizontal paging.
 * Left page: Debug/test page
 * Middle page: Digital watchface
 * Right page: App list (vertical, 5 apps per screen)
 * Power button: on apps page -> go to watchface, on watchface -> sleep
 */
public class LauncherActivity extends Activity {

    private static final String ACTION_OVERLAY_POWER_KEY = "com.qcom.overlay.POWER_KEY";
    private static final String EXTRA_POWER_KEY_ACTION = "action";

    private static final int PAGE_DEBUG = 0;
    private static final int PAGE_WATCHFACE = 1;
    private static final int PAGE_APPS = 2;

    private static final int PERIODIC_INTERVAL = 30000; // 30 seconds
    private static final int NOTIFICATION_ID = 1001;

    // Cover to sleep: keycode 254 (0x00fe) is sent by cyttsp5 touchscreen driver
    // when a large area is detected (palm/hand covering screen)
    private static final int KEYCODE_LARGE_TOUCH = 254;

    // Packages to hide from launcher
    private static final Set<String> HIDDEN_PACKAGES = new HashSet<>();
    static {
        HIDDEN_PACKAGES.add("com.android.settings");           // AOSP Settings
        HIDDEN_PACKAGES.add("com.qualcomm.settings");          // Qualcomm Settings
        HIDDEN_PACKAGES.add("com.qti.qualcomm.settings");      // Qualcomm Settings alternate
        HIDDEN_PACKAGES.add("com.android.providers.downloads.ui"); // DownloadUI
        HIDDEN_PACKAGES.add("com.android.documentsui");        // Downloads/Files
    }

    private ViewPager mViewPager;
    private LauncherPagerAdapter mPagerAdapter;
    private PowerManager.WakeLock mWakeLock;
    private Handler mHandler = new Handler();
    private boolean mPeriodicEnabled = false;
    private int mNotificationCounter = 0;

    // Receive power button broadcasts from PhoneWindowManager
    private BroadcastReceiver mPowerKeyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_OVERLAY_POWER_KEY.equals(intent.getAction())) {
                int action = intent.getIntExtra(EXTRA_POWER_KEY_ACTION, -1);
                if (action == 0) { // ACTION_DOWN
                    handlePowerKey();
                }
            }
        }
    };

    // Receive package install/uninstall/update broadcasts
    private BroadcastReceiver mPackageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_PACKAGE_ADDED.equals(action) ||
                Intent.ACTION_PACKAGE_REMOVED.equals(action) ||
                Intent.ACTION_PACKAGE_REPLACED.equals(action) ||
                Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
                // Refresh app list
                if (mPagerAdapter != null) {
                    mPagerAdapter.refreshAppList();
                }
            }
        }
    };

    private Runnable mPeriodicNotificationRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPeriodicEnabled) {
                sendTestNotification();
                mHandler.postDelayed(this, PERIODIC_INTERVAL);
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
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
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

        setContentView(R.layout.activity_launcher);

        // Acquire wake lock to prevent screen off
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Launcher:WakeLock");
        mWakeLock.acquire();

        // Register for power key broadcast from PhoneWindowManager
        IntentFilter powerFilter = new IntentFilter(ACTION_OVERLAY_POWER_KEY);
        registerReceiver(mPowerKeyReceiver, powerFilter);

        // Register for package changes
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packageFilter.addDataScheme("package");
        registerReceiver(mPackageReceiver, packageFilter);

        // Enable notification listener on first launch
        enableNotificationListener();

        setupViewPager();
        startClockUpdates();
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
            unregisterReceiver(mPackageReceiver);
        } catch (Exception e) {
            // Ignore
        }
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        mHandler.removeCallbacksAndMessages(null);
    }

    private void enableNotificationListener() {
        try {
            String packageName = getPackageName();
            ComponentName cn = new ComponentName(packageName, 
                packageName + ".NotificationService");
            String flat = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
            
            if (flat == null || !flat.contains(cn.flattenToString())) {
                if (flat == null || flat.isEmpty()) {
                    flat = cn.flattenToString();
                } else {
                    flat = flat + ":" + cn.flattenToString();
                }
                Settings.Secure.putString(getContentResolver(),
                    "enabled_notification_listeners", flat);
            }
        } catch (Exception e) {
            // Ignore - may need WRITE_SECURE_SETTINGS permission
        }
    }

    private void setupViewPager() {
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mPagerAdapter = new LauncherPagerAdapter();
        mViewPager.setAdapter(mPagerAdapter);
        // Start on watchface page (middle page)
        mViewPager.setCurrentItem(PAGE_WATCHFACE);
    }

    private void handlePowerKey() {
        int currentPage = mViewPager.getCurrentItem();
        if (currentPage == PAGE_WATCHFACE) {
            // On watchface: go to sleep
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            pm.goToSleep(System.currentTimeMillis(),
                    PowerManager.GO_TO_SLEEP_REASON_POWER_BUTTON, 0);
        } else {
            // On other pages: go to watchface
            mViewPager.setCurrentItem(PAGE_WATCHFACE, true);
        }
    }

    private void startClockUpdates() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateClock();
                mHandler.postDelayed(this, 1000); // Update every second
            }
        }, 1000);
    }

    private void updateClock() {
        if (mPagerAdapter != null) {
            mPagerAdapter.updateWatchface();
        }
    }

    /**
     * Handle key events for cover-to-sleep detection.
     * The cyttsp5 touchscreen driver sends keycode 254 (0x00fe) when a large area
     * is detected (palm/hand covering the screen).
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KEYCODE_LARGE_TOUCH) {
            Log.d("LauncherActivity", "Large touch detected (cover screen), going to sleep");
            goToSleep();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void goToSleep() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            pm.goToSleep(System.currentTimeMillis(),
                    PowerManager.GO_TO_SLEEP_REASON_POWER_BUTTON, 0);
        }
    }

    /**
     * Wake the screen when touched.
     * Called from touch events to ensure screen wakes on any touch.
     */
    private void wakeScreen() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) {
            PowerManager.WakeLock wl = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE,
                "Launcher:TouchWakeLock");
            wl.acquire(3000); // Hold for 3 seconds max
        }
    }

    private void sendTestNotification() {
        mNotificationCounter++;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        Intent intent = new Intent(this, LauncherActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(getString(R.string.test_notification_title) + " #" + mNotificationCounter)
            .setContentText(getString(R.string.test_notification_text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL);

        nm.notify(NOTIFICATION_ID + mNotificationCounter, builder.build());
    }

    private void togglePeriodicNotification(TextView button, TextView status) {
        mPeriodicEnabled = !mPeriodicEnabled;
        
        if (mPeriodicEnabled) {
            button.setText(R.string.stop_periodic);
            status.setText(R.string.periodic_running);
            status.setVisibility(View.VISIBLE);
            mHandler.postDelayed(mPeriodicNotificationRunnable, PERIODIC_INTERVAL);
        } else {
            button.setText(R.string.start_periodic);
            status.setVisibility(View.GONE);
            mHandler.removeCallbacks(mPeriodicNotificationRunnable);
        }
    }

    private class LauncherPagerAdapter extends PagerAdapter {
        private View mDebugPage;
        private View mAppsPage;
        private View mWatchfacePage;
        private TextView mTimeText;
        private TextView mDateText;
        private AppsAdapter mAppsAdapter;
        private ListView mListView;

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            LayoutInflater inflater = LayoutInflater.from(LauncherActivity.this);

            if (position == PAGE_DEBUG) {
                mDebugPage = inflater.inflate(R.layout.page_debug, container, false);
                setupDebugPage(mDebugPage);
                container.addView(mDebugPage);
                return mDebugPage;
            } else if (position == PAGE_APPS) {
                mAppsPage = inflater.inflate(R.layout.page_apps, container, false);
                setupAppsPage(mAppsPage);
                container.addView(mAppsPage);
                return mAppsPage;
            } else {
                mWatchfacePage = inflater.inflate(R.layout.page_watchface, container, false);
                mTimeText = (TextView) mWatchfacePage.findViewById(R.id.time_text);
                mDateText = (TextView) mWatchfacePage.findViewById(R.id.date_text);
                updateWatchface();
                container.addView(mWatchfacePage);
                return mWatchfacePage;
            }
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        private void setupDebugPage(View page) {
            final TextView sendButton = (TextView) page.findViewById(R.id.btn_send_notification);
            final TextView periodicButton = (TextView) page.findViewById(R.id.btn_periodic_notification);
            final TextView statusText = (TextView) page.findViewById(R.id.periodic_status);

            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendTestNotification();
                }
            });

            periodicButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    togglePeriodicNotification(periodicButton, statusText);
                }
            });

            // Update button state if periodic is already running
            if (mPeriodicEnabled) {
                periodicButton.setText(R.string.stop_periodic);
                statusText.setText(R.string.periodic_running);
                statusText.setVisibility(View.VISIBLE);
            }
        }

        private void setupAppsPage(View page) {
            mListView = (ListView) page.findViewById(R.id.app_list);
            mAppsAdapter = new AppsAdapter();
            mListView.setAdapter(mAppsAdapter);

            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    AppInfo app = (AppInfo) mAppsAdapter.getItem(position);
                    if (app != null) {
                        try {
                            Intent intent = new Intent();
                            intent.setComponent(app.componentName);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                }
            });
        }

        public void refreshAppList() {
            if (mAppsAdapter != null) {
                mAppsAdapter.reload();
            }
        }

        public void updateWatchface() {
            if (mTimeText != null && mDateText != null) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());

                Date now = new Date();
                mTimeText.setText(timeFormat.format(now));
                mDateText.setText(dateFormat.format(now));
            }
        }
    }

    private class AppsAdapter extends BaseAdapter {
        private List<AppInfo> mApps = new ArrayList<>();

        public AppsAdapter() {
            loadApps();
        }

        public void reload() {
            loadApps();
            notifyDataSetChanged();
        }

        private void loadApps() {
            mApps.clear();
            PackageManager pm = getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
            for (ResolveInfo info : activities) {
                if (info.activityInfo != null) {
                    String packageName = info.activityInfo.packageName;
                    
                    // Skip hidden packages
                    if (HIDDEN_PACKAGES.contains(packageName)) {
                        continue;
                    }
                    
                    // Skip our own launcher
                    if (packageName.equals(getPackageName())) {
                        continue;
                    }

                    AppInfo app = new AppInfo();
                    app.label = info.loadLabel(pm).toString();
                    app.icon = info.activityInfo.loadIcon(pm);
                    app.componentName = new ComponentName(
                        packageName,
                        info.activityInfo.name);
                    mApps.add(app);
                }
            }

            // Sort alphabetically
            Collections.sort(mApps, new Comparator<AppInfo>() {
                @Override
                public int compare(AppInfo a, AppInfo b) {
                    return a.label.compareToIgnoreCase(b.label);
                }
            });
        }

        @Override
        public int getCount() {
            return mApps.size();
        }

        @Override
        public Object getItem(int position) {
            return mApps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(LauncherActivity.this)
                    .inflate(R.layout.item_app, parent, false);
            }

            AppInfo app = mApps.get(position);

            ImageView iconView = (ImageView) convertView.findViewById(R.id.app_icon);
            TextView nameView = (TextView) convertView.findViewById(R.id.app_name);

            if (app.icon != null) {
                iconView.setImageDrawable(app.icon);
            }
            nameView.setText(app.label);

            return convertView;
        }
    }

    private static class AppInfo {
        String label;
        Drawable icon;
        ComponentName componentName;
    }
}