/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import app.notesr.core.security.SecretCache;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.data.DatabaseProvider;

@ExtendWith(MockitoExtension.class)
class AppSecurityServiceTest {

    private static final int MASTER_KEY_SIZE = 48;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Mock
    private Context mockContext;

    @Mock
    private CryptoManager mockCryptoManager;

    private AppSecurityService appSecurityService;

    @BeforeEach
    void setUp() {
        appSecurityService = new AppSecurityService(mockContext, mockCryptoManager);
    }

    @Test
    void testConstructorWithContext() {
        try (MockedStatic<CryptoManagerProvider> mockedProvider = mockStatic(CryptoManagerProvider.class)) {
            mockedProvider.when(() -> CryptoManagerProvider.getInstance(mockContext))
                    .thenReturn(mockCryptoManager);

            AppSecurityService service = new AppSecurityService(mockContext);

            assertNotNull(service);
            mockedProvider.verify(() -> CryptoManagerProvider.getInstance(mockContext));
        }
    }

    @Test
    void testGetSecretsWithRandomKey() {
        char[] password = "testPassword123".toCharArray();

        CryptoSecrets secrets = appSecurityService.getSecretsWithRandomKey(password);

        assertNotNull(secrets);
        assertEquals(MASTER_KEY_SIZE, secrets.getKey().length);
    }

    @Test
    void testGetSecretsWithRandomKeyGeneratesDifferentKeys() {
        char[] password = "testPassword123".toCharArray();

        CryptoSecrets secrets1 = appSecurityService.getSecretsWithRandomKey(password);
        CryptoSecrets secrets2 = appSecurityService.getSecretsWithRandomKey(password);

        assertNotNull(secrets1);
        assertNotNull(secrets2);
        assertNotEquals(secrets1.getKey(), secrets2.getKey());
    }

    @Test
    void testGetActualSecrets() {
        CryptoSecrets expectedSecrets = mock(CryptoSecrets.class);
        when(mockCryptoManager.getSecrets()).thenReturn(expectedSecrets);

        CryptoSecrets actualSecrets = appSecurityService.getActualSecrets();

        assertEquals(expectedSecrets, actualSecrets);
        verify(mockCryptoManager).getSecrets();
    }

    @Test
    void testGetActualSecretsReturnsNull() {
        when(mockCryptoManager.getSecrets()).thenReturn(null);

        CryptoSecrets actualSecrets = appSecurityService.getActualSecrets();

        assertNull(actualSecrets);
        verify(mockCryptoManager).getSecrets();
    }

    @Test
    void testIsAppBlockedReturnsTrue() {
        when(mockCryptoManager.isBlocked(mockContext)).thenReturn(true);

        boolean isBlocked = appSecurityService.isAppBlocked();

        assertTrue(isBlocked);
        verify(mockCryptoManager).isBlocked(mockContext);
    }

    @Test
    void testIsAppBlockedReturnsFalse() {
        when(mockCryptoManager.isBlocked(mockContext)).thenReturn(false);

        boolean isBlocked = appSecurityService.isAppBlocked();

        assertFalse(isBlocked);
        verify(mockCryptoManager).isBlocked(mockContext);
    }

    @Test
    void testIsAuthConfiguredReturnsTrue() {
        when(mockCryptoManager.isConfigured()).thenReturn(true);

        boolean isConfigured = appSecurityService.isAuthConfigured();

        assertTrue(isConfigured);
        verify(mockCryptoManager).isConfigured();
    }

    @Test
    void testIsAuthConfiguredReturnsFalse() {
        when(mockCryptoManager.isConfigured()).thenReturn(false);

        boolean isConfigured = appSecurityService.isAuthConfigured();

        assertFalse(isConfigured);
        verify(mockCryptoManager).isConfigured();
    }

    @Test
    void testIsKeyExistsReturnsTrue() {
        when(mockCryptoManager.isKeyExists(mockContext)).thenReturn(true);

        boolean keyExists = appSecurityService.isKeyExists();

        assertTrue(keyExists);
        verify(mockCryptoManager).isKeyExists(mockContext);
    }

    @Test
    void testIsKeyExistsReturnsFalse() {
        when(mockCryptoManager.isKeyExists(mockContext)).thenReturn(false);

        boolean keyExists = appSecurityService.isKeyExists();

        assertFalse(keyExists);
        verify(mockCryptoManager).isKeyExists(mockContext);
    }

    @Test
    void testIsKeyMatchingWithStoredReturnsTrue() throws Exception {
        byte[] key = "testKey".getBytes();
        when(mockCryptoManager.verifyKey(mockContext, key)).thenReturn(true);

        boolean isMatching = appSecurityService.isKeyMatchingWithStored(key);

        assertTrue(isMatching);
        verify(mockCryptoManager).verifyKey(mockContext, key);
    }

