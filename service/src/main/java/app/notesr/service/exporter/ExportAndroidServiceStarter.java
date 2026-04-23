/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.exporter;

import android.content.Context;
import android.content.Intent;

import com.fasterxml.jackson.databind.ObjectMapper;

import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.service.AndroidServiceStarter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public final class ExportAndroidServiceStarter implements AndroidServiceStarter {

    private Payload payload;

    @Override
    public void start(Context context) {
        context.startForegroundService(buildIntent(context));
    }

    @Override
    public void start(Context context, CryptoSecrets secrets, String payload) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        this.payload = mapper.readValue(payload, Payload.class);
        start(context);
    }

    private Intent buildIntent(Context context) {
        return new Intent(context, ExportAndroidService.class)
                .putExtra(ExportAndroidService.EXTRA_APP_VERSION, payload.getAppVersion());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private String appVersion;
    }
}
