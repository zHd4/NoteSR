/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import javax.inject.Inject;

import app.notesr.R;
import app.notesr.activity.security.password.AppPasswordActivity;
import app.notesr.util.ActivityUtils;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Displays the initial setup screen for an application that has not been configured yet.
 */
@AndroidEntryPoint
public class StartActivity extends ActivityBase {

    private static final String TAG = StartActivity.class.getSimpleName();
    private static final double BANNER_MARGIN_FOR_LOW_HEIGHT = 0.1;
    private static final double BANNER_MARGIN_FOR_LARGE_HEIGHT = 0.2;
    private static final int LOW_SCREEN_HEIGHT = 800;

    @Inject
    ActivityUtils activityUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            ensureAccessible();
        } catch (IllegalStateException e) {
            finishWithError(TAG, R.string.start_activity_invalid_state,
                    "Invalid state in StartActivity", e);
            return;
        }

        setContentView(R.layout.activity_start);
        applyInsets(findViewById(R.id.main));

        Intent appPasswordActivity = new Intent(getApplicationContext(), AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE,
                        AppPasswordActivity.Mode.CREATE_PASSWORD.getModeName());

        View.OnClickListener onGetStartedButtonClick =
                view -> startActivity(appPasswordActivity);
        findViewById(R.id.getStartedButton).setOnClickListener(onGetStartedButtonClick);

        activityUtils.disableBackButton();
        placeBannerFront();
    }

    /**
     * Checks that the application is in a state that allows initial setup.
     *
     * @throws IllegalStateException if application configuration already exists or the
     *                              application is blocked
     */
    protected void ensureAccessible() {
        if (appSecurityService.isKeyExists()) {
            throw new IllegalStateException(
                    "StartActivity is inaccessible if application is already configured" +
                            " and key already exists");
        }

        if (appSecurityService.isAppBlocked()) {
            throw new IllegalStateException(
                    "StartActivity is inaccessible if application is blocked");
        }
    }

    /**
     * Positions the front banner according to the device's screen height.
     */
    protected void placeBannerFront() {
        ConstraintLayout layout = findViewById(R.id.bannerFrontLayout);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        int screenHeight = displayMetrics.heightPixels;

        double percent = screenHeight > LOW_SCREEN_HEIGHT
                ? BANNER_MARGIN_FOR_LARGE_HEIGHT
                : BANNER_MARGIN_FOR_LOW_HEIGHT;

        int topMargin = (int) (percent * screenHeight);

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone((ConstraintLayout) layout.getParent());

        constraintSet.connect(
                R.id.bannerFrontLayout,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
                topMargin
        );

        constraintSet.applyTo((ConstraintLayout) layout.getParent());
    }
}
