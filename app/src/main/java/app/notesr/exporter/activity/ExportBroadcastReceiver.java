package app.notesr.exporter.activity;

import static java.util.Objects.requireNonNull;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import app.notesr.exporter.service.ExportAndroidService;
import app.notesr.exporter.service.ExportStatus;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExportBroadcastReceiver extends BroadcastReceiver {
    private static final int DEFAULT_PROGRESS = -1;

    private final Consumer<String> onOutputPathReceived;
    private final BiConsumer<ExportStatus, Integer> onExportRunning;
    private final Consumer<ExportStatus> onExportComplete;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ExportAndroidService.BROADCAST_ACTION.equals(intent.getAction())) {
            if (intent.hasExtra(ExportAndroidService.OUTPUT_PATH_EXTRA)) {
                String outputPath = intent.getStringExtra(ExportAndroidService.OUTPUT_PATH_EXTRA);
                onOutputPathReceived.accept(outputPath);
            }

            ExportStatus status =
                    (ExportStatus) intent.getSerializableExtra(ExportAndroidService.STATUS_EXTRA);

            if (!ExportAndroidService.FINISH_STATUSES.contains(status)) {
                int progress = intent.getIntExtra(ExportAndroidService.PROGRESS_EXTRA,
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
}
