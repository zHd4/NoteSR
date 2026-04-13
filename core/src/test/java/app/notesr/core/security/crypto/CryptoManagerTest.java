/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.security.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static app.notesr.core.util.HashUtils.fromSha256HexString;
import static app.notesr.core.util.HashUtils.toSha256Bytes;
import static app.notesr.core.util.HashUtils.toSha256String;

import android.content.SharedPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.security.exception.SessionExpiredException;
import app.notesr.core.util.FilesUtilsAdapter;
import app.notesr.core.util.WiperAdapter;

@ExtendWith(MockitoExtension.class)
class CryptoManagerTest {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom(new byte[]{1, 2, 3});

    @Mock
    private SharedPreferences prefs;

    @Mock
    private SharedPreferences.Editor editor;

    @Mock
    private FilesUtilsAdapter filesUtils;

    @Mock
    private WiperAdapter wiper;

    @Mock
    private CryptorFactory cryptorFactory;

    private CryptoManager cryptoManager;

    @BeforeEach
    void setUp() {
        cryptoManager = new CryptoManager(prefs, filesUtils, wiper, SECURE_RANDOM, cryptorFactory);
    }

    @Test
    void testGenerateSecretsCreatesKeyOfCorrectSize() {
        CryptoSecrets secrets = cryptoManager.generateSecrets("pass".toCharArray());
        assertEquals(CryptoManager.KEY_SIZE, secrets.getKey().length,
                "Generated key size should match constant");
        assertArrayEquals("pass".toCharArray(), secrets.getPassword(),
                "Generated secrets should contain the provided password");
    }

    @Test
    void testisKeyExistsReturnsTrueWhenFileExists() {
        File mockFile = mock(File.class);
        when(filesUtils.getInternalFile(null, "key.encrypted")).thenReturn(mockFile);
        when(mockFile.exists()).thenReturn(true);

        assertTrue(cryptoManager.isKeyExists(null),
                "isKeyExists should return true when the encrypted key file exists");
    }

    @Test
    void testBlockSetsPrefAndWipesFile() throws IOException {
        when(prefs.edit()).thenReturn(editor);
        when(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor);

        File mockFile = mock(File.class);
        when(filesUtils.getInternalFile(null, "key.encrypted")).thenReturn(mockFile);

        cryptoManager.block(null);

        verify(wiper).wipeFile(mockFile);
        verify(editor).putBoolean("is_blocked", true);
        verify(editor).apply();
    }

    @Test
    void testUnblockRemovesMarkerFile() throws IOException {
        when(prefs.edit()).thenReturn(editor);
        when(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor);

        File markerFile = mock(File.class);
        when(filesUtils.getInternalFile(null, ".blocked")).thenReturn(markerFile);
        when(markerFile.exists()).thenReturn(true);
        when(markerFile.delete()).thenReturn(true);

        cryptoManager.unblock(null);

        verify(editor).putBoolean("is_blocked", false);
        verify(editor).apply();
        verify(markerFile).delete();
    }

    @Test
    void testVerifyKeyReturnsTrueWhenHashesMatch() throws Exception {
        byte[] key = new byte[]{1, 2, 3};
        String hash = toSha256String(key);
        when(prefs.getString("key_hash", null)).thenReturn(hash);

        boolean result = cryptoManager.verifyKey(null, key);

        assertTrue(result,
                "verifyKey should return true when" +
                        " the provided key's hash matches the stored hash");
    }

    @Test
    void testVerifyKeyReturnsFalseWhenHashesDoNotMatch() throws Exception {
        byte[] key = new byte[]{1, 2, 3};
        byte[] otherKey = new byte[]{4, 5, 6};
        byte[] hash = toSha256Bytes(otherKey);
        when(prefs.getString("key_hash", null)).thenReturn(toSha256String(hash));

        boolean result = cryptoManager.verifyKey(null, key);

        assertFalse(result,
                "verifyKey should return false when" +
                        " the provided key's hash does not match the stored hash");
    }

