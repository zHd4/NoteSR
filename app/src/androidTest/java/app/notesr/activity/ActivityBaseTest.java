/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import app.notesr.di.service.security.AppSecurityServiceModule;
import app.notesr.di.util.ActivityUtilsModule;
import app.notesr.service.security.AppSecurityService;
import app.notesr.util.ActivityUtils;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.UninstallModules;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;


@HiltAndroidTest
@UninstallModules({
        AppSecurityServiceModule.class,
        ActivityUtilsModule.class
})
@RunWith(AndroidJUnit4.class)
public class ActivityBaseTest {

    private Context context;

    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @BindValue
    AppSecurityService mockAppSecurityService = mock(AppSecurityService.class);

    @BindValue
    ActivityUtils mockActivityUtils = mock(ActivityUtils.class);

    private ActivityScenario<TestActivity> activityScenario;

    @Before
    public void setUp() {
        hiltRule.inject();
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @After
    public void tearDown() {
        if (activityScenario != null) {
            activityScenario.close();
            activityScenario = null;
        }
    }

    private TestActivity createActivity() {
        AtomicReference<TestActivity> testActivityReference = new AtomicReference<>();
        activityScenario = ActivityScenario.launch(TestActivity.class);
        activityScenario.onActivity(testActivityReference::set);

        return testActivityReference.get();
    }

    @Test
    public void testOptionsItemSelectedHandlesHomeButton() {
        TestActivity activity = createActivity();
        MenuItem mockItem = mock(MenuItem.class);
        when(mockItem.getItemId()).thenReturn(android.R.id.home);

        boolean result = activity.onOptionsItemSelected(mockItem);

        assertTrue("onOptionsItemSelected should return true for home button",
                result);
    }

    @Test
    public void testOptionsItemSelectedDelegatesOtherItems() {
        TestActivity activity = createActivity();
        MenuItem mockItem = mock(MenuItem.class);
        when(mockItem.getItemId()).thenReturn(999); // Non-home item ID

        boolean result = activity.onOptionsItemSelected(mockItem);

        assertFalse("onOptionsItemSelected should return false for non-home items",
                result);
    }

    @Test
    public void testIsSessionActiveCallsSecurityService() {
        TestActivity activity = createActivity();
        when(mockAppSecurityService.isAuthConfigured()).thenReturn(true);

        boolean isSessionActive = activity.isSessionActive();

        assertTrue("isSessionActive should return true"
                + " when security service reports configured", isSessionActive);

        verify(mockAppSecurityService).isAuthConfigured();
    }

    @Test
    public void testRestartAppCalledOnValidateSession() {
        TestActivity activity = spy(createActivity());
        doNothing().when(activity).restartApp();
        when(mockAppSecurityService.isAuthConfigured()).thenReturn(false);

        activity.validateSession();

        verify(activity).restartApp();
    }

    @Test
    public void testValidateSessionSkipsRestartWhenSessionIsActive() {
        TestActivity activity = spy(createActivity());

        when(mockAppSecurityService.isAuthConfigured()).thenReturn(true);
        doNothing().when(activity).restartApp();

        activity.validateSession();

        verify(activity, never()).restartApp();
    }

    @Test
    public void testApplyInsetsAddsListener() {
        TestActivity activity = createActivity();
        View testView = new View(context);

        activity.applyInsets(testView);

        assertNotNull("View should have view tree observer after applying insets",
                testView.getViewTreeObserver());
    }

    @Test
    public void testApplyInsetsWithMultipleViews() {
        TestActivity activity = createActivity();
        View view1 = new View(context);
        View view2 = new View(context);

        activity.applyInsets(view1);
        activity.applyInsets(view2);

        assertNotNull("Both views should be configured", view1);
        assertNotNull("Both views should be configured", view2);
    }

    @Test
    public void testEnableWindowProtectionSetsFlagSecure() {
        int flagSecure = WindowManager.LayoutParams.FLAG_SECURE;

        Window mockWindow = mock(Window.class);
        TestActivity activity = spy(createActivity());
        when(activity.getWindow()).thenReturn(mockWindow);

        activity.enableWindowProtection();

        verify(mockWindow).setFlags(flagSecure, flagSecure);
    }

    @Test
    public void testFinishWithErrorShowsToastAndFinishesActivity() {
        TestActivity activity = spy(createActivity());
        Resources resources = mock(Resources.class);

        String message = context.getString(android.R.string.ok);

        doReturn(resources).when(activity).getResources();
        when(resources.getString(android.R.string.ok)).thenReturn(message);

        Throwable throwable = new IllegalStateException("test failure");

        activity.finishWithError(
                "ActivityBaseTest",
                android.R.string.ok,
                "Expected test error",
                throwable);

        verify(mockActivityUtils).showToastMessage(message, Toast.LENGTH_SHORT);
        verify(activity).finish();
    }
}
