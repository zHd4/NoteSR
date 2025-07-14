package app.notesr.activity;

import android.content.Intent;
import android.os.Bundle;

import java.util.List;
import java.util.function.Supplier;

import app.notesr.App;
import app.notesr.BuildConfig;
import app.notesr.activity.data.MigrationActivity;
import app.notesr.R;
import app.notesr.activity.data.ExportActivity;
import app.notesr.activity.data.ImportActivity;
import app.notesr.activity.data.ReEncryptionActivity;
import app.notesr.activity.notes.NoteListActivity;
import app.notesr.activity.security.AuthActivity;
import app.notesr.activity.security.KeyRecoveryActivity;
import app.notesr.crypto.CryptoManager;
import app.notesr.service.android.ExportAndroidService;
import app.notesr.service.android.ImportAndroidService;
import app.notesr.service.android.ReEncryptionAndroidService;
import app.notesr.service.migration.DataVersionManager;

public class MainActivity extends ExtendedAppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        App context = App.getContext();
        CryptoManager cryptoManager = App.getAppContainer().getCryptoManager();

        List<Supplier<Intent>> intentSuppliers = getIntentSuppliers(context, cryptoManager);
        Intent defaultIntent = new Intent(context, NoteListActivity.class);

        startActivity(new StartupIntentResolver(intentSuppliers, defaultIntent).resolve());
        finish();
    }

    private List<Supplier<Intent>> getIntentSuppliers(App context, CryptoManager cryptoManager) {
        return List.of(
                () -> cryptoManager.isFirstRun()
                        ? new Intent(context, StartActivity.class)
                        : null,

                () -> cryptoManager.isBlocked()
                        ? new Intent(context, KeyRecoveryActivity.class)
                        : null,

                () -> context.isServiceRunning(ExportAndroidService.class)
                        ? new Intent(context, ExportActivity.class)
                        : null,

                () -> context.isServiceRunning(ImportAndroidService.class)
                        ? new Intent(context, ImportActivity.class)
                        : null,

                () -> context.isServiceRunning(ReEncryptionAndroidService.class)
                        ? new Intent(context, ReEncryptionActivity.class)
                        : null,

                () -> {
                    if (!cryptoManager.ready()) {
                        Intent intent = new Intent(context, AuthActivity.class);
                        intent.putExtra("mode", AuthActivity.Mode.AUTHORIZATION.toString());

                        return intent;
                    }

                    return null;
                }
        );
    }
}