    @Test
    void testSetKeyHashDoubleHashingProblem() throws Exception {
        AesGcmCryptor mockCryptor = mock(AesGcmCryptor.class);

        when(mockCryptor.encrypt(any(byte[].class))).thenAnswer(invocation -> {
            byte[] input = invocation.getArgument(0);
            return Arrays.copyOf(input, input.length);
        });

        when(cryptorFactory.create(any(char[].class), eq(AesGcmCryptor.class)))
                .thenReturn(mockCryptor);

        when(prefs.edit()).thenReturn(editor);
        when(editor.putString(anyString(), anyString())).thenReturn(editor);

        File mockEncryptedFile = mock(File.class);
        File mockKeyHashFile = mock(File.class);

        when(filesUtils.getInternalFile(null, "key.encrypted"))
                .thenReturn(mockEncryptedFile);
        when(filesUtils.getInternalFile(null, "key.sha256"))
                .thenReturn(mockKeyHashFile);

        when(mockKeyHashFile.exists()).thenReturn(false);

        byte[] key = new byte[]{10, 20, 30};
        CryptoSecrets secrets = new CryptoSecrets(key, "password".toCharArray());

        cryptoManager.setSecrets(null, secrets);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(editor).putString(eq("key_hash"), captor.capture());

        String storedValue = captor.getValue();

        byte[] storedHash = fromSha256HexString(storedValue);

        byte[] expected = toSha256Bytes(key);

        assertArrayEquals(expected, storedHash,
                "The saved hash was expected to be SHA-256(key), " +
                        "but a double hash was stored instead");
    }

    @Test
    void testConfigureSuccess() throws Exception {
        char[] password = "password".toCharArray();
        byte[] encryptedKey = new byte[]{1, 2, 3};
        byte[] decryptedKey = new byte[CryptoManager.KEY_SIZE];
        SECURE_RANDOM.nextBytes(decryptedKey);

        File keyFile = mock(File.class);
        when(filesUtils.getInternalFile(null, "key.encrypted")).thenReturn(keyFile);
        when(filesUtils.readFileBytes(keyFile)).thenReturn(encryptedKey);

        AesGcmCryptor mockCryptor = mock(AesGcmCryptor.class);
        when(cryptorFactory.create(eq(password), eq(AesGcmCryptor.class))).thenReturn(mockCryptor);
        when(mockCryptor.decrypt(encryptedKey)).thenReturn(decryptedKey);

        boolean result = cryptoManager.configure(null, password);

        assertTrue(result, "configure should return true on success");
        assertTrue(cryptoManager.isConfigured(),
                "CryptoManager should be configured after successful configuration");
        assertArrayEquals(decryptedKey, cryptoManager.getSecrets().getKey(),
                "The stored key should match the decrypted key");
    }

    @Test
    void testConfigureFallbackToCbc() throws Exception {
        char[] password = "password".toCharArray();
        byte[] encryptedKey = new byte[]{1, 2, 3};
        byte[] decryptedKey = new byte[CryptoManager.KEY_SIZE];
        SECURE_RANDOM.nextBytes(decryptedKey);

        File keyFile = mock(File.class);
        when(filesUtils.getInternalFile(null, "key.encrypted")).thenReturn(keyFile);
        when(filesUtils.readFileBytes(keyFile)).thenReturn(encryptedKey);

        AesGcmCryptor mockGcm = mock(AesGcmCryptor.class);
        AesCbcCryptor mockCbc = mock(AesCbcCryptor.class);
        when(cryptorFactory.create(eq(password), eq(AesGcmCryptor.class))).thenReturn(mockGcm);
        when(cryptorFactory.create(eq(password), eq(AesCbcCryptor.class))).thenReturn(mockCbc);

        when(mockGcm.decrypt(encryptedKey))
                .thenThrow(new GeneralSecurityException("GCM failed"));
        when(mockCbc.decrypt(encryptedKey)).thenReturn(decryptedKey);

        boolean result = cryptoManager.configure(null, password);

        assertTrue(result,
                "configure should return true when fallback to CBC succeeds");
        assertTrue(cryptoManager.isConfigured(),
                "CryptoManager should be configured after successful fallback");
        assertArrayEquals(decryptedKey, cryptoManager.getSecrets().getKey(),
                "The stored key should match the CBC-decrypted key");
    }

