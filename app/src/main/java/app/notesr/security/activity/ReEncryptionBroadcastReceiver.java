package app.notesr.security.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import app.notesr.security.service.ReEncryptionAndroidService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReEncryptionBroadcastReceiver extends BroadcastReceiver {
    private final Runnable onReEncryptionComplete;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ReEncryptionAndroidService.BROADCAST_ACTION.equals(intent.getAction())) {
            boolean isCompleted = intent.getBooleanExtra(ReEncryptionAndroidService.EXTRA_COMPLETE,
                    false);

            if (isCompleted) {
                onReEncryptionComplete.run();
            }
        }
    }
}
