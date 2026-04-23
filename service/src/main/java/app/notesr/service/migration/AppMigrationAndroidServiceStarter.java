/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.migration;

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.content.Intent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.service.AndroidServiceStarter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public final class AppMigrationAndroidServiceStarter implements AndroidServiceStarter {

    private Payload payload;

    @Override
    public void start(Context context) {
        requireNonNull(payload, "Payload is null");
        context.startForegroundService(buildIntent(context));
    }

    @Override
    public void start(Context context, CryptoSecrets secrets, String payload)
            throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        this.payload = mapper.readValue(payload, Payload.class);
        start(context);
    }

    private Intent buildIntent(Context context) {
        return new Intent(context, AppMigrationAndroidService.class)
                .putExtra(AppMigrationAndroidService.EXTRA_CURRENT_DATA_SCHEMA_VERSION,
                        payload.getCurrentDataSchemaVersion());
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Payload {
        private int currentDataSchemaVersion;
    }
}
