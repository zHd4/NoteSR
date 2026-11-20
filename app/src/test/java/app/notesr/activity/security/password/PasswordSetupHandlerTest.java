/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security.password;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.notesr.activity.security.KeySetupMode;
import app.notesr.activity.security.SetupKeyActivity;
import app.notesr.core.security.SecretCache;

class PasswordSetupHandlerTest {

    private PasswordHelper passwordHelper;
    private PasswordFlowCompletionHelper passwordFlowCompletionHelper;
    private PasswordSetupHandler handler;

    @BeforeEach
    void setUp() {
        SecretCache.clear();

        passwordHelper = mock(PasswordHelper.class);
        passwordFlowCompletionHelper = mock(PasswordFlowCompletionHelper.class);
        handler = new PasswordSetupHandler(passwordHelper, passwordFlowCompletionHelper);
    }

    @AfterEach
    void tearDown() {
        SecretCache.clear();
    }

    @Test
    void testCreatePasswordWaitsWhenPasswordIsNotConfirmed() throws CharacterCodingException {
        when(passwordHelper.proceedPasswordSetting()).thenReturn(null);

        handler.createPassword();

        assertFalse(SecretCache.contains(SetupKeyActivity.CACHE_KEY_PASSWORD),
                "An unconfirmed password should not be stored in the secret cache");

        verify(passwordFlowCompletionHelper, never()).getNewActivityIntent(SetupKeyActivity.class);
        verify(passwordFlowCompletionHelper, never()).navigateNextActivity(
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void testCreatePasswordCachesUtf8PasswordAndNavigatesToFirstRunKeySetup()
            throws CharacterCodingException {
        char[] password = "pässword".toCharArray();
        Intent setupKeyIntent = mock(Intent.class);

        when(passwordHelper.proceedPasswordSetting()).thenReturn(password);
        when(passwordFlowCompletionHelper.getNewActivityIntent(SetupKeyActivity.class))
                .thenReturn(setupKeyIntent);
        when(setupKeyIntent.putExtra(SetupKeyActivity.EXTRA_MODE,
                KeySetupMode.FIRST_RUN.getModeName())).thenReturn(setupKeyIntent);

        handler.createPassword();

        assertArrayEquals("pässword".getBytes(StandardCharsets.UTF_8),
                SecretCache.take(SetupKeyActivity.CACHE_KEY_PASSWORD),
                "The confirmed password should be cached as UTF-8 bytes");

        verify(passwordFlowCompletionHelper).getNewActivityIntent(SetupKeyActivity.class);
        verify(setupKeyIntent).putExtra(SetupKeyActivity.EXTRA_MODE,
                KeySetupMode.FIRST_RUN.getModeName());
        verify(passwordFlowCompletionHelper).navigateNextActivity(setupKeyIntent);
    }

    @Test
    void testCreatePasswordPropagatesEncodingFailureWithoutCachingOrNavigating() {
        when(passwordHelper.proceedPasswordSetting()).thenReturn(new char[]{'\uD800'});

        assertThrows(CharacterCodingException.class, handler::createPassword,
                "An invalid UTF-16 password should propagate its UTF-8 encoding failure");

        assertFalse(SecretCache.contains(SetupKeyActivity.CACHE_KEY_PASSWORD),
                "A password that cannot be encoded should not be cached");

        verify(passwordFlowCompletionHelper, never()).getNewActivityIntent(SetupKeyActivity.class);
        verify(passwordFlowCompletionHelper, never()).navigateNextActivity(
                org.mockito.ArgumentMatchers.any());
    }
}
