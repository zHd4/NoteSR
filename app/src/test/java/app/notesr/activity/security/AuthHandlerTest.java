/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.widget.TextView;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import app.notesr.BuildConfig;
import app.notesr.R;
import app.notesr.activity.FsaEntry;
import app.notesr.activity.FsaResolver;
import app.notesr.activity.migration.MigrationActivity;
import app.notesr.activity.note.list.NotesListActivity;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.CharUtils;
import app.notesr.core.util.KeyUtils;
import app.notesr.service.AndroidServiceBootstrapper;
import app.notesr.service.migration.DataVersionManager;
import app.notesr.service.security.AppSecurityService;
import app.notesr.service.security.AuthenticationFailedException;
import app.notesr.core.util.SecureStringBuilder;
import app.notesr.service.security.rotation.SecretsRotationService;

@ExtendWith(MockitoExtension.class)
class AuthHandlerTest {

    private static final int MAX_FAILED_AUTH_ATTEMPTS = 3;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Mock
    private AuthActivity activity;

    @Mock
    private AppSecurityService appSecurityService;

    @Mock
    private SecretsRotationService secretsRotationService;

    @Mock
    private SecureStringBuilder passwordBuilder;

    @Mock
    private FsaResolver fsaResolver;

    @Mock
    private AndroidServiceBootstrapper serviceBootstrapper;

    @Mock
    private DataVersionManager dataVersionManager;

