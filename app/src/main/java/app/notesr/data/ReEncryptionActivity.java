package app.notesr.data;

import static app.notesr.util.ActivityUtils.disableBackButton;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.note.NoteListActivity;
import app.notesr.dto.CryptoKey;
import app.notesr.service.android.ReEncryptionAndroidService;

public class ReEncryptionActivity extends ActivityBase {

    private TextView progressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_re_encryption);

        disableBackButton(this);

        progressView = findViewById(R.id.reEncryptionProgressLabel);
        CryptoKey newCryptoKey = (CryptoKey) getIntent().getSerializableExtra("newCryptoKey");

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver(),
                new IntentFilter(ReEncryptionAndroidService.BROADCAST_ACTION));

        if (newCryptoKey != null) {
            Intent serviceIntent = new Intent(this, ReEncryptionAndroidService.class)
                    .putExtra("newCryptoKey", newCryptoKey);

            startForegroundService(serviceIntent);
        }
    }

    private BroadcastReceiver broadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int progress = intent.getIntExtra("progress", -1);

                if (progress != -1) {
                    String progressStr = progress + "%";
                    progressView.setText(progressStr);

                    if (progress == 100) {
                        startActivity(new Intent(context, NoteListActivity.class));
                        finish();
                    }
                }
            }
        };
    }
}