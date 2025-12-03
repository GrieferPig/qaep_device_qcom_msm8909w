/*
 * Copyright (c) 2024, The Linux Foundation. All rights reserved.
 *
 * Simple Task Switcher Activity for msm8909w
 * Shows recent apps in a simple list view.
 * Swipe left on an item to kill the app with animation.
 * Swipe anywhere outside list to dismiss immediately.
 * Power button short press exits the activity.
 */

package com.qcom.taskswitcher;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Simple task switcher activity showing recent apps.
 * Tap an app to switch to it, swipe left to kill it with animation.
 * Power button short press exits (via broadcast from PhoneWindowManager).
 */
public class TaskSwitcherActivity extends Activity {

    private static final String ACTION_OVERLAY_POWER_KEY = "com.qcom.overlay.POWER_KEY";
    private static final String EXTRA_POWER_KEY_ACTION = "action";
    private static final String EXTRA_POWER_KEY_REPEAT_COUNT = "repeat_count";

    private static final int MAX_RECENT_TASKS = 10;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    private FrameLayout mRootLayout;
    private View mContentLayout;
    private ListView mTaskList;
    private TextView mEmptyText;
    private ActivityManager mActivityManager;
    private PackageManager mPackageManager;
    private List<TaskInfo> mTasks = new ArrayList<>();
    private TaskAdapter mAdapter;
    private GestureDetector mListGestureDetector;
    private boolean mIsFinishing = false;
    private PowerManager.WakeLock mWakeLock;
    private Set<String> mLauncherComponents = new HashSet<>();

