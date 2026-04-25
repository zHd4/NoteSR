/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security;

import static java.util.Objects.requireNonNull;
import static app.notesr.core.util.CharUtils.charsToBytes;
import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

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
    public void start(Context context, CryptoSecrets secrets, String payload)
            throws DecryptionFailedException, JsonProcessingException, CharacterCodingException {

        requireNonNull(secrets, "Secrets are null");
        var payloadJson = new ValueDecryptor(new AesGcmCryptor(getSecretKeyFromSecrets(secrets)))
                .decrypt(payload);

        var mapper = new ObjectMapper();
        this.payload = mapper.readValue(payloadJson,
                SecretsUpdateAndroidServiceStarter.Payload.class);

        start(context);
    }

    private Intent buildIntent(Context context) {
        return new Intent(context, SecretsUpdateAndroidService.class);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private byte[] newKey;
        private char[] newPassword;
    }
}
