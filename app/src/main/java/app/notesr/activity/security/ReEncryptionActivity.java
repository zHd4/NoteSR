package app.notesr.activity.security;

import static app.notesr.core.util.ActivityUtils.disableBackButton;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.notesr.activity.App;
import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.note.NotesListActivity;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.service.security.ReEncryptionAndroidService;

public final class ReEncryptionActivity extends ActivityBase {

    public static final String EXTRA_NEW_SECRETS = "new_secrets";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_re_encryption);

        disableBackButton(this);

        ReEncryptionBroadcastReceiver broadcastReceiver =
                new ReEncryptionBroadcastReceiver(this::onReEncryptionComplete);

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(ReEncryptionAndroidService.BROADCAST_ACTION));

        startReEncryptionService();
    }

    private void startReEncryptionService() {
        CryptoSecrets secrets = (CryptoSecrets) getIntent()
                .getSerializableExtra(EXTRA_NEW_SECRETS);

        if (!App.getContext().isServiceRunning(ReEncryptionAndroidService.class)) {
            Intent serviceIntent = new Intent(getApplicationContext(),
                    ReEncryptionAndroidService.class);

            serviceIntent.putExtra(ReEncryptionAndroidService.EXTRA_NEW_SECRETS, secrets);
            startForegroundService(serviceIntent);
        }
    }

    private void onReEncryptionComplete() {
        startActivity(new Intent(getApplicationContext(), NotesListActivity.class));
        finish();
    }
}