    // Receive power button broadcasts from PhoneWindowManager
    private BroadcastReceiver mPowerKeyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_OVERLAY_POWER_KEY.equals(intent.getAction())) {
                int action = intent.getIntExtra(EXTRA_POWER_KEY_ACTION, KeyEvent.ACTION_UP);
                // Short press - exit immediately
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
                // Power button was pressed - keep screen on and exit
                if (!mIsFinishing) {
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

        setContentView(R.layout.activity_task_switcher);

        mRootLayout = (FrameLayout) findViewById(R.id.root_layout);
        mContentLayout = findViewById(R.id.content_layout);
        mTaskList = (ListView) findViewById(R.id.task_list);
        mEmptyText = (TextView) findViewById(R.id.empty_text);

        mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        mPackageManager = getPackageManager();

        // Acquire wake lock to prevent screen off
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "TaskSwitcher:WakeLock");
        mWakeLock.acquire();

        // Register for power key broadcast from PhoneWindowManager
        IntentFilter powerFilter = new IntentFilter(ACTION_OVERLAY_POWER_KEY);
        registerReceiver(mPowerKeyReceiver, powerFilter);

        // Register for screen off broadcast as fallback
        IntentFilter screenFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenReceiver, screenFilter);

        // Get launcher packages to hide from recent apps
        findLauncherPackages();

        loadRecentTasks();

        mAdapter = new TaskAdapter();
        mTaskList.setAdapter(mAdapter);

        // Set up gesture detector for list item swipe (to kill apps)
        mListGestureDetector = new GestureDetector(this, new ListSwipeGestureListener());

        mTaskList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mListGestureDetector.onTouchEvent(event);
            }
        });

        mTaskList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switchToTask(position);
            }
        });
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
            "TaskSwitcher:WakeUp");
        wl.acquire(1000);
    }

    private void findLauncherPackages() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> launchers = mPackageManager.queryIntentActivities(homeIntent, 0);
        for (ResolveInfo info : launchers) {
            if (info.activityInfo != null) {
                // Store full component name (package/class) to avoid filtering out
                // apps like Settings that have a Home settings activity
                String componentName = info.activityInfo.packageName + "/" + info.activityInfo.name;
                mLauncherComponents.add(componentName);
            }
        }
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            // Menu key pressed again - dismiss
            finishImmediately();
            return true;
        }
        return super.onKeyDown(keyCode, event);
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

    private void loadRecentTasks() {
        mTasks.clear();

        List<ActivityManager.RecentTaskInfo> recentTasks =
            mActivityManager.getRecentTasks(MAX_RECENT_TASKS, ActivityManager.RECENT_IGNORE_UNAVAILABLE);

        String myPackage = getPackageName();

        for (ActivityManager.RecentTaskInfo taskInfo : recentTasks) {
            if (taskInfo.baseIntent != null && taskInfo.baseIntent.getComponent() != null) {
                ComponentName component = taskInfo.baseIntent.getComponent();
                String packageName = component.getPackageName();
                String componentName = packageName + "/" + component.getClassName();

                // Skip our own app, system UI, AssistPlaceholder, and actual launcher activities
                if (packageName.equals(myPackage) ||
                    packageName.equals("com.android.systemui") ||
                    packageName.equals("com.qcom.assistplaceholder") ||
                    mLauncherComponents.contains(componentName)) {
                    continue;
                }

                TaskInfo task = new TaskInfo();
                task.taskId = taskInfo.persistentId;
                task.packageName = packageName;
                task.baseIntent = taskInfo.baseIntent;

                try {
                    task.icon = mPackageManager.getApplicationIcon(packageName);
                    task.label = mPackageManager.getApplicationLabel(
                        mPackageManager.getApplicationInfo(packageName, 0)).toString();
                } catch (PackageManager.NameNotFoundException e) {
                    task.label = packageName;
                    task.icon = null;
                }

                mTasks.add(task);
            }
        }

        updateEmptyState();
    }

    private void updateEmptyState() {
        if (mTasks.isEmpty()) {
            mTaskList.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.VISIBLE);
        } else {
            mTaskList.setVisibility(View.VISIBLE);
            mEmptyText.setVisibility(View.GONE);
        }
    }

    private void switchToTask(int position) {
        if (position >= 0 && position < mTasks.size()) {
            TaskInfo task = mTasks.get(position);

            // Try to move task to front
            mActivityManager.moveTaskToFront(task.taskId, ActivityManager.MOVE_TASK_WITH_HOME);

            finish();
            overridePendingTransition(0, 0);
        }
    }

    private void killTaskWithAnimation(final int position, View itemView) {
        if (position < 0 || position >= mTasks.size()) return;

        final TaskInfo task = mTasks.get(position);
        final String appName = task.label;

        // Create slide out + fade animation
        AnimationSet animSet = new AnimationSet(true);

        TranslateAnimation slideOut = new TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0,
            Animation.RELATIVE_TO_SELF, -1,
            Animation.RELATIVE_TO_SELF, 0,
            Animation.RELATIVE_TO_SELF, 0);
        slideOut.setDuration(200);

        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(200);

        animSet.addAnimation(slideOut);
        animSet.addAnimation(fadeOut);
        animSet.setFillAfter(true);

        animSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // Kill the app's background processes
                mActivityManager.killBackgroundProcesses(task.packageName);

                // Remove the task from recents
                mActivityManager.removeTask(task.taskId);

                // Remove from our list and update UI
                mTasks.remove(position);
                mAdapter.notifyDataSetChanged();
                updateEmptyState();

                Toast.makeText(TaskSwitcherActivity.this, appName + " closed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        itemView.startAnimation(animSet);
    }

    private class ListSwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;

            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            // Check for left swipe (negative X direction) to kill app
            if (Math.abs(diffX) > Math.abs(diffY) &&
                Math.abs(diffX) > SWIPE_THRESHOLD &&
                Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD &&
                diffX < 0) {

                // Get the position of the item that was swiped
                int position = mTaskList.pointToPosition((int) e1.getX(), (int) e1.getY());
                if (position != ListView.INVALID_POSITION) {
                    // Get the view at this position
                    int firstVisible = mTaskList.getFirstVisiblePosition();
                    View itemView = mTaskList.getChildAt(position - firstVisible);
                    if (itemView != null) {
                        killTaskWithAnimation(position, itemView);
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false; // Let the ListView handle taps
        }
    }

    private class TaskAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mTasks.size();
        }

        @Override
        public Object getItem(int position) {
            return mTasks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.task_item, parent, false);
            }

            TaskInfo task = mTasks.get(position);

            ImageView iconView = (ImageView) convertView.findViewById(R.id.app_icon);
            TextView nameView = (TextView) convertView.findViewById(R.id.app_name);

            if (task.icon != null) {
                iconView.setImageDrawable(task.icon);
            } else {
                iconView.setImageResource(android.R.drawable.sym_def_app_icon);
            }
            nameView.setText(task.label);

            return convertView;
        }
    }

    private static class TaskInfo {
        int taskId;
        String packageName;
        String label;
        Drawable icon;
        Intent baseIntent;
    }
}
