package notesr.activity;

import android.content.Intent;
import android.os.Bundle;
import notesr.App;
import notesr.R;
import notesr.activity.data.ExportActivity;
import notesr.activity.data.ImportActivity;
import notesr.activity.notes.NotesListActivity;
import notesr.activity.security.AuthActivity;
import notesr.activity.security.KeyRecoveryActivity;
import notesr.crypto.CryptoManager;
import notesr.service.ExportService;
import notesr.service.ImportService;

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
        } else if (App.getContext().serviceRunning(ExportService.class)) {
            intent = new Intent(this, ExportActivity.class);
        } else if (App.getContext().serviceRunning(ImportService.class)) {
            intent = new Intent(this, ImportActivity.class);
        } else if (!cryptoManager.ready()) {
            intent = new Intent(this, AuthActivity.class);
            intent.putExtra("mode", AuthActivity.AUTHORIZATION_MODE);
        } else {
            intent = new Intent(this, NotesListActivity.class);
        }

        startActivity(intent);
        finish();
    }
}