    @Test
    void testIsKeyMatchingWithStoredReturnsFalse() throws Exception {
        byte[] key = "testKey".getBytes();
        when(mockCryptoManager.verifyKey(mockContext, key)).thenReturn(false);

        boolean isMatching = appSecurityService.isKeyMatchingWithStored(key);

        assertFalse(isMatching);
        verify(mockCryptoManager).verifyKey(mockContext, key);
    }

    @Test
    void testIsKeyMatchingWithStoredThrowsAppSecurityExceptionOnFileNotFound() throws Exception {
        byte[] key = "testKey".getBytes();
        when(mockCryptoManager.verifyKey(mockContext, key))
                .thenThrow(new FileNotFoundException("Key hash not found"));

        AppSecurityException exception = assertThrows(AppSecurityException.class,
                () -> appSecurityService.isKeyMatchingWithStored(key));

        assertEquals("Failed to verify key, key hash not found", exception.getMessage());
        verify(mockCryptoManager).verifyKey(mockContext, key);
    }

    @Test
    void testIsKeyMatchingWithStoredThrowsAppSecurityExceptionOnIOError() throws Exception {
        byte[] key = "testKey".getBytes();
        when(mockCryptoManager.verifyKey(mockContext, key))
                .thenThrow(new IOException("I/O error occurred"));

        AppSecurityException exception = assertThrows(AppSecurityException.class,
                () -> appSecurityService.isKeyMatchingWithStored(key));

        assertEquals("Failed to verify key, I/O issue", exception.getMessage());
        verify(mockCryptoManager).verifyKey(mockContext, key);
    }

    @Test
    void testIsKeyMatchingWithStoredThrowsAppSecurityExceptionOnAlgorithmError() throws Exception {
        byte[] key = "testKey".getBytes();
        when(mockCryptoManager.verifyKey(mockContext, key))
                .thenThrow(new NoSuchAlgorithmException("Algorithm not available"));

        AppSecurityException exception = assertThrows(AppSecurityException.class,
                () -> appSecurityService.isKeyMatchingWithStored(key));

        assertEquals("Failed to verify key, hash algorithm issue", exception.getMessage());
        verify(mockCryptoManager).verifyKey(mockContext, key);
    }

    @Test
    void testAuthenticateSuccess() throws Exception {
        char[] password = "validPassword123".toCharArray();

        appSecurityService.authenticate(password);

        verify(mockCryptoManager).configure(mockContext, password);
    }

    @Test
    void testAuthenticateThrowsAuthenticationFailedExceptionOnDecryptionFailure() throws Exception {
        char[] password = "invalidPassword".toCharArray();
        doThrow(new GeneralSecurityException())
                .when(mockCryptoManager).configure(mockContext, password);

        AuthenticationFailedException exception = assertThrows(AuthenticationFailedException.class,
                () -> appSecurityService.authenticate(password));

        assertEquals("Failed to authenticate, invalid password or cryptographic issue",
                exception.getMessage());
        verify(mockCryptoManager).configure(mockContext, password);
    }

    @Test
    void testAuthenticateThrowsAppSecurityExceptionOnIOError() throws Exception {
        char[] password = "testPassword".toCharArray();
        doThrow(new IOException("I/O error during authentication"))
                .when(mockCryptoManager).configure(mockContext, password);

        AppSecurityException exception = assertThrows(AppSecurityException.class,
                () -> appSecurityService.authenticate(password));

        assertEquals("An I/O error occurred while authenticating", exception.getMessage());
        verify(mockCryptoManager).configure(mockContext, password);
    }

    @Test
    void testLogoutClosesConnectionsAndClearsCaches() {
        try (MockedStatic<DatabaseProvider> mockedDatabaseProvider = mockStatic(DatabaseProvider.class);
             MockedStatic<SecretCache> mockedSecretCache = mockStatic(SecretCache.class)) {

            appSecurityService.logout();

            mockedDatabaseProvider.verify(DatabaseProvider::close);
            verify(mockCryptoManager).destroySecrets();
            mockedSecretCache.verify(SecretCache::clear);
        }
    }

    @Test
    void testSetSecretsSuccess() throws Exception {
        CryptoSecrets newSecrets = mock(CryptoSecrets.class);

        appSecurityService.setSecrets(newSecrets);

        verify(mockCryptoManager).setSecrets(mockContext, newSecrets);
        verify(newSecrets).destroy();
    }

    @Test
    void testSetSecretsThrowsWhenSecretsIsNull() {
        assertThrows(IllegalArgumentException.class, () -> appSecurityService.setSecrets(null),
                "setSecrets should throw IllegalArgumentException when secrets are null");
    }

