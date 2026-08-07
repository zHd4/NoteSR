/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import app.notesr.activity.note.list.NotesListActivity;
import app.notesr.activity.security.AuthActivity;
import app.notesr.activity.security.KeyRecoveryActivity;
import app.notesr.service.AndroidServiceRegistry;
import app.notesr.service.lifecycle.AppCloseAndroidService;
import app.notesr.service.security.AppSecurityService;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    private Context context;
    private AndroidServiceRegistry mockServiceRegistry;
    private AppSecurityService mockAppSecurityService;
    private FsaResolver mockFsaResolver;

    @Before
    public void setUp() {
        var instrumentationRegistry = InstrumentationRegistry.getInstrumentation();
        instrumentationRegistry.getUiAutomation().adoptShellPermissionIdentity();

        context = instrumentationRegistry.getTargetContext();
        mockServiceRegistry = mock(AndroidServiceRegistry.class);
        mockAppSecurityService = mock(AppSecurityService.class);
        mockFsaResolver = mock(FsaResolver.class);
    }

    private MainActivity createActivity() {
        AtomicReference<MainActivity> activityRef = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> activityRef.set(new MainActivity()));

        return activityRef.get();
    }

    @Test
    public void testGetIntentSuppliersWhenAppBlockedReturnsKeyRecoveryActivityIntent() {
        when(mockAppSecurityService.isAppBlocked()).thenReturn(true);
        when(mockAppSecurityService.isKeyExists()).thenReturn(true);
        when(mockAppSecurityService.isAuthConfigured()).thenReturn(true);
        when(mockFsaResolver.getFsaEntryOfCurrentRunningFs()).thenReturn(null);

        MainActivity activity = createActivity();
        List<Supplier<Intent>> suppliers = activity.getIntentSuppliers(
                context,
                mockAppSecurityService,
                mockFsaResolver
        );

        Intent resolvedIntent = suppliers.get(0).get();

        assertNotNull("First supplier should return KeyRecoveryActivity intent",
                resolvedIntent);
        assertNotNull("Intent component should not be null",
                resolvedIntent.getComponent());
        assertEquals("Intent component should be set to KeyRecoveryActivity",
                KeyRecoveryActivity.class.getName(), resolvedIntent.getComponent().getClassName());
    }

    @Test
    public void testGetIntentSuppliersWhenKeyDoesNotExistReturnsStartActivityIntent() {
        when(mockAppSecurityService.isAppBlocked()).thenReturn(false);
        when(mockAppSecurityService.isKeyExists()).thenReturn(false);
        when(mockAppSecurityService.isAuthConfigured()).thenReturn(true);
        when(mockFsaResolver.getFsaEntryOfCurrentRunningFs()).thenReturn(null);

        MainActivity activity = createActivity();
        List<Supplier<Intent>> suppliers = activity.getIntentSuppliers(
                context,
                mockAppSecurityService,
                mockFsaResolver
        );

        assertNull("First supplier should return null when app not blocked",
                suppliers.get(0).get());

        Intent resolvedIntent = suppliers.get(1).get();

        assertNotNull("Second supplier should return StartActivity intent",
                resolvedIntent);
        assertNotNull("Intent component should not be null",
                resolvedIntent.getComponent());
        assertEquals("Intent component should be set to StartActivity",
                StartActivity.class.getName(), resolvedIntent.getComponent().getClassName());
    }

    @Test
    public void testGetIntentSuppliersWhenAuthNotConfiguredReturnsAuthActivityIntent() {
        when(mockAppSecurityService.isAppBlocked()).thenReturn(false);
        when(mockAppSecurityService.isKeyExists()).thenReturn(true);
        when(mockAppSecurityService.isAuthConfigured()).thenReturn(false);
        when(mockFsaResolver.getFsaEntryOfCurrentRunningFs()).thenReturn(null);

        MainActivity activity = createActivity();
        List<Supplier<Intent>> suppliers = activity.getIntentSuppliers(
                context,
                mockAppSecurityService,
                mockFsaResolver
        );


        assertNull("First supplier should return null", suppliers.get(0).get());
        assertNull("Second supplier should return null", suppliers.get(1).get());

        Intent resolvedIntent = suppliers.get(2).get();

        assertNotNull("Third supplier should return AuthActivity intent",
                resolvedIntent);
        assertNotNull("Intent component should not be null",
                resolvedIntent.getComponent());
        assertEquals("Intent component should be set to AuthActivity",
                AuthActivity.class.getName(), resolvedIntent.getComponent().getClassName());
        assertNotNull("Auth mode extra should be set",
                resolvedIntent.getStringExtra(AuthActivity.EXTRA_MODE));
    }

    @Test
    public void testGetIntentSuppliersWhenFsaRunningReturnsFsaActivityIntent() {
        when(mockAppSecurityService.isAppBlocked()).thenReturn(false);
        when(mockAppSecurityService.isKeyExists()).thenReturn(true);
        when(mockAppSecurityService.isAuthConfigured()).thenReturn(true);

        FsaEntry fsaEntry = new FsaEntry();
        fsaEntry.setActivityClass(NotesListActivity.class);
        when(mockFsaResolver.getFsaEntryOfCurrentRunningFs()).thenReturn(fsaEntry);

        MainActivity activity = createActivity();
        List<Supplier<Intent>> suppliers = activity.getIntentSuppliers(
                context,
                mockAppSecurityService,
                mockFsaResolver
        );

        assertNull("First supplier should return null", suppliers.get(0).get());
        assertNull("Second supplier should return null", suppliers.get(1).get());
        assertNull("Third supplier should return null", suppliers.get(2).get());

        Intent resolvedIntent = suppliers.get(3).get();
        assertNotNull("Fourth supplier should return FSA activity intent",
                resolvedIntent);
        assertNotNull("Intent component should not be null",
                resolvedIntent.getComponent());
        assertEquals("Intent component should be set to FSA activity",
                NotesListActivity.class.getName(), resolvedIntent.getComponent().getClassName());
    }

    @Test
    public void testGetIntentSuppliersWhenNoSpecialConditionsAllSuppliersReturnNull() {
        when(mockAppSecurityService.isAppBlocked()).thenReturn(false);
        when(mockAppSecurityService.isKeyExists()).thenReturn(true);
        when(mockAppSecurityService.isAuthConfigured()).thenReturn(true);
        when(mockFsaResolver.getFsaEntryOfCurrentRunningFs()).thenReturn(null);

        MainActivity activity = createActivity();
        List<Supplier<Intent>> suppliers = activity.getIntentSuppliers(
                context,
                mockAppSecurityService,
                mockFsaResolver
        );

        // All suppliers should return null when no special conditions apply
        for (Supplier<Intent> supplier : suppliers) {
            assertNull("Supplier should return null when no special condition applies",
                    supplier.get());
        }
    }

    @Test
    public void testStartAppCloseServiceWhenServiceNotRunningStartsService() {
        when(mockServiceRegistry.isServiceRunning(AppCloseAndroidService.class))
                .thenReturn(false);

        MainActivity activity = createActivity();
        activity.startAppCloseService(context, mockServiceRegistry);

        verify(mockServiceRegistry).isServiceRunning(AppCloseAndroidService.class);
    }

    @Test
    public void testStartAppCloseServiceWhenServiceAlreadyRunningDoesNotStartAgain() {
        when(mockServiceRegistry.isServiceRunning(AppCloseAndroidService.class))
                .thenReturn(true);

        MainActivity activity = createActivity();
        activity.startAppCloseService(context, mockServiceRegistry);

        verify(mockServiceRegistry).isServiceRunning(AppCloseAndroidService.class);
    }

    @Test
    public void testRequiresSessionReturnsFalse() {
        MainActivity activity = createActivity();
        assertFalse("Activity should not require session", activity.requiresSession());
    }
}
