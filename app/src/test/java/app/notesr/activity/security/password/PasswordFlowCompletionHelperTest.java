/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security.password;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import app.notesr.activity.FsaEntry;
import app.notesr.activity.FsaResolver;
import app.notesr.activity.migration.MigrationActivity;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.service.AndroidServiceBootstrapper;
import app.notesr.service.migration.DataVersionManager;
import app.notesr.service.security.AppSecurityService;

class PasswordFlowCompletionHelperTest {

    private AppPasswordActivity activity;
    private AppSecurityService appSecurityService;
    private FsaResolver fsaResolver;
    private AndroidServiceBootstrapper androidServiceBootstrapper;
    private DataVersionManager dataVersionManager;
    private PasswordFlowCompletionHelper helper;
    private Context applicationContext;

    @BeforeEach
    void setUp() {
        activity = mock(AppPasswordActivity.class);
        appSecurityService = mock(AppSecurityService.class);
        fsaResolver = mock(FsaResolver.class);
        androidServiceBootstrapper = mock(AndroidServiceBootstrapper.class);
        dataVersionManager = mock(DataVersionManager.class);
        applicationContext = mock(Context.class);

        when(activity.getApplicationContext()).thenReturn(applicationContext);
        when(dataVersionManager.getCurrentVersion()).thenReturn(2);

        helper = new PasswordFlowCompletionHelper(activity, appSecurityService, fsaResolver,
                androidServiceBootstrapper, dataVersionManager);
    }

    @Test
    void testBootstrapPostAuthServicesUsesCurrentSecretsAndProvidedContext() {
        Context context = mock(Context.class);
        CryptoSecrets secrets = mock(CryptoSecrets.class);
        when(appSecurityService.getActualSecrets()).thenReturn(secrets);

        helper.bootstrapPostAuthServices(context);

        verify(androidServiceBootstrapper).startServicesPostAuth(context, secrets);
    }

    @Test
    void testNavigateNextActivityStartsIntentAndFinishesCurrentActivity() {
        Intent nextActivity = mock(Intent.class);

        helper.navigateNextActivity(nextActivity);

        verify(activity).startActivity(nextActivity);
        verify(activity).finish();
    }

    @Test
    void testGetNewActivityIntentUsesApplicationContextAndRequestedActivity() {
        List<List<?>> constructorArguments = new ArrayList<>();
        try (MockedConstruction<Intent> intents = mockConstruction(Intent.class,
                (intent, context) -> constructorArguments.add(context.arguments()))) {
            helper.getNewActivityIntent(MigrationActivity.class);

            verify(activity).getApplicationContext();

            assertEquals(1, constructorArguments.size(),
                    "Exactly one Intent should be constructed");
            assertEquals(applicationContext, constructorArguments.get(0).get(0),
                    "The Intent should use the activity application context");
            assertEquals(MigrationActivity.class, constructorArguments.get(0).get(1),
                    "The Intent should target the requested activity");
        }
    }

    @Test
    void testIsMigrationRequiredOnlyWhenStoredVersionIsOlder() {
        when(dataVersionManager.getCurrentVersion()).thenReturn(1);
        assertTrue(helper.isMigrationRequired(),
                "Migration should be required for an older data schema");

        when(dataVersionManager.getCurrentVersion()).thenReturn(2);
        assertFalse(helper.isMigrationRequired(),
                "Migration should not be required for the current data schema");

        when(dataVersionManager.getCurrentVersion()).thenReturn(3);
        assertFalse(helper.isMigrationRequired(),
                "Migration should not be required for a newer data schema");
    }

    @Test
    void testGetFsaIntentReturnsNullWhenNoForegroundServiceActivityExists() {
        when(fsaResolver.getFsaEntryOfCurrentRunningFs()).thenReturn(null);

        assertNull(helper.getFsaIntent(),
                "A null FSA entry should produce no intermediate Intent");
    }

    @Test
    void testGetFsaIntentTargetsCurrentForegroundServiceActivity() {
        when(fsaResolver.getFsaEntryOfCurrentRunningFs())
                .thenReturn(new FsaEntry(null, MigrationActivity.class));

        List<List<?>> constructorArguments = new ArrayList<>();

        try (MockedConstruction<Intent> intents = mockConstruction(Intent.class,
                (intent, context) -> constructorArguments.add(context.arguments()))) {
            Intent intent = helper.getFsaIntent();

            assertEquals(intents.constructed().get(0), intent,
                    "The FSA intermediate Intent should be returned");
            assertEquals(MigrationActivity.class, constructorArguments.get(0).get(1),
                    "The FSA intermediate Intent should target the associated activity");
        }
    }

    @Test
    void testMigrationTakesPrecedenceOverForegroundServiceActivity() {
        when(dataVersionManager.getCurrentVersion()).thenReturn(1);
        when(fsaResolver.getFsaEntryOfCurrentRunningFs())
                .thenReturn(new FsaEntry(null, MigrationActivity.class));

        List<List<?>> constructorArguments = new ArrayList<>();

        try (MockedConstruction<Intent> intents = mockConstruction(Intent.class,
                (intent, context) -> constructorArguments.add(context.arguments()))) {
            Intent intent = helper.getIntermediateActivityIntent();

            assertEquals(intents.constructed().get(0), intent,
                    "The migration intermediate Intent should be returned");
            assertEquals(MigrationActivity.class, constructorArguments.get(0).get(1),
                    "Migration should produce a MigrationActivity Intent");

            verify(fsaResolver, never()).getFsaEntryOfCurrentRunningFs();
        }
    }

    @Test
    void testNavigateNextOrIntermediateActivityFallsBackToTargetWhenNothingIsRequired() {
        Intent target = mock(Intent.class);
        when(fsaResolver.getFsaEntryOfCurrentRunningFs()).thenReturn(null);

        helper.navigateNextOrIntermediateActivity(target);

        verify(activity).startActivity(target);
        verify(activity).finish();
    }

    @Test
    void testNavigateNextOrIntermediateActivityUsesRequiredIntermediateActivity() {
        when(dataVersionManager.getCurrentVersion()).thenReturn(1);
        Intent target = mock(Intent.class);

        List<List<?>> constructorArguments = new ArrayList<>();
        try (MockedConstruction<Intent> intents = mockConstruction(Intent.class,
                (intent, context) -> constructorArguments.add(context.arguments()))) {
            helper.navigateNextOrIntermediateActivity(target);

            assertEquals(MigrationActivity.class, constructorArguments.get(0).get(1),
                    "Navigation should use the migration intermediate activity");

            verify(activity).startActivity(intents.constructed().get(0));
            verify(activity).finish();
        }
    }
}
