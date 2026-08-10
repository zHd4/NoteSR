/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import app.notesr.service.security.AppSecurityService;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class ActivityBaseTest {

    private Context context;
    private AppSecurityService mockAppSecurityService;

    @Before
    public void setUp() {
        var instrumentationRegistry = InstrumentationRegistry.getInstrumentation();
        instrumentationRegistry.getUiAutomation().adoptShellPermissionIdentity();

        context = instrumentationRegistry.getTargetContext();
        mockAppSecurityService = mock(AppSecurityService.class);
    }

    private TestActivity createActivity() {
        AtomicReference<TestActivity> testActivityReference = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> testActivityReference.set(new TestActivity()));

        return testActivityReference.get();
    }

    @Test
    public void testRequiresSessionDefaultReturnTrue() {
        TestActivity activity = createActivity();
        assertTrue("ActivityBase.requiresSession() should return true by default",
                activity.requiresSession());
    }

    @Test
    public void testRequiresSessionCanBeOverridden() {
        TestActivity activity = createActivity();
        activity.setSessionRequired(false);
        assertFalse("requiresSession() should return false when overridden",
                activity.requiresSession());
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
        activity.setAppSecurityService(mockAppSecurityService);
        when(mockAppSecurityService.isAuthConfigured()).thenReturn(true);

        boolean isSessionActive = activity.isSessionActive();

        assertTrue("isSessionActive should return true"
                + " when security service reports configured", isSessionActive);

        verify(mockAppSecurityService).isAuthConfigured();
    }

    @Test
    public void testRestartAppCalledOnValidateSession() {
        TestActivity activity = spy(createActivity());
        activity.setAppSecurityService(mockAppSecurityService);
        doNothing().when(activity).restartApp();
        when(activity.requiresSession()).thenReturn(true);
        when(mockAppSecurityService.isAuthConfigured()).thenReturn(false);

        activity.validateSession();

        verify(activity).restartApp();
    }

    @Test
    public void testOnValidateSessionSkipsRestartWhenNotRequired() {
        TestActivity activity = spy(createActivity());
        activity.setAppSecurityService(mockAppSecurityService);

        when(activity.requiresSession()).thenReturn(false);
        when(mockAppSecurityService.isAuthConfigured()).thenReturn(false);
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
}
