package app.notesr.migration.changes.security.crypto;

import android.content.Context;

import app.notesr.core.security.exception.EncryptionFailedException;
import app.notesr.migration.service.AppMigration;
import app.notesr.migration.service.AppMigrationException;
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
            cryptoManager.setSecrets(context, cryptoManager.getSecrets());
        } catch (EncryptionFailedException e) {
            throw new AppMigrationException("Failed to migrate key", e);
        }
    }

    CryptoManager getCryptoManager(Context context) {
        return CryptoManagerProvider.getInstance(context);
    }
}
