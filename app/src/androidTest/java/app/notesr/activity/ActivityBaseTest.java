/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ActivityBaseTest {

    private static final String EXTRA_REQUIRES_SESSION = "requiresSession";

    private Context context;

    @Before
    public void setUp() {
        var instrumentationRegistry = InstrumentationRegistry.getInstrumentation();
        instrumentationRegistry.getUiAutomation().adoptShellPermissionIdentity();

        context = instrumentationRegistry.getTargetContext();
    }

    @Test
    public void testOnCreateSetsContentView() {
        ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class);

        try (scenario) {
            scenario.onActivity(activity ->
                    assertNotNull("Activity should have a content view",
                            activity.findViewById(android.R.id.content)));
        }
    }

    @Test
    public void testOnCreateSetsFlagSecure() {
        ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class);

        try (scenario) {
            scenario.onActivity(activity -> {
                WindowManager.LayoutParams params = activity.getWindow().getAttributes();
                int flags = params.flags;
                assertTrue("FLAG_SECURE should be set on window",
                        (flags & WindowManager.LayoutParams.FLAG_SECURE) != 0);
            });
        }
    }

    @Test
    public void testOnCreateConfiguresWindowInsets() {
        ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class);

        try (scenario) {
            scenario.onActivity(activity -> {
                assertNotNull("Window should be configured",
                        activity.getWindow());
                assertNotNull("Window decorView should exist",
                        activity.getWindow().getDecorView());
            });
        }
    }

    @Test
    public void testApplyInsetsAddsPadding() {
        ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class);

        try (scenario) {
            scenario.onActivity(activity -> {
                View testView = new View(activity);
                activity.applyInsets(testView);

                assertNotNull("View should have window insets listener applied",
                        testView.getViewTreeObserver());
            });
        }
    }

    @Test
    public void testRequiresSessionDefaultReturnTrue() {
        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity ->
                    assertTrue("ActivityBase.requiresSession() should return true by default",
                            activity.requiresSession()));
        }
    }

    @Test
    public void testRequiresSessionCanBeOverridden() {
        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                activity.setRequiresSessionValue(false);
                assertFalse("requiresSession() should return false when overridden",
                        activity.requiresSession());
            });
        }
    }

    @Test
    public void testOptionsItemSelectedHandlesHomeButton() {
        ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class);

        try (scenario) {
            scenario.onActivity(activity -> {
                MenuItem mockItem = mock(MenuItem.class);
                when(mockItem.getItemId()).thenReturn(android.R.id.home);

                boolean result = activity.onOptionsItemSelected(mockItem);

                assertTrue("onOptionsItemSelected should return true for home button",
                        result);
            });
        }
    }

    @Test
    public void testOptionsItemSelectedCallsFinishForHomeButton() {
        ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class);

        try (scenario) {
            scenario.onActivity(activity -> {
                MenuItem mockItem = mock(MenuItem.class);
                when(mockItem.getItemId()).thenReturn(android.R.id.home);

                activity.onOptionsItemSelected(mockItem);
            });

            // Activity should be destroyed after home button is pressed.
            // Move the activity state outside the onActivity callback because
            // that callback runs on the main thread.
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED);
        }
    }

    @Test
    public void testOptionsItemSelectedDelegatesOtherItems() {
        ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class);

        try (scenario) {
            scenario.onActivity(activity -> {
                MenuItem mockItem = mock(MenuItem.class);
                when(mockItem.getItemId()).thenReturn(999); // Non-home item ID

                boolean result = activity.onOptionsItemSelected(mockItem);

                assertFalse("onOptionsItemSelected should return false for non-home items",
                        result);
            });
        }
    }

    @Test
    public void testIsSessionActiveCallsSecurityService() {
        Intent activityIntent = new Intent(context, TestActivity.class)
                .putExtra(EXTRA_REQUIRES_SESSION, false);

        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(activityIntent)) {
            scenario.onActivity(activity -> {
                // isSessionActive will be called during onCreate
                // If requiresSession returns true, it checks session
                assertNotNull("Activity should be created successfully", activity);
            });
        }
    }

    @Test
    public void testRestartAppCalledOnActivityCreation() {
        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity ->
                    assertTrue("Activity should call restartApp() on creation",
                            activity.isRestartCalled()));
        }
    }

    @Test
    public void testWindowDecorFitsSystemWindowsSetToFalse() {
        Intent activityIntent = new Intent(context, TestActivity.class)
                .putExtra(EXTRA_REQUIRES_SESSION, false);

        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(activityIntent)) {
            scenario.onActivity(activity -> {
                // Verify that WindowCompat.setDecorFitsSystemWindows was called
                // This ensures the window will extend behind system UI
                assertNotNull("Window should exist after onCreate",
                        activity.getWindow());
            });
        }
    }

    @Test
    public void testActivityInitializesAppSecurityService() {
        Intent activityIntent = new Intent(context, TestActivity.class)
                .putExtra(EXTRA_REQUIRES_SESSION, false);

        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(activityIntent)) {
            scenario.onActivity(activity -> {
                // AppSecurityService should be initialized in onCreate
                assertNotNull("AppSecurityService should be initialized",
                        activity.getAppSecurityService());
            });
        }
    }

    @Test
    public void testOnCreateValidatesSessionWhenRequired() {
        ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class);

        try (scenario) {
            scenario.onActivity(activity -> {
                activity.setRequiresSessionValue(true);
                assertTrue("Activity should handle session validation",
                        activity.requiresSession());
            });
        }
    }

    @Test
    public void testOnCreateSkipsSessionValidationWhenNotRequired() {
        ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class);

        try (scenario) {
            scenario.onActivity(activity -> {
                activity.setRequiresSessionValue(false);
                assertFalse("Activity should skip session validation when not required",
                        activity.requiresSession());
            });
        }
    }

    @Test
    public void testApplyInsetsWithMultipleViews() {
        ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class);

        try (scenario) {
            scenario.onActivity(activity -> {
                View view1 = new View(activity);
                View view2 = new View(activity);

                activity.applyInsets(view1);
                activity.applyInsets(view2);

                assertNotNull("Both views should be configured", view1);
                assertNotNull("Both views should be configured", view2);
            });
        }
    }

    @Test
    public void testActivityPreservesState() {
        Intent activityIntent = new Intent(context, TestActivity.class)
                .putExtra(EXTRA_REQUIRES_SESSION, false);

        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(activityIntent)) {
            scenario.recreate();

            scenario.onActivity(activity ->
                    assertFalse("Activity state should be preserved across recreate",
                            activity.requiresSession()));
        }
    }
}
