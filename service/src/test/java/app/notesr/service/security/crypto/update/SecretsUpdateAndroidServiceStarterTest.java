/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security.crypto.update;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import app.notesr.core.security.SecretCache;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.ValueEncryptor;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.core.util.KeyUtils;

@ExtendWith(MockitoExtension.class)
class SecretsUpdateAndroidServiceStarterTest {
    
    private static final int KEY_SIZE = 48;

    @Mock
    private Context context;

    private SecretsUpdateAndroidServiceStarter starter;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        SecretCache.clear();
    }

    @AfterEach
    void tearDown() {
        SecretCache.clear();
    }

    @Test
    void testStartNullPayloadNoCacheThrowsIllegalStateException() {
        starter = new SecretsUpdateAndroidServiceStarter();
        assertThrows(IllegalStateException.class, () -> starter.start(context));
    }

    @Test
    void testStartNullPayloadCachePresentStartsService() throws Exception {
        byte[] newKey = "newKey".getBytes();
        byte[] password = "password".getBytes();

        SecretCache.put(SecretsUpdateAndroidService.NEW_KEY, newKey);
        SecretCache.put(SecretsUpdateAndroidService.PASSWORD, password);

        MockedConstruction<Intent> mockedIntent = mockConstruction(Intent.class,
                (mock, context) ->
                        when(mock.putExtra(anyString(), (Serializable) any())).thenReturn(mock));

        try (mockedIntent) {
            starter = new SecretsUpdateAndroidServiceStarter();
            starter.start(context);

            verify(context).startForegroundService(any(Intent.class));
            Intent constructed = mockedIntent.constructed().get(0);
            verify(constructed)
                    .putExtra(eq(SecretsUpdateAndroidService.EXTRA_CURRENT_STATE),
                            (Serializable) eq(null));
        }
    }

    @Test
    void testStartWithPayloadUpdatesCacheAndStartsService() throws Exception {
        byte[] newKey = "newKey".getBytes();
        byte[] expectedNewKey = newKey.clone();

        String passwordStr = "password";
        char[] newPassword = passwordStr.toCharArray();
        byte[] expectedPasswordBytes = passwordStr.getBytes(StandardCharsets.UTF_8);

        var payload = new SecretsUpdateAndroidServiceStarter.Payload(newKey, newPassword);

        MockedConstruction<Intent> mockedIntent = mockConstruction(Intent.class,
                (mock, context) ->
                        when(mock.putExtra(anyString(), (Serializable) any())).thenReturn(mock));

        try (mockedIntent) {
            starter = new SecretsUpdateAndroidServiceStarter(payload, null);
            starter.start(context);

            assertArrayEquals(expectedNewKey,
                    SecretCache.take(SecretsUpdateAndroidService.NEW_KEY));
            assertArrayEquals(expectedPasswordBytes,
                    SecretCache.take(SecretsUpdateAndroidService.PASSWORD));

            verify(context).startForegroundService(any(Intent.class));
        }
    }

    @Test
    void testStartWithEncryptedDataDecryptsAndStartsService() throws Exception {
        byte[] key = new byte[KEY_SIZE];
        char[] password = "oldPassword".toCharArray();

        CryptoSecrets secrets = new CryptoSecrets(key, password);

        byte[] newKey = "newKey".getBytes();
        byte[] expectedNewKey = newKey.clone();

        String newPasswordStr = "newPassword";
        char[] newPassword = newPasswordStr.toCharArray();
        byte[] expectedNewPasswordBytes = newPasswordStr.getBytes(StandardCharsets.UTF_8);

        var payload = new SecretsUpdateAndroidServiceStarter.Payload(newKey, newPassword);

        String payloadJson = mapper.writeValueAsString(payload);

        var encryptor = new ValueEncryptor(new AesGcmCryptor(
                KeyUtils.getSecretKeyFromSecrets(secrets)));
        String encryptedPayload = encryptor.encrypt(payloadJson);

        SecretsUpdateState state = new SecretsUpdateState(SecretsUpdateStatus.MOVING_DB_DATA,
                "tx123");
        String stateJson = mapper.writeValueAsString(state);

        MockedConstruction<Intent> mockedIntent = mockConstruction(Intent.class,
                (mock, context) ->
                        when(mock.putExtra(anyString(), (Serializable) any())).thenReturn(mock));

        try (mockedIntent) {
            starter = new SecretsUpdateAndroidServiceStarter();
            starter.start(context, secrets, encryptedPayload, stateJson);

            assertArrayEquals(expectedNewKey,
                    SecretCache.take(SecretsUpdateAndroidService.NEW_KEY));
            assertArrayEquals(expectedNewPasswordBytes,
                    SecretCache.take(SecretsUpdateAndroidService.PASSWORD));

            verify(context).startForegroundService(any(Intent.class));
            Intent constructed = mockedIntent.constructed().get(0);
            verify(constructed).putExtra(SecretsUpdateAndroidService.EXTRA_CURRENT_STATE, state);
        }
    }

    @Test
    void testStartWithEncryptedDataInvalidSecretsThrowsDecryptionFailedException() {
        byte[] key = new byte[KEY_SIZE];
        CryptoSecrets secrets = new CryptoSecrets(key, "wrong".toCharArray());

        String encryptedPayload = "invalid_payload";

        starter = new SecretsUpdateAndroidServiceStarter();
        assertThrows(DecryptionFailedException.class, () -> 
                starter.start(context, secrets, encryptedPayload, null));
    }

    @Test
    void testStartWithEncryptedDataInvalidJsonThrowsJsonProcessingException() throws Exception {
        byte[] key = new byte[KEY_SIZE];
        char[] password = "oldPassword".toCharArray();
        CryptoSecrets secrets = new CryptoSecrets(key, password);

        var encryptor = new ValueEncryptor(new AesGcmCryptor(
                KeyUtils.getSecretKeyFromSecrets(secrets)));
        String encryptedPayload = encryptor.encrypt("not a json");

        starter = new SecretsUpdateAndroidServiceStarter();
        assertThrows(JsonProcessingException.class, () -> 
                starter.start(context, secrets, encryptedPayload, null));
    }

    @Test
    void testStartWithEncryptedDataNullSecretsThrowsNullPointerException() {
        starter = new SecretsUpdateAndroidServiceStarter();
        assertThrows(NullPointerException.class, () -> 
                starter.start(context, null, "payload", null));
    }

    @Test
    void testStartWithEncryptedDataNullPayloadThrowsNullPointerException() {
        byte[] key = new byte[KEY_SIZE];
        CryptoSecrets secrets = new CryptoSecrets(key, "pass".toCharArray());
        starter = new SecretsUpdateAndroidServiceStarter();
        assertThrows(NullPointerException.class, () -> 
                starter.start(context, secrets, null, null));
    }
}
