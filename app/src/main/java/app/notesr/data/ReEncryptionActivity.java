package app.notesr.data;

import static app.notesr.util.ActivityUtils.disableBackButton;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.notesr.App;
import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.note.NoteListActivity;
import app.notesr.dto.CryptoSecrets;
import app.notesr.service.android.ReEncryptionAndroidService;

public class ReEncryptionActivity extends ActivityBase {

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

    protected void startReEncryptionService() {
        CryptoSecrets newCryptoKey = (CryptoSecrets) getIntent()
                .getSerializableExtra(EXTRA_NEW_SECRETS);

        if (!App.getContext().isServiceRunning(ReEncryptionAndroidService.class)) {
            Intent serviceIntent = new Intent(getApplicationContext(),
                    ReEncryptionAndroidService.class);

            serviceIntent.putExtra(ReEncryptionAndroidService.EXTRA_NEW_SECRETS, newCryptoKey);
            startForegroundService(serviceIntent);
        }
    }

    private void onReEncryptionComplete() {
        startActivity(new Intent(getApplicationContext(), NoteListActivity.class));
        finish();
    }
}