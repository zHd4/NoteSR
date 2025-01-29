package app.notesr.activity;

import android.content.Intent;
import android.os.Bundle;

import app.notesr.App;
import app.notesr.R;
import app.notesr.activity.data.ExportActivity;
import app.notesr.activity.data.ImportActivity;
import app.notesr.activity.data.ReEncryptionActivity;
import app.notesr.activity.notes.NotesListActivity;
import app.notesr.activity.security.AuthActivity;
import app.notesr.activity.security.KeyRecoveryActivity;
import app.notesr.crypto.CryptoManager;
import app.notesr.service.android.ExportService;
import app.notesr.service.android.ImportService;
import app.notesr.service.android.ReEncryptionService;

public class MainActivity extends ExtendedAppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CryptoManager cryptoManager = App.getAppContainer().getCryptoManager();

        Intent intent;

        if (cryptoManager.isFirstRun()) {
            intent = new Intent(this, StartActivity.class);

        } else if (cryptoManager.isBlocked()) {
            intent = new Intent(this, KeyRecoveryActivity.class);

        } else if (App.getContext().isServiceRunning(ExportService.class)) {
            intent = new Intent(this, ExportActivity.class);

        } else if (App.getContext().isServiceRunning(ImportService.class)) {
            intent = new Intent(this, ImportActivity.class);

        } else if (App.getContext().isServiceRunning(ReEncryptionService.class)) {
            intent = new Intent(this, ReEncryptionActivity.class);

        } else if (!cryptoManager.ready()) {
            intent = new Intent(this, AuthActivity.class);
            intent.putExtra("mode", AuthActivity.Mode.AUTHORIZATION.toString());

        } else {
            intent = new Intent(this, NotesListActivity.class);
        }

        startActivity(intent);
        finish();
    }
}