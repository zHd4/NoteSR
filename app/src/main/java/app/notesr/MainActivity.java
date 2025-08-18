package app.notesr;

import android.content.Intent;
import android.os.Bundle;

import java.util.List;
import java.util.function.Supplier;

import app.notesr.exporter.activity.ExportActivity;
import app.notesr.importer.activity.ImportActivity;
import app.notesr.migration.activity.MigrationActivity;
import app.notesr.security.activity.ReEncryptionActivity;
import app.notesr.note.activity.NotesListActivity;
import app.notesr.security.activity.AuthActivity;
import app.notesr.security.activity.KeyRecoveryActivity;
import app.notesr.security.crypto.CryptoManager;
import app.notesr.security.crypto.CryptoManagerProvider;
import app.notesr.migration.service.AppMigrationAndroidService;
import app.notesr.exporter.service.ExportAndroidService;
import app.notesr.importer.service.ImportAndroidService;
import app.notesr.security.service.ReEncryptionAndroidService;

public class MainActivity extends ActivityBase {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CryptoManager cryptoManager = CryptoManagerProvider.getInstance();

        List<Supplier<Intent>> intentSuppliers = getIntentSuppliers(App.getContext(),
                cryptoManager);
        Intent defaultIntent = new Intent(getApplicationContext(), NotesListActivity.class);

        startActivity(new StartupIntentResolver(intentSuppliers, defaultIntent).resolve());
        finish();
    }

    private List<Supplier<Intent>> getIntentSuppliers(App context, CryptoManager cryptoManager) {
        return List.of(
                () -> cryptoManager.isBlocked(getApplicationContext())
                        ? new Intent(context, KeyRecoveryActivity.class)
                        : null,
                () -> !cryptoManager.isKeyExists(getApplicationContext())
                        ? new Intent(context, StartActivity.class)
                        : null,
                () -> context.isServiceRunning(AppMigrationAndroidService.class)
                        ? new Intent(context, MigrationActivity.class)
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
                    if (!cryptoManager.isConfigured()) {
                        Intent intent = new Intent(context, AuthActivity.class);
                        intent.putExtra("mode", AuthActivity.Mode.AUTHORIZATION.toString());

                        return intent;
                    }

                    return null;
                }
        );
    }
}
