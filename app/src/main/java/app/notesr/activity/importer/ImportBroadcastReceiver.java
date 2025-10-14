package app.notesr.activity.importer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.function.Consumer;

import app.notesr.service.importer.ImportAndroidService;
import app.notesr.service.importer.ImportStatus;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ImportBroadcastReceiver extends BroadcastReceiver {
    private final Consumer<ImportStatus> onImportRunning;
    private final Consumer<ImportStatus> onImportComplete;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ImportAndroidService.BROADCAST_ACTION.equals(intent.getAction())) {
            ImportStatus status =
                    (ImportStatus) intent.getSerializableExtra(ImportAndroidService.EXTRA_STATUS);

            if (!ImportAndroidService.FINISH_STATUSES.contains(status)) {
                onImportRunning.accept(status);
            } else {
                onImportComplete.accept(status);
            }
        }
    }
}
