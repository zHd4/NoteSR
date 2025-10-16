package app.notesr.service.security;

import android.content.Context;

import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.security.exception.EncryptionFailedException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class SecretsSetupService {
    private final Context context;
    private final CryptoManager cryptoManager;
    private final CryptoSecrets cryptoSecrets;

    public SecretsSetupService(Context context, CryptoManager cryptoManager, char[] password) {
        this.context = context;
        this.cryptoManager = cryptoManager;
        this.cryptoSecrets = cryptoManager.generateSecrets(password);
    }

    public void apply() throws EncryptionFailedException {
        cryptoManager.setSecrets(context, cryptoSecrets);
    }
}