    @Test
    void testConfigureFailure() throws Exception {
        char[] password = "wrong".toCharArray();
        byte[] encryptedKey = new byte[]{1, 2, 3};

        File keyFile = mock(File.class);
        when(filesUtils.getInternalFile(null, "key.encrypted")).thenReturn(keyFile);
        when(filesUtils.readFileBytes(keyFile)).thenReturn(encryptedKey);

        AesGcmCryptor mockGcm = mock(AesGcmCryptor.class);
        AesCbcCryptor mockCbc = mock(AesCbcCryptor.class);
        when(cryptorFactory.create(eq(password), eq(AesGcmCryptor.class))).thenReturn(mockGcm);
        when(cryptorFactory.create(eq(password), eq(AesCbcCryptor.class))).thenReturn(mockCbc);

        when(mockGcm.decrypt(any()))
                .thenThrow(new GeneralSecurityException("fail"));
        when(mockCbc.decrypt(any()))
                .thenThrow(new GeneralSecurityException("fail"));

        boolean result = cryptoManager.configure(null, password);

        assertFalse(result,
                "configure should return false when both GCM and CBC decryption fail");
        assertFalse(cryptoManager.isConfigured(),
                "CryptoManager should not be configured" +
                " after a failed configuration attempt");
    }

    @Test
    void testIsConfigured() {
        assertFalse(cryptoManager.isConfigured(),
                "CryptoManager should not be configured by default");
    }

    @Test
    void testGetSecretsThrowsWhenNotConfigured() {
        assertThrows(SessionExpiredException.class, () -> cryptoManager.getSecrets(),
                "getSecrets should throw SessionExpiredException when not configured");
    }

    @Test
    void testIsBlockedChecksFileAsWell() {
        when(prefs.getBoolean("is_blocked", false)).thenReturn(false);
        File markerFile = mock(File.class);
        when(filesUtils.getInternalFile(null, ".blocked")).thenReturn(markerFile);
        when(markerFile.exists()).thenReturn(true);

        assertTrue(cryptoManager.isBlocked(null),
                "isBlocked should return true if the blocked marker file exists");
    }

    @Test
    void testVerifyKeyWithFileHash() throws Exception {
        byte[] key = new byte[]{1, 2, 3};
        byte[] hash = toSha256Bytes(key);
        when(prefs.getString("key_hash", null)).thenReturn(null);
        File hashFile = mock(File.class);
        when(filesUtils.getInternalFile(null, "key.sha256")).thenReturn(hashFile);
        when(hashFile.exists()).thenReturn(true);
        when(filesUtils.readFileBytes(hashFile)).thenReturn(hash);

        boolean result = cryptoManager.verifyKey(null, key);

        assertTrue(result,
                "verifyKey should return true when" +
                        " the key hash matches the one stored in a file");
    }

    @Test
    void testDestroySecrets() throws Exception {
        char[] password = "password".toCharArray();
        byte[] encryptedKey = new byte[]{1, 2, 3};
        byte[] decryptedKey = new byte[CryptoManager.KEY_SIZE];

        File keyFile = mock(File.class);
        when(filesUtils.getInternalFile(null, "key.encrypted")).thenReturn(keyFile);
        when(filesUtils.readFileBytes(keyFile)).thenReturn(encryptedKey);

        AesGcmCryptor mockCryptor = mock(AesGcmCryptor.class);
        when(cryptorFactory.create(eq(password), eq(AesGcmCryptor.class))).thenReturn(mockCryptor);
        when(mockCryptor.decrypt(encryptedKey)).thenReturn(decryptedKey);

        cryptoManager.configure(null, password);
        assertTrue(cryptoManager.isConfigured(),
                "Manager should be configured after successful configuration");

        cryptoManager.destroySecrets();

        assertFalse(cryptoManager.isConfigured(),
                "Manager should not be configured after destroying secrets");
        assertThrows(SessionExpiredException.class, () -> cryptoManager.getSecrets(),
                "getSecrets should throw SessionExpiredException" +
                        " after secrets are destroyed");
    }
}
