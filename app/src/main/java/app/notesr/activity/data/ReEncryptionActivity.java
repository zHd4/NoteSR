package app.notesr.activity.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.notesr.R;
import app.notesr.activity.ExtendedAppCompatActivity;
import app.notesr.activity.notes.NoteListActivity;
import app.notesr.dto.CryptoKey;
import app.notesr.service.android.ReEncryptionService;

public class ReEncryptionActivity extends ExtendedAppCompatActivity {

    private TextView progressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_re_encryption);

        disableBackButton();

        progressView = findViewById(R.id.reEncryptionProgressLabel);
        CryptoKey newCryptoKey = (CryptoKey) getIntent().getSerializableExtra("newCryptoKey");

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver(),
                new IntentFilter(ReEncryptionService.BROADCAST_ACTION));

        if (newCryptoKey != null) {
            Intent serviceIntent = new Intent(this, ReEncryptionService.class)
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