    @Test
    void testSetSecretsThrowsWhenKeyIsNull() {
        CryptoSecrets secrets = new CryptoSecrets(null, "password".toCharArray());

        assertThrows(IllegalArgumentException.class, () -> appSecurityService.setSecrets(secrets),
                "setSecrets should throw IllegalArgumentException when key is null");
    }

    @Test
    void testSetSecretsThrowsWhenKeyIsEmpty() {
        CryptoSecrets secrets = new CryptoSecrets(new byte[0], "password".toCharArray());

        assertThrows(IllegalArgumentException.class, () -> appSecurityService.setSecrets(secrets),
                "setSecrets should throw IllegalArgumentException when key is empty");
    }

    @Test
    void testSetSecretsThrowsWhenKeyLengthIsInvalid() {
        byte[] invalidKey = generateRandomBytes(MASTER_KEY_SIZE - 1);
        CryptoSecrets secrets = new CryptoSecrets(invalidKey, "password".toCharArray());

        assertThrows(IllegalArgumentException.class, () -> appSecurityService.setSecrets(secrets),
                "setSecrets should throw IllegalArgumentException when key length is invalid");
    }

    @Test
    void testSetSecretsThrowsWhenPasswordIsNull() {
        byte[] key = generateRandomBytes(MASTER_KEY_SIZE);
        CryptoSecrets secrets = new CryptoSecrets(key, null);

        assertThrows(IllegalArgumentException.class, () -> appSecurityService.setSecrets(secrets),
                "setSecrets should throw IllegalArgumentException when password is null");
    }

    @Test
    void testSetSecretsThrowsWhenPasswordIsEmpty() {
        byte[] key = generateRandomBytes(MASTER_KEY_SIZE);
        CryptoSecrets secrets = new CryptoSecrets(key, new char[0]);

        assertThrows(IllegalArgumentException.class, () -> appSecurityService.setSecrets(secrets),
                "setSecrets should throw IllegalArgumentException when password is empty");
    }

    @Test
    void testSetSecretsThrowsWhenPasswordIsTooShort() {
        byte[] key = generateRandomBytes(MASTER_KEY_SIZE);
        CryptoSecrets secrets = new CryptoSecrets(key, "123".toCharArray());

        assertThrows(IllegalArgumentException.class, () -> appSecurityService.setSecrets(secrets),
                "setSecrets should throw IllegalArgumentException when password is too short");
    }

    @Test
    void testSetSecretsThrowsWhenKeyIsAllZeros() {
        byte[] key = new byte[MASTER_KEY_SIZE];
        CryptoSecrets secrets = new CryptoSecrets(key, "password".toCharArray());

        assertThrows(IllegalArgumentException.class, () -> appSecurityService.setSecrets(secrets),
                "setSecrets should throw IllegalArgumentException when key is all zeros");
    }

    @Test
    void testSetSecretsThrowsWhenPasswordIsAllZeros() {
        byte[] key = generateRandomBytes(MASTER_KEY_SIZE);
        CryptoSecrets secrets = new CryptoSecrets(key, new char[4]);

        assertThrows(IllegalArgumentException.class, () -> appSecurityService.setSecrets(secrets),
                "setSecrets should throw IllegalArgumentException when password is all zeros");
    }

    @Test
    void testSetSecretsThrowsAppSecurityExceptionOnEncryptionFailure() throws Exception {
        CryptoSecrets newSecrets = mock(CryptoSecrets.class);
        doThrow(new GeneralSecurityException())
                .when(mockCryptoManager).setSecrets(mockContext, newSecrets);

        AppSecurityException exception = assertThrows(AppSecurityException.class,
                () -> appSecurityService.setSecrets(newSecrets));

        assertEquals("Failed to set new secrets, encryption issue", exception.getMessage());
        verify(mockCryptoManager).setSecrets(mockContext, newSecrets);
        verify(newSecrets).validate();
        verify(newSecrets).destroy();
    }

    @Test
    void testSetSecretsDestroysSecretsEvenOnException() throws Exception {
        CryptoSecrets newSecrets = mock(CryptoSecrets.class);
        doThrow(new GeneralSecurityException())
                .when(mockCryptoManager).setSecrets(mockContext, newSecrets);

        assertThrows(AppSecurityException.class,
                () -> appSecurityService.setSecrets(newSecrets));

        verify(newSecrets).validate();
        verify(newSecrets).destroy();
    }

    @Test
    void testBlockAppSuccess() throws Exception {
        appSecurityService.blockApp();

        verify(mockCryptoManager).block(mockContext);
    }

