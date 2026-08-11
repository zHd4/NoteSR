/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import app.notesr.activity.ActivityBase;
import app.notesr.service.security.AppSecurityService;

@RunWith(AndroidJUnit4.class)
public class LockActionTest {

    private Context context;
    private ActivityBase activity;
    private AppSecurityService appSecurityService;

    @Before
    public void setUp() {
        var instrumentationRegistry = InstrumentationRegistry.getInstrumentation();
        instrumentationRegistry.getUiAutomation().adoptShellPermissionIdentity();

        context = instrumentationRegistry.getTargetContext();
        activity = mock(ActivityBase.class);
        appSecurityService = mock(AppSecurityService.class);
    }

    @Test
    public void testLockLogsOutAndStartsAuthenticationActivity() {
        when(activity.getApplicationContext()).thenReturn(context);

        LockAction lockAction = new LockAction(activity, appSecurityService);
        lockAction.lock();

        InOrder inOrder = inOrder(appSecurityService, activity);
        inOrder.verify(appSecurityService).logout();
        inOrder.verify(activity).startActivity(any(Intent.class));
        inOrder.verify(activity).finish();
    }

    @Test
    public void testLockStartsAuthActivityWithAuthenticationMode() {
        when(activity.getApplicationContext()).thenReturn(context);

        LockAction lockAction = new LockAction(activity, appSecurityService);
        lockAction.lock();

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivity(intentCaptor.capture());
        Intent startedIntent = intentCaptor.getValue();

        assertNotNull("Intent should be created", startedIntent);
        assertNotNull("Intent component should be set", startedIntent.getComponent());
        assertEquals("Intent component should target AuthActivity",
                AuthActivity.class.getName(), startedIntent.getComponent().getClassName());
        assertEquals("Intent should use authentication mode",
                AuthActivity.Mode.AUTHENTICATION.toString(),
                startedIntent.getStringExtra(AuthActivity.EXTRA_MODE));

        verify(appSecurityService).logout();
        verify(activity).finish();
    }
}
