package app.notesr.activity.data;

import static java.util.Objects.requireNonNull;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.notesr.R;
import app.notesr.activity.ExtendedAppCompatActivity;
import app.notesr.activity.notes.NotesListActivity;
import app.notesr.dto.CryptoKey;
import app.notesr.service.android.ReEncryptionService;

public class ReEncryptionActivity extends ExtendedAppCompatActivity {

    private TextView progressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_re_encryption);

        disableBackButton();

        CryptoKey newCryptoKey =
                requireNonNull((CryptoKey) getIntent().getSerializableExtra("newCryptoKey"));

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver(),
                new IntentFilter(ReEncryptionService.BROADCAST_ACTION));

        Intent serviceIntent = new Intent(getApplicationContext(), ReEncryptionActivity.class)
                .putExtra("newCryptoKey", newCryptoKey);

        startForegroundService(serviceIntent);
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
                        startActivity(new Intent(context, NotesListActivity.class));
                    }
                }
            }
        };
    }
}