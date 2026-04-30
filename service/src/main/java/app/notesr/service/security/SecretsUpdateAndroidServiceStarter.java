/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security;

import static java.util.Objects.requireNonNull;
import static app.notesr.core.util.CharUtils.charsToBytes;
import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;
import static app.notesr.service.security.SecretsUpdateAndroidService.EXTRA_CURRENT_STATE;

import android.content.Context;
import android.content.Intent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

import app.notesr.core.security.SecretCache;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.ValueDecryptor;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.service.AndroidServiceStarter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public final class SecretsUpdateAndroidServiceStarter implements AndroidServiceStarter {

    private Payload payload;
    private SecretsUpdateState state;

    @Override
    public void start(Context context) throws CharacterCodingException {
        if (payload == null) {
            if (!SecretCache.contains(SecretsUpdateAndroidService.NEW_KEY)
                    || !SecretCache.contains(SecretsUpdateAndroidService.PASSWORD)) {
                throw new IllegalStateException("Secrets are not set");
            }
        } else {
            SecretCache.removeIfExists(SecretsUpdateAndroidService.NEW_KEY);
            SecretCache.removeIfExists(SecretsUpdateAndroidService.PASSWORD);

            SecretCache.put(SecretsUpdateAndroidService.NEW_KEY, payload.getNewKey());
            SecretCache.put(SecretsUpdateAndroidService.PASSWORD,
                    charsToBytes(payload.getNewPassword(), StandardCharsets.UTF_8));
        }

        context.startForegroundService(buildIntent(context));
    }

    @Override
    public void start(Context context, CryptoSecrets secrets, String payload, String state)
            throws DecryptionFailedException, JsonProcessingException, CharacterCodingException {

        var mapper = new ObjectMapper();

        if (state != null) {
            this.state = mapper.readValue(state, SecretsUpdateState.class);
        }

        this.payload = decryptPayload(mapper, secrets, payload);
        start(context);
    }

    private Payload decryptPayload(ObjectMapper mapper, CryptoSecrets secrets, String payload)
            throws DecryptionFailedException, JsonProcessingException {

        requireNonNull(secrets, "Secrets are null");
        requireNonNull(payload, "Payload is null");

        var payloadJson = new ValueDecryptor(new AesGcmCryptor(getSecretKeyFromSecrets(secrets)))
                .decrypt(payload);

        return mapper.readValue(payloadJson, Payload.class);
    }

    private Intent buildIntent(Context context) {
        return new Intent(context, SecretsUpdateAndroidService.class)
                .putExtra(EXTRA_CURRENT_STATE, state);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private byte[] newKey;
        private char[] newPassword;
    }
}
