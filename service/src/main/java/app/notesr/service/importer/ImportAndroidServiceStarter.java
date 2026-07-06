/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.importer;

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.notesr.core.security.crypto.AesCryptorFactory;
import app.notesr.core.security.crypto.ValueDecryptor;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.service.AndroidServiceStarter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public final class ImportAndroidServiceStarter implements AndroidServiceStarter {

    private Payload payload;

    @Override
    public void start(Context context) {
        requireNonNull(payload, "Payload is null");
        context.startForegroundService(buildIntent(context));
    }

    @Override
    public void start(Context context, CryptoSecrets secrets, String payload, String state)
            throws DecryptionFailedException, JsonProcessingException {

        requireNonNull(secrets, "Secrets are null");
        var payloadJson = new ValueDecryptor(AesCryptorFactory.createAesGcmCryptor(secrets))
                .decrypt(payload);

        var mapper = new ObjectMapper();

        this.payload = mapper.readValue(payloadJson, Payload.class);
        start(context);
    }

    private Intent buildIntent(Context context) {
        var sourceUri = payload.getSourceUri() != null ? Uri.parse(payload.getSourceUri()) : null;
        return new Intent(context, ImportAndroidService.class).setData(sourceUri);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private String sourceUri;
    }
}
