package app.notesr.activity.security;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import app.notesr.service.security.SecretsUpdateAndroidService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ReEncryptionBroadcastReceiver extends BroadcastReceiver {
    private final Runnable onReEncryptionComplete;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SecretsUpdateAndroidService.BROADCAST_ACTION.equals(intent.getAction())) {
            boolean isCompleted = intent.getBooleanExtra(SecretsUpdateAndroidService.EXTRA_COMPLETE,
                    false);

            if (isCompleted) {
                onReEncryptionComplete.run();
            }
        }
    }
}
