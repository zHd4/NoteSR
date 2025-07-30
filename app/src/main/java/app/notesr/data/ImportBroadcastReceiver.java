package app.notesr.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.function.Consumer;

import app.notesr.service.android.ImportAndroidService;
import app.notesr.service.data.importer.ImportStatus;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ImportBroadcastReceiver extends BroadcastReceiver {
    private final Consumer<ImportStatus> onImportRunning;
    private final Consumer<ImportStatus> onImportComplete;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ImportAndroidService.BROADCAST_ACTION.equals(intent.getAction())) {
            ImportStatus status =
                    (ImportStatus) intent.getSerializableExtra(ImportAndroidService.STATUS_EXTRA);

            if (!ImportAndroidService.FINISH_STATUSES.contains(status)) {
                onImportRunning.accept(status);
            } else {
                onImportComplete.accept(status);
            }
        }
    }
}