    @Test
    void testBlockAppThrowsAppSecurityExceptionOnIOError() throws Exception {
        doThrow(new IOException("I/O error during blocking"))
                .when(mockCryptoManager).block(mockContext);

        AppSecurityException exception = assertThrows(AppSecurityException.class,
                () -> appSecurityService.blockApp());

        assertEquals("Failed to block app, I/O issue", exception.getMessage());
        verify(mockCryptoManager).block(mockContext);
    }

    @Test
    void testUnblockAppSuccess() throws Exception {
        CryptoSecrets secrets = mock(CryptoSecrets.class);
        byte[] key = "validKey".getBytes();
        when(secrets.getKey()).thenReturn(key);
        when(mockCryptoManager.verifyKey(mockContext, key)).thenReturn(true);

        appSecurityService.unblockApp(secrets);

        verify(mockCryptoManager).verifyKey(mockContext, key);
        verify(mockCryptoManager).setSecrets(mockContext, secrets);
        verify(mockCryptoManager).unblock(mockContext);
    }

    @Test
    void testUnblockAppThrowsAuthenticationFailedExceptionOnInvalidKey() throws Exception {
        CryptoSecrets secrets = mock(CryptoSecrets.class);
        byte[] key = "invalidKey".getBytes();
        when(secrets.getKey()).thenReturn(key);
        when(mockCryptoManager.verifyKey(mockContext, key)).thenReturn(false);

        AuthenticationFailedException exception = assertThrows(AuthenticationFailedException.class,
                () -> appSecurityService.unblockApp(secrets));

        assertEquals("Failed to unblock app, invalid key", exception.getMessage());
        verify(mockCryptoManager).verifyKey(mockContext, key);
        verify(mockCryptoManager, never()).setSecrets(any(), any());
        verify(mockCryptoManager, never()).unblock(any());
    }

    @Test
    void testUnblockAppThrowsAppSecurityExceptionOnIOError() throws Exception {
        CryptoSecrets secrets = mock(CryptoSecrets.class);
        byte[] key = "validKey".getBytes();
        when(secrets.getKey()).thenReturn(key);
        when(mockCryptoManager.verifyKey(mockContext, key)).thenReturn(true);
        doThrow(new IOException("I/O error during unblocking"))
                .when(mockCryptoManager).setSecrets(mockContext, secrets);

        AppSecurityException exception = assertThrows(AppSecurityException.class,
                () -> appSecurityService.unblockApp(secrets));

        assertEquals("Failed to unblock app, I/O issue", exception.getMessage());
    }

    @Test
    void testUnblockAppThrowsAppSecurityExceptionOnEncryptionError() throws Exception {
        CryptoSecrets secrets = mock(CryptoSecrets.class);
        byte[] key = "validKey".getBytes();
        when(secrets.getKey()).thenReturn(key);
        when(mockCryptoManager.verifyKey(mockContext, key)).thenReturn(true);
        doThrow(new GeneralSecurityException())
                .when(mockCryptoManager).setSecrets(mockContext, secrets);

        AppSecurityException exception = assertThrows(AppSecurityException.class,
                () -> appSecurityService.unblockApp(secrets));

        assertEquals("Failed to unblock app, encryption issue", exception.getMessage());
    }

    @Test
    void testUnblockAppThrowsAppSecurityExceptionOnAlgorithmError() throws Exception {
        CryptoSecrets secrets = mock(CryptoSecrets.class);
        byte[] key = "validKey".getBytes();
        when(secrets.getKey()).thenReturn(key);
        when(mockCryptoManager.verifyKey(mockContext, key))
                .thenThrow(new NoSuchAlgorithmException("Algorithm not available"));

        AppSecurityException exception = assertThrows(AppSecurityException.class,
                () -> appSecurityService.unblockApp(secrets));

        assertEquals("Failed to unblock app, hash algorithm issue", exception.getMessage());
    }

    @Test
    void testUnblockAppVerifiesKeyBeforeUnblocking() throws Exception {
        CryptoSecrets secrets = mock(CryptoSecrets.class);
        byte[] key = "testKey".getBytes();
        when(secrets.getKey()).thenReturn(key);
        when(mockCryptoManager.verifyKey(mockContext, key)).thenReturn(true);

        appSecurityService.unblockApp(secrets);

        InOrder inOrder = inOrder(mockCryptoManager);
        inOrder.verify(mockCryptoManager).verifyKey(mockContext, key);
        inOrder.verify(mockCryptoManager).setSecrets(mockContext, secrets);
        inOrder.verify(mockCryptoManager).unblock(mockContext);
    }

    private byte[] generateRandomBytes(int size) {
        byte[] key = new byte[size];
        SECURE_RANDOM.nextBytes(key);
        return key;
    }
}
