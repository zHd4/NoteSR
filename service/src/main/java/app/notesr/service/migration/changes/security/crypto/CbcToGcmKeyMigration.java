package app.notesr.service.migration.changes.security.crypto;

import android.content.Context;

import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.security.exception.EncryptionFailedException;
import app.notesr.service.migration.AppMigration;
import app.notesr.service.migration.AppMigrationException;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CbcToGcmKeyMigration implements AppMigration {

    @Getter
    private final int fromVersion;

    @Getter
    private final int toVersion;

    @Override
    public void migrate(Context context) {
        try {
            CryptoManager cryptoManager = getCryptoManager(context);
            CryptoSecrets secrets = cryptoManager.getSecrets();

            cryptoManager.setSecrets(context, secrets);
            secrets.destroy();
        } catch (EncryptionFailedException e) {
            throw new AppMigrationException("Failed to migrate key", e);
        }
    }

    CryptoManager getCryptoManager(Context context) {
        return CryptoManagerProvider.getInstance(context);
    }
}
