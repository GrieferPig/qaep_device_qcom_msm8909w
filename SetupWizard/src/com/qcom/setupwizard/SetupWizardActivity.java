/*
 * Copyright (c) 2024, The Linux Foundation. All rights reserved.
 *
 * Dummy Setup Wizard for msm8909w
 * Displays a welcome screen with a button to complete device setup.
 */

package com.qcom.setupwizard;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;

/**
 * A simple Setup Wizard activity that displays a welcome message and a button
 * to mark device setup as complete.
 */
public class SetupWizardActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_wizard);

        Button completeButton = (Button) findViewById(R.id.complete_setup_button);
        completeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                completeSetup();
            }
        });
    }

    /**
     * Marks the device as provisioned and setup complete, then disables this
     * activity and finishes.
     */
    private void completeSetup() {
        // Mark device as provisioned
        Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 1);

        // Mark user setup as complete
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 1);

        // Disable this activity so it doesn't show up again
        PackageManager pm = getPackageManager();
        ComponentName name = new ComponentName(this, SetupWizardActivity.class);
        pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        // Finish the activity
        finish();
    }
}