    private AuthHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AuthHandler(activity, appSecurityService, secretsRotationService,
                passwordBuilder, fsaResolver, serviceBootstrapper, dataVersionManager);
    }

    @Test
    void testAuthenticateWithEmptyPassword() {
        when(passwordBuilder.toCharArray()).thenReturn(new char[0]);

        AuthHandler spyAuthHandler = spy(handler);
        doNothing().when(spyAuthHandler).showToastMessage(any(Integer.class));

        spyAuthHandler.authenticate();

        verify(spyAuthHandler, times(1))
                .showToastMessage(R.string.enter_the_code);
        verify(spyAuthHandler, never()).onAuthenticationSuccessful();
        verify(spyAuthHandler, never()).onAuthenticationFailed();
    }

    @Test
    void testAuthenticateWithWrongPassword() throws Exception {
        char[] testPassword = "wrong".toCharArray();

        TextView mockCensoredPasswordView = mock(TextView.class);
        when(activity.findViewById(R.id.censoredPasswordTextView))
                .thenReturn(mockCensoredPasswordView);

        when(passwordBuilder.toCharArray()).thenReturn(testPassword);
        doThrow(new AuthenticationFailedException("Authentication failed"))
                .when(appSecurityService).authenticate(testPassword);

        when(activity.getString(R.string.wrong_code_you_have_n_attempts))
                .thenReturn("Wrong code, you have %d attempts remaining");

        AuthHandler spyAuthHandler = spy(handler);
        doNothing().when(spyAuthHandler).showToastMessage(any(String.class));

        spyAuthHandler.authenticate();

        assertEquals(MAX_FAILED_AUTH_ATTEMPTS - 1, spyAuthHandler.getAuthAttempts(),
                "Auth attempts should decrement after failed authentication");

        verify(appSecurityService, times(1)).authenticate(testPassword);
        verify(spyAuthHandler, times(1)).onAuthenticationFailed();
        verify(spyAuthHandler, times(1)).sleepBeforeRetry();
        verify(spyAuthHandler, never()).onAuthenticationSuccessful();
        verify(spyAuthHandler, times(1)).resetPassword();
    }

    @Test
    void testAuthenticateSuccessfulAuthentication() {
        char[] testPassword = "qwerty".toCharArray();

        TextView mockCensoredPasswordView = mock(TextView.class);
        when(activity.findViewById(R.id.censoredPasswordTextView))
                .thenReturn(mockCensoredPasswordView);

        when(passwordBuilder.toCharArray()).thenReturn(testPassword);

        AuthHandler spyAuthHandler = spy(handler);

        spyAuthHandler.authenticate();

        verify(appSecurityService, times(1)).authenticate(testPassword);
        verify(spyAuthHandler, times(1)).onAuthenticationSuccessful();
        verify(spyAuthHandler, never()).onAuthenticationFailed();
        verify(mockCensoredPasswordView, times(1)).setText("");
        verify(serviceBootstrapper, times(1))
                .startServicesPostAuth(any(), any());
        verify(activity, times(1)).startActivity(any(Intent.class));
        verify(activity, times(1)).finish();
    }

    @Test
    void testProceedPasswordSettingMinLengthAndRepeatConfirmation() {
        char[] testPassword = "qwerty1".toCharArray();

        TextView mockTopLabel = mock(TextView.class);
        when(activity.findViewById(R.id.authTopLabel)).thenReturn(mockTopLabel);

        TextView mockCensoredPasswordView = mock(TextView.class);
        when(activity.findViewById(R.id.censoredPasswordTextView))
                .thenReturn(mockCensoredPasswordView);

        when(activity.getString(R.string.repeat_access_code))
                .thenReturn("Repeat access code");
        when(passwordBuilder.length()).thenReturn(CryptoSecrets.PASSWORD_MIN_LENGTH);

        AuthHandler spyAuthHandler = spy(handler);

        // First entry: password meets minimum length
        when(passwordBuilder.toCharArray()).thenReturn(testPassword);
        char[] firstAttempt = spyAuthHandler.proceedPasswordSetting();

        assertNull(firstAttempt,
                "First attempt should return null (awaiting confirmation)");

        // Second entry: user confirms with same password
        when(passwordBuilder.toCharArray()).thenReturn(testPassword);
        char[] confirmed = spyAuthHandler.proceedPasswordSetting();

        assertNotNull(confirmed, "Confirmation should return the confirmed password");
        assertArrayEquals(testPassword, confirmed, "Passwords should match");
        verify(mockTopLabel, times(1)).setText("Repeat access code");
        verify(spyAuthHandler, times(2)).resetPassword();
        verify(mockCensoredPasswordView, times(2)).setText("");
        verify(passwordBuilder, times(2)).wipe();
        verify(spyAuthHandler, never()).showToastMessage(any(String.class));
    }

    @Test
    void testProceedPasswordSettingBelowMinLength() {
        char[] shortPassword = "abc".toCharArray();

        TextView mockTopLabel = mock(TextView.class);
        when(activity.findViewById(R.id.authTopLabel)).thenReturn(mockTopLabel);

        TextView censoredPasswordView = mock(TextView.class);
        when(activity.findViewById(R.id.censoredPasswordTextView)).thenReturn(censoredPasswordView);

        when(passwordBuilder.length()).thenReturn(shortPassword.length);
        when(passwordBuilder.toCharArray()).thenReturn(shortPassword);
        when(activity.getString(R.string.minimum_password_length_is_n))
                .thenReturn("Minimum password length is %d");

        AuthHandler spyAuthHandler = spy(handler);
        doNothing().when(spyAuthHandler).showToastMessage(any(String.class));

        char[] result = spyAuthHandler.proceedPasswordSetting();

        assertNull(result,
                "Result should be null for short password");
        assertNull(spyAuthHandler.getCreatedPassword(),
                "Created password should remain null for short password");

        verify(spyAuthHandler, times(1)).showToastMessage(any(String.class));
        verify(spyAuthHandler, times(1)).resetPassword();
        verify(spyAuthHandler, times(1)).resetPassword();
        verify(spyAuthHandler, times(1))
                .showToastMessage(String.format("Minimum password length is %d",
                        CryptoSecrets.PASSWORD_MIN_LENGTH));
        verify(censoredPasswordView, times(1)).setText("");
        verify(passwordBuilder, times(1)).wipe();
        verify(mockTopLabel, never()).setText(any(String.class));
    }

    @Test
    void testProceedPasswordSettingMismatch() {
        char[] firstPassword = "qwerty1".toCharArray();
        char[] secondPassword = "qwerty2".toCharArray();

        TextView mockTopLabel = mock(TextView.class);
        when(activity.findViewById(R.id.authTopLabel)).thenReturn(mockTopLabel);
        when(passwordBuilder.length()).thenReturn(CryptoSecrets.PASSWORD_MIN_LENGTH);

        TextView mockCensoredPasswordView = mock(TextView.class);
        when(activity.findViewById(R.id.censoredPasswordTextView))
                .thenReturn(mockCensoredPasswordView);

        AuthHandler spyAuthHandler = spy(handler);
        doNothing().when(spyAuthHandler).showToastMessage(any(Integer.class));

        // First entry
        when(passwordBuilder.toCharArray()).thenReturn(firstPassword);
        char[] firstAttempt = spyAuthHandler.proceedPasswordSetting();
        assertNull(firstAttempt, "First password entry should be pending confirmation");

        // Second entry: different password
        when(passwordBuilder.toCharArray()).thenReturn(secondPassword);
        char[] secondAttempt = spyAuthHandler.proceedPasswordSetting();

        assertNull(secondAttempt, "Mismatched confirmation should return null");
        verify(spyAuthHandler, times(1))
                .showToastMessage(R.string.code_not_match);
        verify(spyAuthHandler, times(2)).resetPassword();
    }

    @Test
    void testCreatePasswordSuccessful() {
        char[] testPassword = "qwerty1".toCharArray();

        AuthHandler spyAuthHandler = spy(handler);
        doReturn(testPassword).when(spyAuthHandler).proceedPasswordSetting();

        Intent mockSetupKeyActivityIntent = mock(Intent.class);
        doReturn(mockSetupKeyActivityIntent).when(spyAuthHandler)
                .getNewIntent(SetupKeyActivity.class);
        doReturn(mockSetupKeyActivityIntent).when(mockSetupKeyActivityIntent)
                .putExtra(any(String.class), any(String.class));

        try (MockedStatic<SecretCache> mockSecretCache = mockStatic(SecretCache.class)) {
            spyAuthHandler.createPassword();

            mockSecretCache.verify(() ->
                    SecretCache.put(eq("password"), any(byte[].class)),
                    times(1));

            verify(spyAuthHandler, times(1))
                    .getNewIntent(SetupKeyActivity.class);
            verify(mockSetupKeyActivityIntent, times(1))
                    .putExtra("mode", "first_run");
            verify(activity, times(1))
                    .startActivity(mockSetupKeyActivityIntent);
            verify(activity, times(1)).finish();
        }
    }

    @Test
    void testCreatePasswordCancelledByUser() {
        AuthHandler spyAuthHandler = spy(handler);
        doReturn(null).when(spyAuthHandler).proceedPasswordSetting();

        spyAuthHandler.createPassword();

        verify(activity, never()).startActivity(any(Intent.class));
        verify(activity, never()).finish();
    }

    @Test
    void testRecoverKeyWithMissingHexKey() {
        char[] testPassword = "qwerty1".toCharArray();

        AuthHandler spyAuthHandler = spy(handler);
        doReturn(testPassword).when(spyAuthHandler).proceedPasswordSetting();

        try (MockedStatic<SecretCache> mockSecretCache = mockStatic(SecretCache.class)) {
            mockSecretCache.when(() -> SecretCache.take("hexKey")).thenReturn(null);

            assertThrows(RuntimeException.class, spyAuthHandler::recoverKey,
                    "Recovering a key without a stored hex key should fail");
        }
    }

    @Test
    void testRecoverKeySuccessful() throws Exception {
        char[] password = "qwerty1".toCharArray();

        byte[] keyBytes = new byte[CryptoSecrets.MASTER_KEY_SIZE];
        SECURE_RANDOM.nextBytes(keyBytes);

        char[] hexKey = KeyUtils.getKeyHexFromKeyBytes(keyBytes);
        byte[] hexKeyBytes = CharUtils.charsToBytes(hexKey.clone(), StandardCharsets.UTF_8);

        CryptoSecrets mockSecrets = mock(CryptoSecrets.class);

        AuthHandler spyAuthHandler = spy(handler);
        doReturn(password).when(spyAuthHandler).proceedPasswordSetting();

        try (MockedStatic<SecretCache> mockSecretCache = mockStatic(SecretCache.class);
             MockedStatic<CharUtils> mockCharUtils = mockStatic(CharUtils.class);
             MockedStatic<KeyUtils> mockKeyUtils = mockStatic(KeyUtils.class)) {

            mockSecretCache.when(() ->
                    SecretCache.take("hexKey"))
                    .thenReturn(hexKeyBytes);
            mockCharUtils.when(() ->
                    CharUtils.bytesToChars(hexKeyBytes, StandardCharsets.UTF_8))
                    .thenReturn(hexKey);
            mockKeyUtils.when(() ->
                    KeyUtils.getSecretsFromKeyHexAndPassword(eq(hexKey), eq(password)))
                    .thenReturn(mockSecrets);

            spyAuthHandler.recoverKey();

            verify(appSecurityService, times(1)).unblockApp(mockSecrets);
            verify(mockSecrets, times(1)).destroy();
            verify(activity, times(1)).startActivity(any(Intent.class));
            verify(activity, times(1)).finish();
        }
    }

    @Test
    void testRecoverKeyCancelledByUser() {
        AuthHandler spyAuthHandler = spy(handler);
        doReturn(null).when(spyAuthHandler).proceedPasswordSetting();

        spyAuthHandler.recoverKey();

        verify(appSecurityService, never()).unblockApp(any());
        verify(activity, never()).startActivity(any(Intent.class));
        verify(activity, never()).finish();
    }

    @Test
    void testChangePasswordSuccessful() {
        char[] testPassword = "newpassword1".toCharArray();

        AuthHandler spyAuthHandler = spy(handler);
        doReturn(testPassword).when(spyAuthHandler).proceedPasswordSetting();
        doNothing().when(spyAuthHandler).showToastMessage(any(Integer.class));

        Intent mockNotesListIntent = mock(Intent.class);
        doReturn(mockNotesListIntent).when(spyAuthHandler).getNewIntent(NotesListActivity.class);

        spyAuthHandler.changePassword();

        verify(secretsRotationService, times(1))
                .updatePassword(testPassword);
        verify(spyAuthHandler, times(1)).showToastMessage(R.string.updated);
        verify(spyAuthHandler, times(1))
                .getNewIntent(NotesListActivity.class);
        verify(activity, times(1)).startActivity(mockNotesListIntent);
        verify(activity, times(1)).finish();
    }

    @Test
    void testChangePasswordCancelledByUser() {
        AuthHandler spyAuthHandler = spy(handler);
        doReturn(null).when(spyAuthHandler).proceedPasswordSetting();

        spyAuthHandler.changePassword();

        verify(secretsRotationService, never()).updatePassword(any(char[].class));
        verify(activity, never()).startActivity(any(Intent.class));
        verify(activity, never()).finish();
    }

    @Test
    void testOnAuthenticationFailedWithRetriesRemaining() throws Exception {
        when(activity.getString(R.string.wrong_code_you_have_n_attempts))
                .thenReturn("Wrong code, you have %d attempts remaining");

        TextView censoredPasswordView = mock(TextView.class);
        when(activity.findViewById(R.id.censoredPasswordTextView)).thenReturn(censoredPasswordView);

        AuthHandler spyAuthHandler = spy(handler);
        doNothing().when(spyAuthHandler).showToastMessage(any(String.class));
        doNothing().when(spyAuthHandler).sleepBeforeRetry();

        // Set authAttempts to 2 so one failure leaves 1 remaining
        spyAuthHandler.setAuthAttempts(2);

        spyAuthHandler.onAuthenticationFailed();

        assertEquals(1, spyAuthHandler.getAuthAttempts(),
                "Auth attempts should decrement");

        verify(appSecurityService, never()).blockApp();
        verify(spyAuthHandler, times(1)).sleepBeforeRetry();
        verify(spyAuthHandler, times(1))
                .showToastMessage("Wrong code, you have 1 attempts remaining");
        verify(spyAuthHandler, times(1)).resetPassword();
        verify(spyAuthHandler, never()).showToastMessage(R.string.blocked);
        verify(censoredPasswordView, times(1)).setText("");
        verify(passwordBuilder, times(1)).wipe();
        verify(activity, never()).startActivity(any(Intent.class));
        verify(activity, never()).finish();
    }

    @Test
    void testOnAuthenticationFailedBlocksAppWhenAttemptsExhausted() throws Exception {
        TextView censoredPasswordView = mock(TextView.class);
        when(activity.findViewById(R.id.censoredPasswordTextView)).thenReturn(censoredPasswordView);

        AuthHandler spyAuthHandler = spy(handler);
        doNothing().when(spyAuthHandler).showToastMessage(any(Integer.class));

        // Set authAttempts to 1 so one failure exhausts attempts
        spyAuthHandler.setAuthAttempts(1);

        Intent mockKeyRecoveryIntent = mock(Intent.class);
        doReturn(mockKeyRecoveryIntent).when(spyAuthHandler)
                .getNewIntent(KeyRecoveryActivity.class);

        spyAuthHandler.onAuthenticationFailed();

        assertEquals(0, spyAuthHandler.getAuthAttempts(),
                "Auth attempts should decrement to 0");

        verify(appSecurityService, times(1)).blockApp();
        verify(spyAuthHandler, times(1)).showToastMessage(R.string.blocked);
        verify(activity, times(1)).startActivity(mockKeyRecoveryIntent);
        verify(activity, times(1)).finish();
        verify(spyAuthHandler, times(1)).resetPassword();
        verify(censoredPasswordView, times(1)).setText("");
        verify(passwordBuilder, times(1)).wipe();

        verify(spyAuthHandler, never()).sleepBeforeRetry();
        verify(spyAuthHandler, never())
                .showToastMessage(contains("Wrong code"));
    }

    @Test
    void testGetNextIntentAfterAuthWithFsaEntry() {
        FsaEntry fsaEntry = new FsaEntry();
        fsaEntry.setActivityClass(NotesListActivity.class);
        when(fsaResolver.getFsaEntryOfCurrentRunningFs()).thenReturn(fsaEntry);

        AuthHandler spyAuthHandler = spy(handler);
        when(spyAuthHandler.getNewIntent(any())).thenReturn(null);

        spyAuthHandler.getNextIntentAfterAuth();

        verify(spyAuthHandler, times(1))
                .getNewIntent(eq(NotesListActivity.class));
    }

    @Test
    void testGetNextIntentAfterAuthWithMigrationNeeded() {
        int currentDataSchemaVersion = BuildConfig.DATA_SCHEMA_VERSION;
        int lastMigrationVersion = currentDataSchemaVersion - 1;

        when(fsaResolver.getFsaEntryOfCurrentRunningFs()).thenReturn(null);
        when(dataVersionManager.getCurrentVersion()).thenReturn(lastMigrationVersion);

        AuthHandler spyAuthHandler = spy(handler);
        when(spyAuthHandler.getNewIntent(any())).thenReturn(null);

        spyAuthHandler.getNextIntentAfterAuth();

        verify(spyAuthHandler, times(1))
                .getNewIntent(eq(MigrationActivity.class));
    }

    @Test
    void testGetNextIntentAfterAuthDefaultNotesListActivity() {
        when(fsaResolver.getFsaEntryOfCurrentRunningFs()).thenReturn(null);
        when(dataVersionManager.getCurrentVersion()).thenReturn(BuildConfig.DATA_SCHEMA_VERSION);

        AuthHandler spyAuthHandler = spy(handler);
        when(spyAuthHandler.getNewIntent(any())).thenReturn(null);

        spyAuthHandler.getNextIntentAfterAuth();

        verify(spyAuthHandler, times(1))
                .getNewIntent(eq(NotesListActivity.class));
    }
}
