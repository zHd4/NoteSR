/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security.password;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import app.notesr.activity.note.list.NotesListActivity;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.service.security.AppSecurityService;

class KeyRecoveryHandlerTest {

    private AppPasswordActivity activity;
    private AppSecurityService appSecurityService;
    private PasswordFlowCompletionHelper completionHelper;
    private PasswordHelper passwordHelper;
    private KeyRecoveryHandler handler;

    @BeforeEach
    void setUp() {
        SecretCache.clear();

        activity = mock(AppPasswordActivity.class);
        appSecurityService = mock(AppSecurityService.class);
        completionHelper = mock(PasswordFlowCompletionHelper.class);
        passwordHelper = mock(PasswordHelper.class);
        handler = new KeyRecoveryHandler(activity, appSecurityService, completionHelper,
                passwordHelper);
    }

    @AfterEach
    void tearDown() {
        SecretCache.clear();
    }

    @Test
    void testRecoverKeyWaitsWhenPasswordIsNotConfirmed() throws CharacterCodingException {
        when(passwordHelper.proceedPasswordSetting()).thenReturn(null);

        handler.recoverKey();

        verifyNoInteractions(appSecurityService);
        verifyNoInteractions(completionHelper);
    }

    @Test
    void testRecoverKeyUnblocksAppAndCompletesPostAuthFlow() throws CharacterCodingException {
        byte[] cachedKeyHex = "01 23 45".getBytes(StandardCharsets.UTF_8);
        char[] password = "new-password".toCharArray();

        SecretCache.put(AppPasswordActivity.CACHE_KEY_HEX_KEY, cachedKeyHex);

        when(passwordHelper.proceedPasswordSetting()).thenReturn(password);

        Context applicationContext = mock(Context.class);
        Intent notesListIntent = mock(Intent.class);

        when(activity.getApplicationContext()).thenReturn(applicationContext);
        when(completionHelper.getNewActivityIntent(NotesListActivity.class))
                .thenReturn(notesListIntent);

        Mockito.doAnswer(invocation -> {
            CryptoSecrets secrets = invocation.getArgument(0);

            assertArrayEquals(new byte[]{0x01, 0x23, 0x45}, secrets.getKey(),
                    "The app should be unlocked with the recovered key bytes");
            assertArrayEquals("new-password".toCharArray(), secrets.getPassword(),
                    "The app should be unlocked with the confirmed password");

            return null;
        }).when(appSecurityService).unblockApp(any(CryptoSecrets.class));

        handler.recoverKey();

        ArgumentCaptor<CryptoSecrets> secretsCaptor = ArgumentCaptor.forClass(CryptoSecrets.class);
        verify(appSecurityService).unblockApp(secretsCaptor.capture());

        assertArrayEquals(new byte[3], secretsCaptor.getValue().getKey(),
                "Recovered key bytes should be destroyed after unlocking the app");
        assertArrayEquals(new char[password.length], secretsCaptor.getValue().getPassword(),
                "The confirmed password should be destroyed after unlocking the app");
        assertArrayEquals(new char[password.length], password,
                "The input password should be cleared after creating the recovery secrets");
        assertArrayEquals(new byte[cachedKeyHex.length], cachedKeyHex,
                "The cached key bytes should be cleared when consumed");
        assertFalse(SecretCache.contains(AppPasswordActivity.CACHE_KEY_HEX_KEY),
                "The recovered key should be removed from the secret cache");

        verify(passwordHelper).resetPassword();
        verify(completionHelper).bootstrapPostAuthServices(applicationContext);
        verify(completionHelper).getNewActivityIntent(NotesListActivity.class);
        verify(completionHelper).navigateNextOrIntermediateActivity(notesListIntent);
    }

    @Test
    void testRecoverKeyThrowsWhenCachedKeyIsMissing() {
        when(passwordHelper.proceedPasswordSetting()).thenReturn("password".toCharArray());

        RuntimeException exception = assertThrows(RuntimeException.class, handler::recoverKey,
                "Recovery should fail when no key is cached");

        assertEquals("Missing hex key", exception.getMessage(),
                "The missing-key failure should identify the absent cached key");

        verifyNoInteractions(appSecurityService);
        verifyNoInteractions(completionHelper);
        verify(passwordHelper, never()).resetPassword();
    }

    @Test
    void testRecoverKeyPropagatesInvalidHexWithoutUnlockingOrCompletingFlow() {
        byte[] cachedKeyHex = "invalid".getBytes(StandardCharsets.UTF_8);
        SecretCache.put(AppPasswordActivity.CACHE_KEY_HEX_KEY, cachedKeyHex);

        when(passwordHelper.proceedPasswordSetting()).thenReturn("password".toCharArray());

        assertThrows(IllegalArgumentException.class, handler::recoverKey,
                "Invalid cached hex should propagate its parsing failure");

        assertArrayEquals(new byte[cachedKeyHex.length], cachedKeyHex,
                "The invalid cached key bytes should be cleared when consumed");
        assertFalse(SecretCache.contains(AppPasswordActivity.CACHE_KEY_HEX_KEY),
                "The invalid cached key should be removed from the secret cache");

        verifyNoInteractions(appSecurityService);
        verifyNoInteractions(completionHelper);
        verify(passwordHelper, never()).resetPassword();
    }

    @Test
    void testRecoverKeyPropagatesInvalidUtf8WithoutUnlockingOrCompletingFlow() {
        byte[] invalidUtf8 = {(byte) 0xC3, (byte) 0x28};
        SecretCache.put(AppPasswordActivity.CACHE_KEY_HEX_KEY, invalidUtf8);

        when(passwordHelper.proceedPasswordSetting()).thenReturn("password".toCharArray());

        assertThrows(CharacterCodingException.class, handler::recoverKey,
                "Invalid UTF-8 in the cached key should propagate its decoding failure");

        assertArrayEquals(new byte[invalidUtf8.length], invalidUtf8,
                "The invalid cached key bytes should be cleared when consumed");

        verifyNoInteractions(appSecurityService);
        verifyNoInteractions(completionHelper);
        verify(passwordHelper, never()).resetPassword();
    }
}
