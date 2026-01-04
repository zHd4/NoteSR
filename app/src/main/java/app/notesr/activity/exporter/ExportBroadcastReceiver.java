/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */
 
package app.notesr.activity.exporter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import app.notesr.service.exporter.ExportAndroidService;
import app.notesr.service.exporter.ExportStatus;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ExportBroadcastReceiver extends BroadcastReceiver {
    private static final int DEFAULT_PROGRESS = -1;

    private final Consumer<String> onOutputPathReceived;
    private final BiConsumer<ExportStatus, Integer> onExportRunning;
    private final Consumer<ExportStatus> onExportComplete;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ExportAndroidService.BROADCAST_ACTION.equals(intent.getAction())) {
            if (intent.hasExtra(ExportAndroidService.EXTRA_OUTPUT_PATH)) {
                onOutputPathReceived(intent);
            }

            if (intent.hasExtra(ExportAndroidService.EXTRA_STATUS)) {
                onStatusReceived(intent);
            }
        }
    }

    private void onOutputPathReceived(Intent intent) {
        String outputPath = intent.getStringExtra(ExportAndroidService.EXTRA_OUTPUT_PATH);
        onOutputPathReceived.accept(outputPath);
    }

    private void onStatusReceived(Intent intent) {
        ExportStatus status =
                (ExportStatus) intent.getSerializableExtra(ExportAndroidService.EXTRA_STATUS);

        if (!ExportAndroidService.FINISH_STATUSES.contains(status)) {
            int progress = intent.getIntExtra(ExportAndroidService.EXTRA_PROGRESS,
                    DEFAULT_PROGRESS);

            if (progress == DEFAULT_PROGRESS) {
                throw new RuntimeException("Unexpected progress value: no value provided");
            }

            onExportRunning.accept(status, progress);
        } else {
            onExportComplete.accept(status);
        }
    }
}
