package app.notesr;

import android.content.Intent;
import android.os.Bundle;

import java.util.List;
import java.util.function.Supplier;

import app.notesr.data.ExportActivity;
import app.notesr.data.ImportActivity;
import app.notesr.data.MigrationActivity;
import app.notesr.data.ReEncryptionActivity;
import app.notesr.note.NoteListActivity;
import app.notesr.crypto.AuthActivity;
import app.notesr.crypto.KeyRecoveryActivity;
import app.notesr.crypto.CryptoManager;
import app.notesr.service.android.AppMigrationAndroidService;
import app.notesr.service.android.ExportAndroidService;
import app.notesr.service.android.ImportAndroidService;
import app.notesr.service.android.ReEncryptionAndroidService;

public class MainActivity extends ActivityBase {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        App context = App.getContext();
        CryptoManager cryptoManager = CryptoManager.getInstance(context);

        List<Supplier<Intent>> intentSuppliers = getIntentSuppliers(context, cryptoManager);
        Intent defaultIntent = new Intent(context, NoteListActivity.class);

        startActivity(new StartupIntentResolver(intentSuppliers, defaultIntent).resolve());
        finish();
    }

    private List<Supplier<Intent>> getIntentSuppliers(App context, CryptoManager cryptoManager) {
        return List.of(
                () -> cryptoManager.isKeyExists()
                        ? new Intent(context, StartActivity.class)
                        : null,

                () -> cryptoManager.isBlocked()
                        ? new Intent(context, KeyRecoveryActivity.class)
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
