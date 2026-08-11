/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import app.notesr.BuildConfig;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.migration.MigrationActivity;
import app.notesr.activity.note.list.NotesListActivity;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.service.migration.DataVersionManager;
import app.notesr.service.security.AppSecurityService;
import app.notesr.service.security.rotation.SecretsUpdateAndroidService;
import io.bloco.faker.Faker;

@RunWith(AndroidJUnit4.class)
public class KeySetupCompletionHandlerTest {

    private static final int KEY_LENGTH = 48;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Faker FAKER = new Faker();

    private Context context;
    private ActivityBase activity;
    private AppSecurityService appSecurityService;

    @Before
    public void setUp() {
        var instrumentationRegistry = InstrumentationRegistry.getInstrumentation();

        context = instrumentationRegistry.getTargetContext();
        activity = mock(ActivityBase.class);
        appSecurityService = mock(AppSecurityService.class);

        clearCache();
    }

    @After
    public void tearDown() {
        clearCache();
    }

    @Test
    public void proceedFirstRunSetsSecretsAndStartsNotesListWhenFirstVersion() {
        when(activity.getApplicationContext()).thenReturn(context);
        when(appSecurityService.isAuthConfigured()).thenReturn(false);

        byte[] passwordBytes = FAKER.internet.password().getBytes(StandardCharsets.UTF_8);
        byte[] passwordBytesCopy = passwordBytes.clone();
        SecretCache.put(SetupKeyActivity.CACHE_KEY_PASSWORD, passwordBytesCopy);

        DataVersionManager dataVersionManager = mock(DataVersionManager.class);
        when(dataVersionManager.getCurrentVersion())
                .thenReturn(DataVersionManager.DEFAULT_FIRST_VERSION);

        byte[] keyBytes = getTestKey();

        KeySetupCompletionHandler handler = new KeySetupCompletionHandler(activity,
                appSecurityService, keyBytes);
        handler.proceedFirstRun(dataVersionManager);

        ArgumentCaptor<CryptoSecrets> secretsCaptor = ArgumentCaptor.forClass(CryptoSecrets.class);
        verify(appSecurityService).setSecrets(secretsCaptor.capture());
        CryptoSecrets passedSecrets = secretsCaptor.getValue();

        assertArrayEquals("Key bytes should be passed to CryptoSecrets",
                keyBytes, passedSecrets.getKey());
        assertArrayEquals("Password chars should be passed to CryptoSecrets",
                new String(passwordBytes, StandardCharsets.UTF_8).toCharArray(),
                passedSecrets.getPassword());
        assertArrayEquals("Password bytes should be zeroed after use",
                new byte[passwordBytes.length], passwordBytesCopy);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivity(intentCaptor.capture());
        Intent startedIntent = intentCaptor.getValue();

        assertNotNull("Intent should be created", startedIntent);
        assertNotNull("Intent component should be set", startedIntent.getComponent());
        assertEquals("Intent should target NotesListActivity",
                NotesListActivity.class.getName(), startedIntent.getComponent().getClassName());

        verify(activity).finish();
        verify(dataVersionManager).setCurrentVersion(BuildConfig.DATA_SCHEMA_VERSION);
    }

    @Test
    public void proceedFirstRunStartsMigrationWhenSchemaOutdated() {
        when(activity.getApplicationContext()).thenReturn(context);
        when(appSecurityService.isAuthConfigured()).thenReturn(false);

        byte[] passwordBytes = FAKER.internet.password().getBytes(StandardCharsets.UTF_8);
        SecretCache.put(SetupKeyActivity.CACHE_KEY_PASSWORD, passwordBytes);

        DataVersionManager dataVersionManager = mock(DataVersionManager.class);
        when(dataVersionManager.getCurrentVersion())
                .thenReturn(BuildConfig.DATA_SCHEMA_VERSION - 1);

        byte[] keyBytes = getTestKey();

        KeySetupCompletionHandler handler =
                new KeySetupCompletionHandler(activity, appSecurityService, keyBytes);
        handler.proceedFirstRun(dataVersionManager);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivity(intentCaptor.capture());
        Intent startedIntent = intentCaptor.getValue();

        assertNotNull(startedIntent);
        assertNotNull(startedIntent.getComponent());
        assertEquals("Intent should target MigrationActivity",
                MigrationActivity.class.getName(), startedIntent.getComponent().getClassName());

        verify(activity).finish();
    }

    @Test
    public void onRegenerationConfirmedPutsSecretsInCacheAndStartsSecretsUpdate() {
        when(activity.getApplicationContext()).thenReturn(context);
        when(appSecurityService.isAuthConfigured()).thenReturn(true);

        byte[] actualKey = getTestKey();
        char[] actualPassword = FAKER.internet.password().toCharArray();

        CryptoSecrets actual = new CryptoSecrets(actualKey, actualPassword.clone());
        when(appSecurityService.getActualSecrets()).thenReturn(actual);

        byte[] newKey = getTestKey();
        byte[] newKeyCopy = newKey.clone();

        KeySetupCompletionHandler handler = new KeySetupCompletionHandler(activity,
                appSecurityService, newKeyCopy);
        handler.onRegenerationConfirmed();

        byte[] storedNewKey = SecretCache.take(SecretsUpdateAndroidService.NEW_KEY);
        assertArrayEquals("New key bytes should be stored", newKey, storedNewKey);
        assertArrayEquals("Original new key bytes should be zeroed", new byte[newKey.length], newKeyCopy);

        byte[] storedPassword = SecretCache.take(SecretsUpdateAndroidService.PASSWORD);
        assertArrayEquals("Password bytes should be stored",
                new String(actualPassword).getBytes(StandardCharsets.UTF_8), storedPassword);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivity(intentCaptor.capture());
        Intent startedIntent = intentCaptor.getValue();

        assertNotNull(startedIntent);
        assertNotNull(startedIntent.getComponent());
        assertEquals("Intent should target SecretsUpdateActivity",
                SecretsUpdateActivity.class.getName(), startedIntent.getComponent().getClassName());

        verify(activity).finish();
    }

    @Test
    public void onRegenerationCanceledZeroesKeyBytes() {
        byte[] keyBytes = getTestKey();

        KeySetupCompletionHandler handler = new KeySetupCompletionHandler(activity,
                appSecurityService, keyBytes);
        handler.onRegenerationCanceled();

        assertArrayEquals("Key bytes should be zeroed after cancellation",
                new byte[keyBytes.length], keyBytes);
    }

    @Test
    public void getCurrentPasswordThrowsWhenNoPasswordAvailable() {
        when(appSecurityService.isAuthConfigured()).thenReturn(false);

        KeySetupCompletionHandler handler = new KeySetupCompletionHandler(activity,
                appSecurityService, new byte[]{1});
        assertThrows(IllegalStateException.class, handler::getCurrentPassword);
    }

    private byte[] getTestKey() {
        byte[] keyBytes = new byte[KEY_LENGTH];
        SECURE_RANDOM.nextBytes(keyBytes);
        return keyBytes;
    }

    private void clearCache() {
        SecretCache.removeIfExists(SetupKeyActivity.CACHE_KEY_PASSWORD);
        SecretCache.removeIfExists(SecretsUpdateAndroidService.NEW_KEY);
        SecretCache.removeIfExists(SecretsUpdateAndroidService.PASSWORD);
    }
}
