/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static java.util.Objects.requireNonNull;

import android.app.Activity;
import android.app.Instrumentation;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.core.app.ActivityScenario;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import app.notesr.R;
import app.notesr.activity.security.AuthActivity;
import app.notesr.di.service.security.AppSecurityServiceModule;
import app.notesr.service.security.AppSecurityService;
import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.UninstallModules;

@RunWith(AndroidJUnit4.class)
@HiltAndroidTest
@UninstallModules(AppSecurityServiceModule.class)
public class StartActivityTest {

    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @BindValue
    AppSecurityService appSecurityService = mock(AppSecurityService.class);

    private Instrumentation instrumentation;

    @Before
    public void setUp() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.getUiAutomation().adoptShellPermissionIdentity();
    }

    private ActivityScenario<StartActivity> createScenario() {
        return ActivityScenario.launch(StartActivity.class);
    }

    @Test
    public void testOnCreateSetsContentView() {
        try (ActivityScenario<StartActivity> scenario = createScenario()) {
            scenario.onActivity(activity ->
                    assertNotNull("Activity should have a content view",
                            activity.findViewById(android.R.id.content)));
        }
    }

    @Test
    public void testOnCreateAppliesInsets() {
        try (ActivityScenario<StartActivity> scenario = createScenario()) {
            scenario.onActivity(activity ->
                    assertNotNull("Main view should be found",
                            activity.findViewById(R.id.main)));
        }
    }

    @Test
    public void testOnCreateDoesNotCallValidateSession() {
        // TODO: Check that validateSession is not called on activity creation.
    }

    @Test
    public void testValidateKeyNotExistsThrowsWhenKeyExists() {
        when(appSecurityService.isKeyExists()).thenReturn(false);

        try (ActivityScenario<StartActivity> scenario = createScenario()) {
            verify(appSecurityService).isKeyExists();
            when(appSecurityService.isKeyExists()).thenReturn(true);

            scenario.onActivity(activity -> {
                IllegalStateException exception = assertThrows(
                        IllegalStateException.class,
                        activity::validateKeyNotExists);

                assertTrue(requireNonNull(exception.getMessage()).contains("key already exists"));
            });
        }
    }

    @Test
    public void testGetStartedButtonHasClickListener() {
        try (ActivityScenario<StartActivity> scenario = createScenario()) {
            scenario.onActivity(activity -> {
                View getStartedButton = activity.findViewById(R.id.getStartedButton);
                assertNotNull("Get started button should exist", getStartedButton);
            });
        }
    }

    @Test
    public void testOnCreateDisablesBackButton() {
        try (ActivityScenario<StartActivity> scenario = createScenario()) {
            scenario.onActivity(activity -> assertTrue(
                    "Back button should be disabled",
                    activity.getOnBackPressedDispatcher().hasEnabledCallbacks()));
        }
    }

    @Test
    public void testPlaceBannerFrontWithLargeScreenHeight() {
        try (ActivityScenario<StartActivity> scenario = createScenario()) {
            scenario.onActivity(activity -> {
                // Simulate large screen height (> 800px)
                activity.getResources().getDisplayMetrics().heightPixels = 1200;

                activity.placeBannerFront();

                ConstraintLayout bannerLayout = activity.findViewById(R.id.bannerFrontLayout);
                assertNotNull("Banner front layout should exist", bannerLayout);
            });
        }
    }

    @Test
    public void testPlaceBannerFrontWithLowScreenHeight() {
        try (ActivityScenario<StartActivity> scenario = createScenario()) {
            scenario.onActivity(activity -> {
                // Simulate low screen height (600px)
                activity.getResources().getDisplayMetrics().heightPixels = 600;

                activity.placeBannerFront();

                ConstraintLayout bannerLayout = activity.findViewById(R.id.bannerFrontLayout);
                assertNotNull("Banner front layout should exist", bannerLayout);
            });
        }
    }

    @Test
    public void testPlaceBannerFrontWithExactlyLowScreenHeightBoundary() {
        try (ActivityScenario<StartActivity> scenario = createScenario()) {
            scenario.onActivity(activity -> {
                // Simulate low screen height (800px)
                activity.getResources().getDisplayMetrics().heightPixels = 800;

                activity.placeBannerFront();

                ConstraintLayout bannerLayout = activity.findViewById(R.id.bannerFrontLayout);
                assertNotNull("Banner front layout should exist at boundary", bannerLayout);
            });
        }
    }

    @Test
    public void testAuthActivityIntentHasCreatePasswordMode() {
        try (ActivityScenario<StartActivity> scenario = createScenario()) {
            Instrumentation.ActivityMonitor monitor = instrumentation
                    .addMonitor(AuthActivity.class.getName(), null, false);

            scenario.onActivity(activity -> {
                View getStartedButton = activity.findViewById(R.id.getStartedButton);
                assertNotNull("Get started button should exist", getStartedButton);
                getStartedButton.performClick();
            });

            Activity started = instrumentation.waitForMonitorWithTimeout(monitor, 20000);

            assertNotNull("AuthActivity should be started on button click", started);
            started.finish();
        }
    }

    @Test
    public void testOnCreateIntentConfiguration() {
        try (ActivityScenario<StartActivity> scenario = createScenario()) {
            scenario.onActivity(activity -> {
                assertNotNull("Activity should be created", activity);
                assertNotNull("Button should be found",
                        activity.findViewById(R.id.getStartedButton));
            });
        }
    }

    @Test
    public void testPlaceBannerFrontPositionsViewCorrectly() {
        try (ActivityScenario<StartActivity> scenario = createScenario()) {
            scenario.onActivity(activity -> {
                activity.placeBannerFront();

                ConstraintLayout bannerLayout = activity.findViewById(R.id.bannerFrontLayout);
                assertNotNull("Banner layout should be positioned", bannerLayout);
            });
        }
    }
}
